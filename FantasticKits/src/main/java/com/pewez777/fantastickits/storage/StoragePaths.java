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
import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.loading.FMLPaths;

import org.slf4j.Logger;

/**
 * Centralizes every on-disk path used by Fantastic Kits under
 * {@code config/fantastickits/} and guarantees the directory tree exists.
 *
 * <pre>
 *   config/fantastickits/
 *   |- config.toml
 *   |- kits/        (one .dat file per kit)
 *   |- players/     (one .dat file per player: claims + history)
 *   '- audit/       (audit.log, security.log and rotated copies)
 * </pre>
 */
public final class StoragePaths {

    private static final Logger LOGGER = LogUtils.getLogger();

    private StoragePaths() {
    }

    public static Path root() {
        return FMLPaths.CONFIGDIR.get().resolve("fantastickits");
    }

    public static Path kitsDir() {
        return root().resolve("kits");
    }

    public static Path playersDir() {
        return root().resolve("players");
    }

    public static Path auditDir() {
        return root().resolve("audit");
    }

    /** Creates the full directory tree if necessary. Never throws. */
    public static void ensureDirectories() {
        createQuietly(root());
        createQuietly(kitsDir());
        createQuietly(playersDir());
        createQuietly(auditDir());
    }

    private static void createQuietly(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("[F-Kits] Could not create directory {}", dir, e);
        }
    }
}
