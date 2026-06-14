package com.pewez.fantasticessentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pewez.fantasticessentials.EssentialsMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Handles loading/saving of player and server data. Data is stored inside the world save
 * directory under {@code essentials/}, so it travels with the world like vanilla data.
 */
public final class DataStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final LevelResource ROOT = LevelResource.ROOT;

    private static final Map<UUID, PlayerData> PLAYER_CACHE = new ConcurrentHashMap<>();
    private static ServerData serverData = new ServerData();
    private static Path dataDir;

    private DataStorage() {
    }

    public static void init(MinecraftServer server) {
        dataDir = server.getWorldPath(ROOT).resolve("fantasticessentials");
        try {
            Files.createDirectories(dataDir.resolve("playerdata"));
        } catch (IOException e) {
            EssentialsMod.LOGGER.error("Failed to create essentials data directory", e);
        }
        loadServerData();
        PLAYER_CACHE.clear();
    }

    public static ServerData serverData() {
        return serverData;
    }

    public static PlayerData playerData(ServerPlayer player) {
        return playerData(player.getUUID());
    }

    public static PlayerData playerData(UUID uuid) {
        return PLAYER_CACHE.computeIfAbsent(uuid, DataStorage::loadPlayerData);
    }

    private static Path playerFile(UUID uuid) {
        return dataDir.resolve("playerdata").resolve(uuid.toString() + ".json");
    }

    private static PlayerData loadPlayerData(UUID uuid) {
        Path file = playerFile(uuid);
        if (Files.exists(file)) {
            try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                PlayerData data = GSON.fromJson(content, PlayerData.class);
                if (data != null) {
                    return data;
                }
            } catch (Exception e) {
                EssentialsMod.LOGGER.error("Failed to read player data for {}", uuid, e);
            }
        }
        return new PlayerData();
    }

    public static void savePlayerData(UUID uuid) {
        PlayerData data = PLAYER_CACHE.get(uuid);
        if (data == null || dataDir == null) {
            return;
        }
        try {
            Path file = playerFile(uuid);
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            EssentialsMod.LOGGER.error("Failed to write player data for {}", uuid, e);
        }
    }

    private static void loadServerData() {
        Path file = dataDir.resolve("server.json");
        if (Files.exists(file)) {
            try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                ServerData data = GSON.fromJson(content, ServerData.class);
                serverData = data != null ? data : new ServerData();
            } catch (Exception e) {
                EssentialsMod.LOGGER.error("Failed to read server data", e);
                serverData = new ServerData();
            }
        } else {
            serverData = new ServerData();
        }
    }

    public static void saveServerData() {
        if (dataDir == null) {
            return;
        }
        try {
            Path file = dataDir.resolve("server.json");
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(serverData), StandardCharsets.UTF_8);
        } catch (IOException e) {
            EssentialsMod.LOGGER.error("Failed to write server data", e);
        }
    }

    public static void saveAll() {
        saveServerData();
        for (UUID uuid : PLAYER_CACHE.keySet()) {
            savePlayerData(uuid);
        }
    }

    public static void unload(UUID uuid) {
        savePlayerData(uuid);
        PLAYER_CACHE.remove(uuid);
    }

    /** Load player data for an offline player by UUID (used for /home of offline players, etc.). */
    public static PlayerData loadOffline(UUID uuid) {
        if (PLAYER_CACHE.containsKey(uuid)) {
            return PLAYER_CACHE.get(uuid);
        }
        return loadPlayerData(uuid);
    }

    public static Stream<UUID> knownPlayers() {
        return PLAYER_CACHE.keySet().stream();
    }
}
