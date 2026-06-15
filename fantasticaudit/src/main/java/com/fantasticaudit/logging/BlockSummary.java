package com.fantasticaudit.logging;

import com.fantasticaudit.config.AuditConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Maintains a cumulative, per-player summary of mined blocks, written to its own text file at
 * {@code config/fantasticaudit/summaries/blocks/{UUID}.txt} (separate from the detailed audit log).
 *
 * <p>Each entry aggregates a {@code (block_id, tool_id)} pair with the total number of blocks
 * mined. All ids are full namespaced registry ids resolved upstream through {@code ForgeRegistries},
 * so modded blocks and modded tools are counted correctly.</p>
 *
 * <p>The file is both human-readable (aligned columns) and machine-parseable (values separated by
 * {@code  | }), so totals are reloaded on server start and keep accumulating across sessions and
 * restarts. Counters live in memory and are flushed to disk atomically (temp file + rename) by a
 * dedicated daemon thread on a fixed interval and once more on shutdown.</p>
 */
public final class BlockSummary {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BlockSummary INSTANCE = new BlockSummary();

    /** Separates block id and tool id inside an in-memory counter key (control char, never in ids). */
    private static final String KEY_SEP = "\u001f";
    private static final String FIELD_SEP = " | ";

    private final ConcurrentHashMap<UUID, PlayerBlockStats> stats = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    private volatile Path summariesDir;
    private volatile boolean running;
    private Thread flusher;

    private BlockSummary() {
    }

    public static BlockSummary get() {
        return INSTANCE;
    }

    /** Per-player aggregated counters plus the most recently seen display name. */
    private static final class PlayerBlockStats {
        private volatile String playerName;
        private final ConcurrentHashMap<String, AtomicLong> counts = new ConcurrentHashMap<>();

        private AtomicLong counter(String key) {
            return counts.computeIfAbsent(key, k -> new AtomicLong(0L));
        }
    }

    /**
     * Initialises the summary store: creates the directory, loads any existing summaries so totals
     * continue accumulating, and starts the background flusher. Idempotent.
     *
     * @param baseDir the mod base directory ({@code config/fantasticaudit})
     */
    public synchronized void init(Path baseDir) {
        if (running) {
            return;
        }
        this.summariesDir = baseDir.resolve("summaries").resolve("blocks");
        try {
            Files.createDirectories(summariesDir);
        } catch (IOException e) {
            LOGGER.error("[FantasticAudit] Could not create block summary directory {}", summariesDir, e);
        }
        loadExisting();

        running = true;
        flusher = new Thread(this::flushLoop, "FantasticAudit-BlockSummary");
        flusher.setDaemon(true);
        flusher.start();
    }

    /**
     * Records {@code quantity} mined blocks of {@code blockId} using {@code toolId} for a player.
     *
     * @param uuid       the mining player's UUID
     * @param playerName the mining player's display name
     * @param blockId    full registry id of the mined block
     * @param toolId     full registry id of the tool used (e.g. {@code minecraft:air} for hand)
     * @param quantity   number of blocks mined (typically 1 per break event)
     */
    public void record(UUID uuid, String playerName, String blockId, String toolId, long quantity) {
        if (!running || uuid == null || quantity <= 0) {
            return;
        }
        PlayerBlockStats playerStats = stats.computeIfAbsent(uuid, u -> new PlayerBlockStats());
        playerStats.playerName = playerName;
        playerStats.counter(blockId + KEY_SEP + toolId).addAndGet(quantity);
        dirty.add(uuid);
    }

