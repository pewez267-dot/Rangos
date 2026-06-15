package com.fantastic.kits.storage;

import com.fantastic.kits.FantasticKits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and persists {@link PlayerData} files, one per player UUID, under
 * {@code /config/fantastickits/players/}.
 * <p>
 * Files are loaded on demand the first time a player is seen, kept in a
 * concurrent cache, and flushed both on every claim and on shutdown.
 */
public final class PlayerDataManager {

    private final Path playersDir;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(Path playersDir) {
        this.playersDir = playersDir;
        try {
            Files.createDirectories(playersDir);
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Cannot create players directory {}", playersDir, e);
        }
    }

    /**
     * Returns the player record, lazily reading from disk if necessary.
     */
    public PlayerData get(UUID uuid, String name) {
        return cache.computeIfAbsent(uuid, id -> loadOrCreate(id, name));
    }

    public boolean hasClaimed(UUID uuid, String kitId) {
        PlayerData pd = cache.get(uuid);
        if (pd == null) pd = loadOrCreate(uuid, null);
        return pd.hasClaimed(kitId);
    }

    public boolean recordClaim(UUID uuid, String name, String kitId, long timestamp) {
        PlayerData pd = get(uuid, name);
        boolean ok = pd.recordClaim(kitId, timestamp);
        if (ok) save(pd); // eager persist - claims are permanent on success
        return ok;
    }

    public void recordDenied(UUID uuid, String name, String kitId, long timestamp) {
        PlayerData pd = get(uuid, name);
        pd.recordDenied(kitId, timestamp);
        save(pd);
    }

    public void flushAll() {
        for (PlayerData pd : cache.values()) save(pd);
    }

    // ------------------------------------------------------------------
    // Internal IO
    // ------------------------------------------------------------------

    private PlayerData loadOrCreate(UUID uuid, String name) {
        Path file = fileFor(uuid);
        if (Files.exists(file)) {
            try {
                CompoundTag tag = NbtIo.readCompressed(file.toFile());
                PlayerData pd = PlayerData.load(tag);
                if (name != null && !name.isBlank()) pd.lastKnownName(name);
                return pd;
            } catch (IOException e) {
                FantasticKits.LOGGER.error("Corrupt player file {}, recreating", file, e);
            }
        }
        return new PlayerData(uuid, name == null ? "" : name);
    }

    private void save(PlayerData pd) {
        try {
            Files.createDirectories(playersDir);
            NbtIo.writeCompressed(pd.save(), fileFor(pd.playerId()).toFile());
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Failed to save player data for {}", pd.playerId(), e);
        }
    }

    private Path fileFor(UUID uuid) {
        return playersDir.resolve(uuid + ".dat");
    }
}
