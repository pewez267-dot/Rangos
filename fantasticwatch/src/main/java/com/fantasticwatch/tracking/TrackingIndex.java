package com.fantasticwatch.tracking;

import com.fantasticwatch.logging.WatchLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The global {@code index.json} mapping each active {@code item_uid} to the operator who
 * spawned it. This lets the full history of any marked item be located in O(1) without scanning
 * every operator log.
 *
 * <p><b>Atomicity:</b> all persistence runs on a single-threaded executor, so writes are
 * serialised, and each write goes to a temporary sibling file that is then atomically renamed
 * over {@code index.json}. The live file is therefore never observed half-written, even across a
 * crash. The in-memory map is a {@link ConcurrentHashMap} so reads/updates from game and tracking
 * threads are safe; the executor only ever serialises an immutable snapshot.</p>
 */
public final class TrackingIndex {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TrackingIndex INSTANCE = new TrackingIndex();

    private static final Type MAP_TYPE = new TypeToken<HashMap<String, IndexEntry>>() {
    }.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final ConcurrentHashMap<String, IndexEntry> entries = new ConcurrentHashMap<>();
    private final ExecutorService persistExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "FantasticWatch-IndexWriter");
                t.setDaemon(true);
                return t;
            });

    private volatile Path indexFile;

    private TrackingIndex() {
    }

    public static TrackingIndex get() {
        return INSTANCE;
    }

    /** Serialisable index entry. Field names intentionally match the JSON schema. */
    public static final class IndexEntry {
        public String spawned_by_uuid;
        public String spawned_by_name;
        public String item_id;
        public int quantity;
        public String spawned_at;
        public String log_file;

        public IndexEntry() {
            // Required by Gson.
        }

        public IndexEntry(String spawnedByUuid, String spawnedByName, String itemId, int quantity,
                          String spawnedAt, String logFile) {
            this.spawned_by_uuid = spawnedByUuid;
            this.spawned_by_name = spawnedByName;
            this.item_id = itemId;
            this.quantity = quantity;
            this.spawned_at = spawnedAt;
            this.log_file = logFile;
        }
    }

    /**
     * Loads the index from disk (called on server start). Missing/corrupt files start empty and
     * are reported to the system log rather than crashing.
     */
    public synchronized void load(Path file) {
        this.indexFile = file;
        entries.clear();
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return;
            }
            Map<String, IndexEntry> loaded = gson.fromJson(json, MAP_TYPE);
            if (loaded != null) {
                loaded.forEach((k, v) -> {
                    if (k != null && v != null) {
                        entries.put(k, v);
                    }
                });
            }
            LOGGER.info("[FantasticWatch] Loaded {} index entries from {}", entries.size(), file);
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("[FantasticWatch] Failed to load index {}, starting empty", file, e);
            WatchLogger.get().system("[INDEX_LOAD_ERROR] file=" + file + " error=" + e.getMessage());
        }
    }

    public boolean contains(String uid) {
        return uid != null && entries.containsKey(uid);
    }

    public IndexEntry get(String uid) {
        return uid == null ? null : entries.get(uid);
    }

    /** Adds/updates an entry and schedules an atomic persist. */
    public void put(String uid, IndexEntry entry) {
        if (uid == null || entry == null) {
            return;
        }
        entries.put(uid, entry);
        schedulePersist();
    }

    /** Removes an entry and schedules an atomic persist. */
    public void remove(String uid) {
        if (uid == null) {
            return;
        }
        if (entries.remove(uid) != null) {
            schedulePersist();
        }
    }

    /** Removes many entries (used by the weekly purge) with a single persist. */
    public int removeAll(Iterable<String> uids) {
        int removed = 0;
        for (String uid : uids) {
            if (uid != null && entries.remove(uid) != null) {
                removed++;
            }
        }
        if (removed > 0) {
            schedulePersist();
        }
        return removed;
    }

    /** @return an immutable snapshot copy of all entries (safe to iterate). */
    public Map<String, IndexEntry> snapshot() {
        return new HashMap<>(entries);
    }

    private void schedulePersist() {
        if (persistExecutor.isShutdown()) {
            return;
        }
        try {
            persistExecutor.submit(this::persistNow);
        } catch (RuntimeException e) {
            LOGGER.error("[FantasticWatch] Could not schedule index persist", e);
        }
    }

    /** Serialises a snapshot and writes it atomically. Runs only on the index writer thread. */
    private void persistNow() {
        Path file = this.indexFile;
        if (file == null) {
            return;
        }
        Map<String, IndexEntry> snapshot = new HashMap<>(entries);
        String json = gson.toJson(snapshot, MAP_TYPE);
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
            Files.write(tmp, json.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException atomicUnsupported) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("[FantasticWatch] Failed to persist index {}", file, e);
            WatchLogger.get().system("[INDEX_PERSIST_ERROR] file=" + file + " error=" + e.getMessage());
        }
    }

    /** Flushes a final persist synchronously and stops the writer thread. */
    public void shutdownAndFlush() {
        try {
            persistExecutor.submit(this::persistNow);
        } catch (RuntimeException ignoredBecauseShuttingDown) {
            LOGGER.debug("[FantasticWatch] Index persist not scheduled during shutdown");
        }
        persistExecutor.shutdown();
        try {
            if (!persistExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                persistExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            persistExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
