package com.fantastickits.integration;

import java.util.List;
import java.util.UUID;

/**
 * Abstraction over a permissions/group backend.
 * This interface contains NO references to any external API, so it is always
 * safe to load regardless of whether LuckPerms is installed.
 *
 * Two implementations exist:
 * - {@link LuckPermsGroupProvider}: backed by the real LuckPerms API (only loaded when present)
 * - {@link NoOpGroupProvider}: a safe fallback used when LuckPerms is not installed
 */
public interface GroupProvider {

    /**
     * @return true if a real permissions backend is available.
     */
    boolean isAvailable();

    /**
     * @return a sorted list of all registered group names (empty if unavailable).
     */
    List<String> getAllGroups();

    /**
     * @return true if the player belongs to (or inherits) the given group.
     */
    boolean playerInGroup(UUID playerUUID, String groupName);

    /**
     * @return all group names a player belongs to (empty if unavailable).
     */
    List<String> getPlayerGroups(UUID playerUUID);

    /**
     * @return the player's primary group, or "default" if unavailable.
     */
    String getPrimaryGroup(UUID playerUUID);

    /**
     * @return true if the group exists in the backend.
     */
    boolean groupExists(String groupName);

    /**
     * @return the display name of a group, or the group name if none set.
     */
    String getGroupDisplayName(String groupName);
}
