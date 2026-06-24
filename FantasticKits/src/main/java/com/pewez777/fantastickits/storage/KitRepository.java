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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.mojang.logging.LogUtils;
import com.pewez777.fantastickits.kits.Kit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import org.slf4j.Logger;

/**
 * Persists kits as individual compressed-NBT files under
 * {@code config/fantastickits/kits/}.
 *
 * <p>Writes are atomic: data is first written to a temporary file and then
 * moved into place, so an interrupted save can never corrupt an existing kit.</p>
 */
public final class KitRepository {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EXTENSION = ".dat";
    private static final String ROOT_KEY = "FantasticKit";
    private static final String FORMAT_KEY = "Format";
    private static final int CURRENT_FORMAT = 1;

    public KitRepository() {
        StoragePaths.ensureDirectories();
    }

    /** Loads every persisted kit from disk. Corrupt files are skipped, not fatal. */
    public List<Kit> loadAll() {
        List<Kit> kits = new ArrayList<>();
        Path dir = StoragePaths.kitsDir();
        if (!Files.isDirectory(dir)) {
            return kits;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*" + EXTENSION)) {
            for (Path file : stream) {
                Kit kit = readFile(file);
                if (kit != null) {
                    kits.add(kit);
                }
            }
        } catch (IOException e) {
            LOGGER.error("[F-Kits] Failed to list kit directory {}", dir, e);
        }
        return kits;
    }

    private Kit readFile(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            CompoundTag root = NbtIo.readCompressed(in);
            CompoundTag kitTag = root.getCompound(ROOT_KEY);
            return Kit.fromNbt(kitTag);
        } catch (Exception e) {
            LOGGER.error("[F-Kits] Skipping corrupt kit file {}", file, e);
            return null;
        }
    }

    /** Atomically writes a kit to disk using its normalized storage key. */
    public boolean save(Kit kit) {
        if (kit == null) {
            return false;
        }
        StoragePaths.ensureDirectories();
        Path target = StoragePaths.kitsDir().resolve(kit.storageKey() + EXTENSION);
        Path temp = StoragePaths.kitsDir().resolve(kit.storageKey() + EXTENSION + ".tmp");

        CompoundTag root = new CompoundTag();
        root.putInt(FORMAT_KEY, CURRENT_FORMAT);
        root.put(ROOT_KEY, kit.toNbt());

        try (OutputStream out = Files.newOutputStream(temp)) {
            NbtIo.writeCompressed(root, out);
        } catch (IOException e) {
            LOGGER.error("[F-Kits] Failed to write kit {}", kit.getName(), e);
            return false;
        }

        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            LOGGER.error("[F-Kits] Failed to commit kit file {}", target, e);
            return false;
        }
    }

    /** Deletes the persisted file for the given storage key, if present. */
    public boolean delete(String storageKey) {
        if (storageKey == null || storageKey.isEmpty()) {
            return false;
        }
        Path target = StoragePaths.kitsDir().resolve(storageKey + EXTENSION);
        try {
            return Files.deleteIfExists(target);
        } catch (IOException e) {
            LOGGER.error("[F-Kits] Failed to delete kit file {}", target, e);
            return false;
        }
    }
}
