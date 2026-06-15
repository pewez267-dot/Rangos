package com.pewez.fantasticshortcuts.integration.luckperms;

import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import com.pewez.fantasticshortcuts.config.ModConfig;
import net.minecraftforge.fml.ModList;

import java.util.Optional;
import java.util.UUID;

/**
 * Optional, read-only integration with LuckPerms (the Forge mod).
 *
 * Implemented purely through reflection so the project has no compile-time dependency on LuckPerms.
 * If LuckPerms is not installed, every method degrades gracefully. This integration NEVER creates or
 * modifies permissions; it only reads a player's primary group for audit/display context.
 */
public final class LuckPermsIntegration {

    private static Boolean available;

    private LuckPermsIntegration() {
    }

    public static boolean isAvailable() {
        if (!ModConfig.LUCKPERMS_INTEGRATION.get()) {
            return false;
        }
        if (available == null) {
            available = ModList.get() != null && ModList.get().isLoaded("luckperms")
                    && classExists("net.luckperms.api.LuckPermsProvider");
            if (available) {
                FantasticShortcutsMod.LOGGER.info("LuckPerms detected - read-only integration enabled");
            }
        }
        return available;
    }

    /**
     * Returns the player's LuckPerms primary group name, if available.
     */
    public static Optional<String> getPrimaryGroup(UUID playerId) {
        if (!isAvailable() || playerId == null) {
            return Optional.empty();
        }
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = providerClass.getMethod("get").invoke(null);
            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, playerId);
            if (user == null) {
                return Optional.empty();
            }
            Object group = user.getClass().getMethod("getPrimaryGroup").invoke(user);
            return Optional.ofNullable((String) group);
        } catch (Throwable t) {
            // LuckPerms not present, user not loaded, or API changed - fail silently.
            return Optional.empty();
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, LuckPermsIntegration.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
