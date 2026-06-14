package com.pewez.fantasticessentials.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players have command-spy enabled.
 */
public final class CommandSpy {

    private static final Set<UUID> ENABLED = new HashSet<>();

    private CommandSpy() {
    }

    public static boolean isEnabled(UUID uuid) {
        return ENABLED.contains(uuid);
    }

    public static boolean toggle(UUID uuid) {
        if (ENABLED.contains(uuid)) {
            ENABLED.remove(uuid);
            return false;
        }
        ENABLED.add(uuid);
        return true;
    }

    public static void set(UUID uuid, boolean enabled) {
        if (enabled) {
            ENABLED.add(uuid);
        } else {
            ENABLED.remove(uuid);
        }
    }

    public static Set<UUID> enabled() {
        return ENABLED;
    }

    public static void clear(UUID uuid) {
        ENABLED.remove(uuid);
    }
}
