package com.fantastickits.integration;

import com.fantastickits.FantasticKits;
import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.UUID;

/**
 * Facade for group/permission lookups.
 *
 * This class contains NO direct references to the LuckPerms API, so it always
 * loads safely. It decides at runtime which backend to use:
 *
 * - If the "luckperms" mod is loaded, it instantiates {@link LuckPermsGroupProvider}
 *   (which references the API) inside a guarded try/catch that also catches
 *   {@link Throwable} — this prevents a NoClassDefFoundError / LinkageError from
 *   crashing the server if the API jar is somehow missing at runtime.
 * - Otherwise it falls back to {@link NoOpGroupProvider}, disabling group features.
 *
 * The public method surface is identical to the original implementation so that
 * callers (SecurityManager, CommandRestrictionHandler, TestCommand) need no changes.
 */
public class LuckPermsIntegration {

    private static final String LUCKPERMS_MOD_ID = "luckperms";

    private final GroupProvider provider;

    public LuckPermsIntegration() {
        GroupProvider selected;
        if (isLuckPermsModPresent()) {
            selected = tryCreateLuckPermsProvider();
        } else {
            FantasticKits.LOGGER.warn("FantasticKits: LuckPerms mod not detected. Group-based features are disabled.");
            selected = new NoOpGroupProvider();
        }
        this.provider = selected;
    }

    /**
     * Checks whether the LuckPerms mod is loaded WITHOUT touching the LuckPerms API.
     */
    private static boolean isLuckPermsModPresent() {
        try {
            return ModList.get() != null && ModList.get().isLoaded(LUCKPERMS_MOD_ID);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Attempts to instantiate the real LuckPerms-backed provider.
     * Catches Throwable so that a missing/incompatible API jar (NoClassDefFoundError,
     * LinkageError) or an uninitialized provider (IllegalStateException) degrades
     * gracefully to the no-op provider instead of crashing the server.
     */
    private static GroupProvider tryCreateLuckPermsProvider() {
        try {
            GroupProvider lp = new LuckPermsGroupProvider();
            FantasticKits.LOGGER.info("FantasticKits: LuckPerms integration loaded successfully.");
            return lp;
        } catch (Throwable t) {
            FantasticKits.LOGGER.warn("FantasticKits: LuckPerms is present but its API could not be initialized ({}). " +
                    "Group-based features are disabled.", t.getClass().getSimpleName());
            return new NoOpGroupProvider();
        }
    }

    /**
     * Check if LuckPerms is available on this server.
     */
    public boolean isAvailable() {
        return provider.isAvailable();
    }

    /**
     * Get all registered group names from LuckPerms in real-time.
     */
    public List<String> getAllGroups() {
        return provider.getAllGroups();
    }

    /**
     * Check if a player belongs to a specific group (direct or inherited).
     */
    public boolean playerInGroup(UUID playerUUID, String groupName) {
        return provider.playerInGroup(playerUUID, groupName);
    }

    /**
     * Get all groups a player belongs to.
     */
    public List<String> getPlayerGroups(UUID playerUUID) {
        return provider.getPlayerGroups(playerUUID);
    }

    /**
     * Get the primary group of a player.
     */
    public String getPrimaryGroup(UUID playerUUID) {
        return provider.getPrimaryGroup(playerUUID);
    }

    /**
     * Check if a group exists in LuckPerms.
     */
    public boolean groupExists(String groupName) {
        return provider.groupExists(groupName);
    }

    /**
     * Get the display name of a group (if set), otherwise the group name.
     */
    public String getGroupDisplayName(String groupName) {
        return provider.getGroupDisplayName(groupName);
    }
}
