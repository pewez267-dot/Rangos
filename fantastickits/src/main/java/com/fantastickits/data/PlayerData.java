package com.fantastickits.data;

import com.fantastickits.FantasticKits;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which kits each player (by UUID) has claimed.
 * Persists to config/fantastickits/players.json
 */
public class PlayerData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;
    // Map of UUID -> Set of kit names (lowercase) that have been claimed
    private final Map<UUID, Set<String>> claimedKits = new ConcurrentHashMap<>();

    public PlayerData() {
        this.filePath = DataPaths.getConfigDir().resolve("players.json");
    }

    public synchronized void load() {
        claimedKits.clear();
        if (!Files.exists(filePath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(filePath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                UUID uuid = UUID.fromString(entry.getKey());
                JsonArray arr = entry.getValue().getAsJsonArray();
                Set<String> kits = new HashSet<>();
                for (int i = 0; i < arr.size(); i++) {
                    kits.add(arr.get(i).getAsString().toLowerCase());
                }
                claimedKits.put(uuid, kits);
            }
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Failed to load players.json", e);
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(filePath.getParent());
            JsonObject root = new JsonObject();
            for (Map.Entry<UUID, Set<String>> entry : claimedKits.entrySet()) {
                JsonArray arr = new JsonArray();
                for (String kit : entry.getValue()) {
                    arr.add(kit);
                }
                root.add(entry.getKey().toString(), arr);
            }
            try (Writer writer = Files.newBufferedWriter(filePath)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Failed to save players.json", e);
        }
    }

    /**
     * Check if a player has already claimed a specific kit.
     */
    public boolean hasClaimed(UUID playerUUID, String kitName) {
        Set<String> claimed = claimedKits.get(playerUUID);
        if (claimed == null) return false;
        return claimed.contains(kitName.toLowerCase());
    }

    /**
     * Mark a kit as claimed by a player. Thread-safe with synchronized save.
     */
    public synchronized void markClaimed(UUID playerUUID, String kitName) {
        claimedKits.computeIfAbsent(playerUUID, k -> ConcurrentHashMap.newKeySet())
                .add(kitName.toLowerCase());
        save();
    }

    /**
     * Reset a player's claim for a specific kit (admin use only).
     */
    public synchronized void resetClaim(UUID playerUUID, String kitName) {
        Set<String> claimed = claimedKits.get(playerUUID);
        if (claimed != null) {
            claimed.remove(kitName.toLowerCase());
            save();
        }
    }

    /**
     * Get all kits claimed by a player.
     */
    public Set<String> getClaimedKits(UUID playerUUID) {
        Set<String> claimed = claimedKits.get(playerUUID);
        return claimed != null ? new HashSet<>(claimed) : new HashSet<>();
    }
}
