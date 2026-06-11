package com.gbaminecraft.minecraft.client;

import com.gbaminecraft.emulator.input.GBAInput;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Key mapping (GBA button -> GLFW key code) with JSON-free persistence to the
 * config directory. Index = GBAInput key constant (0..9).
 */
public final class GBAKeyConfig {

    public static final String[] LABELS = {
        "A", "B", "Select", "Start", "Right", "Left", "Up", "Down", "R", "L"
    };

    private static final int[] DEFAULTS = {
        GLFW.GLFW_KEY_X,         // A
        GLFW.GLFW_KEY_Z,         // B
        GLFW.GLFW_KEY_BACKSPACE, // Select
        GLFW.GLFW_KEY_ENTER,     // Start
        GLFW.GLFW_KEY_RIGHT,     // Right
        GLFW.GLFW_KEY_LEFT,      // Left
        GLFW.GLFW_KEY_UP,        // Up
        GLFW.GLFW_KEY_DOWN,      // Down
        GLFW.GLFW_KEY_S,         // R
        GLFW.GLFW_KEY_A          // L
    };

    private static final int[] MAP = DEFAULTS.clone();
    private static boolean loaded = false;

    private GBAKeyConfig() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("fantasticboyadvance_keys.txt");
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try {
            Path f = file();
            if (Files.exists(f)) {
                for (String line : Files.readAllLines(f)) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] kv = line.split("=");
                    if (kv.length == 2) {
                        int idx = Integer.parseInt(kv[0].trim());
                        int key = Integer.parseInt(kv[1].trim());
                        if (idx >= 0 && idx < MAP.length) MAP[idx] = key;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void save() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# Fantastic Boy Advance key bindings (gbaKeyIndex=glfwKeyCode)\n");
            for (int i = 0; i < MAP.length; i++) {
                sb.append(i).append('=').append(MAP[i]).append('\n');
            }
            Files.writeString(file(), sb.toString());
        } catch (Exception ignored) {}
    }

    public static int getKey(int gbaIndex)        { return MAP[gbaIndex]; }
    public static void setKey(int gbaIndex, int k) { MAP[gbaIndex] = k; save(); }

    public static void resetDefaults() {
        System.arraycopy(DEFAULTS, 0, MAP, 0, MAP.length);
        save();
    }

    /** Returns the GBA key constant bound to a GLFW key code, or -1. */
    public static int gbaKeyForGlfw(int glfwKey) {
        for (int i = 0; i < MAP.length; i++) if (MAP[i] == glfwKey) return i;
        return -1;
    }

    /** Human-readable name for a GLFW key code. */
    public static String keyName(int glfwKey) {
        String n = GLFW.glfwGetKeyName(glfwKey, 0);
        if (n != null) return n.toUpperCase();
        switch (glfwKey) {
            case GLFW.GLFW_KEY_ENTER:     return "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE: return "BKSP";
            case GLFW.GLFW_KEY_SPACE:     return "SPACE";
            case GLFW.GLFW_KEY_RIGHT:     return "RIGHT";
            case GLFW.GLFW_KEY_LEFT:      return "LEFT";
            case GLFW.GLFW_KEY_UP:        return "UP";
            case GLFW.GLFW_KEY_DOWN:      return "DOWN";
            case GLFW.GLFW_KEY_LEFT_SHIFT:return "LSHIFT";
            default: return "KEY#" + glfwKey;
        }
    }
}
