package com.fantastickits.data;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

/**
 * Central place to resolve all config/data file paths.
 */
public final class DataPaths {

    private DataPaths() {}

    /**
     * Returns the base config directory for the mod: config/fantastickits/
     */
    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get().resolve("fantastickits");
    }
}
