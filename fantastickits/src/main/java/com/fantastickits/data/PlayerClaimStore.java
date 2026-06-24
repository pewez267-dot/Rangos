package com.fantastickits.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which kits each player (by {@link UUID}) has already claimed, persisted to
 * {@code config/fantastickits/players.json}.
 *
 * <p>The single source of truth for the "one claim per player" rule. The
 * {@link #tryClaim(UUID, String, String)} method performs an <strong>atomic</strong>
 * check-and-set under the store's monitor, which is what protects against duplicate
 * claims from rapid/duplicated packets (race conditions and packet spoofing): the
 * second caller observes the kit as already claimed and is rejected.</p>
 */
public final class PlayerClaimStore {

    private static final PlayerClaimStore INSTANCE = new PlayerClaimStore();

    /** uuid -> record. */
    private final Map<UUID, Record> records = new LinkedHashMap<>();
    private boolean loaded = false;

    private PlayerClaimStore() {
    }

    public static PlayerClaimStore get() {
        synchronized (INSTANCE) {
            if (!INSTANCE.loaded) {
                INSTANCE.load();
                INSTANCE.loaded = true;
            }
        }
        return INSTANCE;
    }

    private static final class Record {
        String name;
        final Set<String> claimed = new LinkedHashSet<>();
    }

    public synchronized void load() {
        this.records.clear();
        final JsonObject root = JsonIO.read(DataPaths.players());
        if (!root.has("players") || !root.get("players").isJsonObject()) {
            return;
        }
        final JsonObject players = root.getAsJsonObject("players");
        for (final Map.Entry<String, JsonElement> entry : players.entrySet()) {
            try {
                final UUID uuid = UUID.fromString(entry.getKey());
                final JsonObject obj = entry.getValue().getAsJsonObject();
                final Record record = new Record();
                record.name = obj.has("name") ? obj.get("name").getAsString() : "";
                if (obj.has("claimed") && obj.get("claimed").isJsonArray()) {
                    for (final JsonElement kitId : obj.getAsJsonArray("claimed")) {
                        record.claimed.add(kitId.getAsString().toLowerCase());
                    }
                }
                this.records.put(uuid, record);
            } catch (final Exception ignored) {
                // Skip malformed entries rather than aborting the whole load.
            }
        }
    }

    public synchronized void save() {
        final JsonObject root = new JsonObject();
        final JsonObject players = new JsonObject();
        for (final Map.Entry<UUID, Record> entry : this.records.entrySet()) {
            final Record record = entry.getValue();
            final JsonObject obj = new JsonObject();
            obj.addProperty("name", record.name == null ? "" : record.name);
            final JsonArray claimed = new JsonArray();
            for (final String kitId : record.claimed) {
                claimed.add(kitId);
            }
            obj.add("claimed", claimed);
            players.add(entry.getKey().toString(), obj);
        }
        root.add("players", players);
        JsonIO.write(DataPaths.players(), root);
    }

    public synchronized boolean hasClaimed(final UUID uuid, final String kitId) {
        final Record record = this.records.get(uuid);
        return record != null && record.claimed.contains(kitId.toLowerCase());
    }

    /**
     * Atomically records a claim for {@code uuid} on {@code kitId}.
     *
     * @return {@code true} if the claim was newly recorded; {@code false} if the
     *         player had already claimed this kit (in which case no change is made).
     */
    public synchronized boolean tryClaim(final UUID uuid, final String playerName, final String kitId) {
        final String key = kitId.toLowerCase();
        final Record record = this.records.computeIfAbsent(uuid, u -> new Record());
        record.name = playerName;
        if (record.claimed.contains(key)) {
            return false;
        }
        record.claimed.add(key);
        save();
        return true;
    }

    /** Administrative reset of a single claim, allowing the player to claim it again. */
    public synchronized boolean resetClaim(final UUID uuid, final String kitId) {
        final Record record = this.records.get(uuid);
        if (record == null) {
            return false;
        }
        final boolean removed = record.claimed.remove(kitId.toLowerCase());
        if (removed) {
            save();
        }
        return removed;
    }

    public synchronized Set<String> claimedKits(final UUID uuid) {
        final Record record = this.records.get(uuid);
        return record == null ? new LinkedHashSet<>() : new LinkedHashSet<>(record.claimed);
    }
}
