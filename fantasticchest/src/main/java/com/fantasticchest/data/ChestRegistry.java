package com.fantasticchest.data;

import com.fantasticchest.FantasticChest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative in-memory index of every chest definition, keyed by id, backed by
 * {@code chests.json}.
 *
 * <p>Loaded exactly once at server start; all gameplay reads hit this map (never the
 * disk). Mutations set a dirty flag and queue an asynchronous save via
 * {@link ChestSerializer}; {@link #flush()} guarantees a final write on shutdown.</p>
 */
public final class ChestRegistry {

    private static final ChestRegistry INSTANCE = new ChestRegistry();

    private final ChestSerializer serializer = new ChestSerializer();
    private final Map<String, ChestDefinition> chests = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;
    private volatile boolean dirty = false;

    private ChestRegistry() {
    }

    public static ChestRegistry get() {
        return INSTANCE;
    }

    public static String normalizeId(final String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    /** Loads chests.json into memory. Called once on server start. */
    public synchronized void load() {
        this.chests.clear();
        for (final ChestDefinition d : this.serializer.load()) {
            this.chests.put(normalizeId(d.id), d);
        }
        this.loaded = true;
        this.dirty = false;
        FantasticChest.LOGGER.info("[FantasticChest] {} cofre(s) cargados.", this.chests.size());
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public ChestDefinition get(final String id) {
        return this.chests.get(normalizeId(id));
    }

    public boolean exists(final String id) {
        return this.chests.containsKey(normalizeId(id));
    }

    /** Inserts or replaces a definition and queues an async save. */
    public void put(final ChestDefinition def) {
        if (def == null || def.id == null || def.id.isBlank()) {
            return;
        }
        this.chests.put(normalizeId(def.id), def);
        markDirtyAndSave();
    }

    public boolean remove(final String id) {
        final boolean removed = this.chests.remove(normalizeId(id)) != null;
        if (removed) {
            markDirtyAndSave();
        }
        return removed;
    }

    public List<ChestDefinition> all() {
        final List<ChestDefinition> out = new ArrayList<>();
        for (final ChestDefinition d : this.chests.values()) {
            out.add(d.copy());
        }
        return out;
    }

    /** Marks the registry dirty and queues a write only when there was a real change. */
    private void markDirtyAndSave() {
        this.dirty = true;
        final List<ChestDefinition> snapshot = new ArrayList<>(this.chests.values());
        this.serializer.saveAsync(snapshot);
        this.dirty = false;
    }

    /** Forces a synchronous flush of any pending writes (server shutdown). */
    public void flush() {
        if (this.dirty) {
            this.serializer.saveAsync(new ArrayList<>(this.chests.values()));
            this.dirty = false;
        }
        this.serializer.shutdown();
    }
}