    private void flushLoop() {
        long intervalMillis = Math.max(1L, AuditConfig.FLUSH_INTERVAL_SECONDS.get()) * 1000L;
        try {
            while (running) {
                Thread.sleep(intervalMillis);
                flushDirty();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            flushDirty();
        }
    }

    private void flushDirty() {
        // Snapshot and clear the dirty set; any concurrent record() re-adds the uuid for next pass.
        List<UUID> toWrite = new ArrayList<>(dirty);
        for (UUID uuid : toWrite) {
            dirty.remove(uuid);
            PlayerBlockStats playerStats = stats.get(uuid);
            if (playerStats != null) {
                writeSummary(uuid, playerStats);
            }
        }
    }

    private void writeSummary(UUID uuid, PlayerBlockStats playerStats) {
        Path dir = this.summariesDir;
        if (dir == null) {
            return;
        }
        Path file = dir.resolve(uuid.toString() + ".txt");
        String content = render(uuid, playerStats);
        try {
            Files.createDirectories(dir);
            Path temp = Files.createTempFile(dir, uuid.toString(), ".tmp");
            try {
                Files.write(temp, content.getBytes(StandardCharsets.UTF_8));
                try {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException atomicUnsupported) {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            LOGGER.error("[FantasticAudit] Failed to write block summary {}", file, e);
            AuditLogger.get().system("[BLOCK_SUMMARY_ERROR] file=" + file + " error=" + e.getMessage());
        }
    }

    /** Builds the aligned, parseable summary text for one player. */
    private String render(UUID uuid, PlayerBlockStats playerStats) {
        List<Map.Entry<String, AtomicLong>> entries = new ArrayList<>(playerStats.counts.entrySet());
        // Most-mined first, then by block/tool key for stable ordering.
        entries.sort(Comparator
                .comparingLong((Map.Entry<String, AtomicLong> e) -> e.getValue().get()).reversed()
                .thenComparing(Map.Entry::getKey));

        long grandTotal = 0L;
        int blockWidth = "BLOQUE".length();
        int qtyWidth = "CANTIDAD".length();
        List<String[]> rows = new ArrayList<>(entries.size());
        for (Map.Entry<String, AtomicLong> entry : entries) {
            String[] parts = entry.getKey().split(KEY_SEP, 2);
            String blockId = parts.length > 0 ? parts[0] : "unknown";
            String toolId = parts.length > 1 ? parts[1] : "unknown";
            long count = entry.getValue().get();
            grandTotal += count;
            rows.add(new String[]{blockId, Long.toString(count), toolId});
            blockWidth = Math.max(blockWidth, blockId.length());
            qtyWidth = Math.max(qtyWidth, Long.toString(count).length());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# Fantastic Audit - Resumen de bloques minados").append('\n');
        sb.append("# jugador=").append(playerStats.playerName == null ? "unknown" : playerStats.playerName)
                .append(" uuid=").append(uuid).append('\n');
        sb.append("# actualizado=").append(Instant.now().truncatedTo(ChronoUnit.SECONDS)).append('\n');
        sb.append("# total_bloques_minados=").append(grandTotal).append('\n');
        sb.append("# formato: BLOQUE").append(FIELD_SEP).append("CANTIDAD").append(FIELD_SEP).append("HERRAMIENTA")
                .append('\n');
        sb.append(pad("BLOQUE", blockWidth)).append(FIELD_SEP)
                .append(pad("CANTIDAD", qtyWidth)).append(FIELD_SEP)
                .append("HERRAMIENTA").append('\n');
        for (String[] row : rows) {
            sb.append(pad(row[0], blockWidth)).append(FIELD_SEP)
                    .append(pad(row[1], qtyWidth)).append(FIELD_SEP)
                    .append(row[2]).append('\n');
        }
        return sb.toString();
    }

    private static String pad(String value, int width) {
        if (value.length() >= width) {
            return value;
        }
        return value + " ".repeat(width - value.length());
    }

    /** Loads existing summary files so counters keep accumulating across restarts. */
    private void loadExisting() {
        Path dir = this.summariesDir;
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> txtFiles = files.filter(p -> p.getFileName().toString().endsWith(".txt")).toList();
            for (Path file : txtFiles) {
                loadFile(file);
            }
            LOGGER.info("[FantasticAudit] Loaded {} block-summary file(s) from {}", txtFiles.size(), dir);
        } catch (IOException e) {
            LOGGER.error("[FantasticAudit] Failed to list block-summary directory {}", dir, e);
        }
    }

    private void loadFile(Path file) {
        String fileName = file.getFileName().toString();
        String uuidPart = fileName.substring(0, fileName.length() - ".txt".length());
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidPart);
        } catch (IllegalArgumentException e) {
            return; // not a per-player summary file
        }
        PlayerBlockStats playerStats = new PlayerBlockStats();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                if (line.startsWith("#")) {
                    // Recover the last-known player name from the header comment.
                    int idx = line.indexOf("jugador=");
                    if (idx >= 0) {
                        String rest = line.substring(idx + "jugador=".length());
                        int sp = rest.indexOf(" uuid=");
                        playerStats.playerName = sp >= 0 ? rest.substring(0, sp) : rest.trim();
                    }
                    continue;
                }
                String[] cols = line.split("\\|");
                if (cols.length < 3) {
                    continue;
                }
                String blockId = cols[0].trim();
                String qtyStr = cols[1].trim();
                String toolId = cols[2].trim();
                if (blockId.equals("BLOQUE")) {
                    continue; // header row
                }
                long count;
                try {
                    count = Long.parseLong(qtyStr);
                } catch (NumberFormatException e) {
                    continue;
                }
                playerStats.counter(blockId + KEY_SEP + toolId).addAndGet(count);
            }
            if (!playerStats.counts.isEmpty()) {
                stats.put(uuid, playerStats);
            }
        } catch (IOException e) {
            LOGGER.error("[FantasticAudit] Failed to load block summary {}", file, e);
        }
    }

    /** Flushes all pending summaries and stops the background thread. */
    public synchronized void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        if (flusher != null) {
            flusher.interrupt();
            try {
                flusher.join(TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            flusher = null;
        }
        // Final guaranteed flush of everything still in memory.
        for (Map.Entry<UUID, PlayerBlockStats> entry : stats.entrySet()) {
            writeSummary(entry.getKey(), entry.getValue());
        }
    }
}
