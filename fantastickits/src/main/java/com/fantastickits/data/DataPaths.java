package com.fantastickits.data;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central definition of every on-disk file used by Fantastic Kits.
 *
 * <p>All data lives under {@code config/fantastickits/}:</p>
 * <ul>
 *     <li>{@code kits.json}          - full kit definitions (items, NBT, assigned group)</li>
 *     <li>{@code players.json}       - claim state per player UUID</li>
 *     <li>{@code group_commands.json}- group to allowed-commands mapping</li>
 *     <li>{@code audit.log}          - append-only audit trail</li>
 *     <li>{@code config.toml}        - general config (managed by the Forge Config API)</li>
 * </ul>
 */
public final class DataPaths {

    public static final String FOLDER = "fantastickits";

    private DataPaths() {
    }

    /** {@code config/fantastickits/}, created on demand. */
    public static Path baseDir() {
        final Path dir = FMLPaths.CONFIGDIR.get().resolve(FOLDER);
        try {
            Files.createDirectories(dir);
        } catch (final IOException e) {
            throw new IllegalStateException("[FantasticKits] No se pudo crear el directorio de datos: " + dir, e);
        }
        return dir;
    }

    public static Path kits() {
        return baseDir().resolve("kits.json");
    }

    public static Path players() {
        return baseDir().resolve("players.json");
    }

    public static Path groupCommands() {
        return baseDir().resolve("group_commands.json");
    }

    public static Path auditLog() {
        return baseDir().resolve("audit.log");
    }
}
