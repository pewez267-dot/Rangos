/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import org.slf4j.Logger;

/**
 * Persists per-player claim/history data under
 * {@code config/fantastickits/players/<uuid>.dat}.
 *
 * <p>An in-memory cache keyed by UUID keeps reads cheap and ensures that
 * concurrent claim attempts operate on the same authoritative object, which is
 * essential to the race-condition / double-claim protections.</p>
 */
public final class PlayerDataRepository {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EXTENSION = ".dat";

    private final ConcurrentHashMap<UUID, PlayerKitData> cache = new ConcurrentHashMap<>();

    public PlayerDataRepository() {
        StoragePaths.ensureDirectories();
    }

    private Path fileFor(UUID id) {
        return StoragePaths.playersDir().resolve(id.toString() + EXTENSION);
    }

    /** Returns the cached data for a player, loading it from disk on first use. */
    public PlayerKitData get(UUID id) {
        return cache.computeIfAbsent(id, this::loadFromDisk);
    }

    private PlayerKitData loadFromDisk(UUID id) {
        Path file = fileFor(id);
        if (!Files.isRegularFile(file)) {
            return new PlayerKitData(id);
        }
        try (InputStream in = Files.newInputStream(file)) {
            CompoundTag tag = NbtIo.readCompressed(in);
            return PlayerKitData.fromNbt(id, tag);
        } catch (Exception e) {
            LOGGER.error("[F-Kits] Failed to read player data {}; starting fresh.", file, e);
            return new PlayerKitData(id);
        }
    }

    /** Atomically persists a player's data to disk. */
    public synchronized boolean save(PlayerKitData data) {
        if (data == null) {
            return false;
        }
        StoragePaths.ensureDirectories();
        cache.put(data.getPlayerId(), data);

        Path target = fileFor(data.getPlayerId());
        Path temp = StoragePaths.playersDir().resolve(data.getPlayerId() + EXTENSION + ".tmp");
        try (OutputStream out = Files.newOutputStream(temp)) {
            NbtIo.writeCompressed(data.toNbt(), out);
        } catch (IOException e) {
            LOGGER.error("[F-Kits] Failed to write player data for {}", data.getPlayerId(), e);
            return false;
        }
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            LOGGER.error("[F-Kits] Failed to commit player data file {}", target, e);
            return false;
        }
    }

    /** Drops the in-memory cache (used on server stop). */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Removes every claim record for a deleted kit across all player files and
     * the in-memory cache. Best-effort: corrupt files are skipped.
     *
     * @param kitId the internal id of the deleted kit
     * @return the number of player records updated
     */
    public synchronized int purgeKitClaims(String kitId) {
        if (kitId == null || kitId.isEmpty()) {
            return 0;
        }
        int updated = 0;

        // Cached players first.
        for (PlayerKitData data : cache.values()) {
            if (data.removeClaim(kitId)) {
                save(data);
                updated++;
            }
        }

        Path dir = StoragePaths.playersDir();
        if (!Files.isDirectory(dir)) {
            return updated;
        }
        try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*" + EXTENSION)) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                String idText = fileName.substring(0, fileName.length() - EXTENSION.length());
                UUID id;
                try {
                    id = UUID.fromString(idText);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                if (cache.containsKey(id)) {
                    continue; // already handled above
                }
                PlayerKitData data = loadFromDisk(id);
                if (data.removeClaim(kitId)) {
                    save(data);
                    cache.remove(id); // keep cache lean; will reload on demand
                    updated++;
                }
            }
        } catch (IOException e) {
            LOGGER.error("[F-Kits] Failed to purge claims for kit {}", kitId, e);
        }
        return updated;
    }
}
