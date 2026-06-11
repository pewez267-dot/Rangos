package com.gbaminecraft.minecraft.client;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages the "RomsGBA" folder located in the instance root directory, plus
 * the per-ROM battery saves ("RomsGBA/saves") and save-states ("RomsGBA/states").
 */
public final class RomLibrary {

    private RomLibrary() {}

    public static Path romsDir() {
        Path dir = FMLPaths.GAMEDIR.get().resolve("RomsGBA");
        try { Files.createDirectories(dir); } catch (Exception ignored) {}
        return dir;
    }

    public static Path savesDir() {
        Path dir = romsDir().resolve("saves");
        try { Files.createDirectories(dir); } catch (Exception ignored) {}
        return dir;
    }

    public static Path statesDir() {
        Path dir = romsDir().resolve("states");
        try { Files.createDirectories(dir); } catch (Exception ignored) {}
        return dir;
    }

    /** All *.gba files in the RomsGBA folder, sorted alphabetically. */
    public static List<File> listRoms() {
        List<File> roms = new ArrayList<>();
        File[] files = romsDir().toFile().listFiles();
        if (files != null) {
            for (File f : files) {
                String n = f.getName().toLowerCase();
                if (f.isFile() && (n.endsWith(".gba") || n.endsWith(".agb") || n.endsWith(".bin"))) {
                    roms.add(f);
                }
            }
        }
        roms.sort(Comparator.comparing(f -> f.getName().toLowerCase()));
        return roms;
    }

    public static File batteryFile(File rom) {
        return savesDir().resolve(baseName(rom) + ".sav").toFile();
    }

    public static File stateFile(File rom) {
        return statesDir().resolve(baseName(rom) + ".state").toFile();
    }

    public static String baseName(File rom) {
        String n = rom.getName();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }
}
