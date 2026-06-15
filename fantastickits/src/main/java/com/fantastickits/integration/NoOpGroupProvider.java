package com.fantastickits.integration;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Fallback {@link GroupProvider} used when LuckPerms is NOT installed.
 * Contains zero references to the LuckPerms API, so it never triggers
 * class-loading errors. All group-based features are effectively disabled.
 */
public class NoOpGroupProvider implements GroupProvider {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<String> getAllGroups() {
        return Collections.emptyList();
    }

    @Override
    public boolean playerInGroup(UUID playerUUID, String groupName) {
        return false;
    }

    @Override
    public List<String> getPlayerGroups(UUID playerUUID) {
        return Collections.emptyList();
    }

    @Override
    public String getPrimaryGroup(UUID playerUUID) {
        return "default";
    }

    @Override
    public boolean groupExists(String groupName) {
        return false;
    }

    @Override
    public String getGroupDisplayName(String groupName) {
        return groupName;
    }
}
