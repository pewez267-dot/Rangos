package com.fantastickits.integration;

import com.fantastickits.FantasticKits;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Integration with the official LuckPerms API for Forge.
 * Reads groups at runtime — never hardcoded.
 * All operations are read-only; this mod does not modify LuckPerms data.
 */
public class LuckPermsIntegration {

    private LuckPerms luckPerms;
    private boolean available;

    public LuckPermsIntegration() {
        try {
            this.luckPerms = LuckPermsProvider.get();
            this.available = true;
            FantasticKits.LOGGER.info("FantasticKits: LuckPerms integration loaded successfully.");
        } catch (IllegalStateException e) {
            this.luckPerms = null;
            this.available = false;
            FantasticKits.LOGGER.warn("FantasticKits: LuckPerms not available. Group-based features will be disabled.");
        }
    }

    /**
     * Check if LuckPerms is available on this server.
     */
    public boolean isAvailable() {
        return available && luckPerms != null;
    }

    /**
     * Get all registered group names from LuckPerms in real-time.
     * This is called dynamically each time the GUI needs to display groups.
     *
     * @return A sorted list of all group names
     */
    public List<String> getAllGroups() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }

        GroupManager groupManager = luckPerms.getGroupManager();
        Set<Group> loadedGroups = groupManager.getLoadedGroups();

        List<String> groupNames = new ArrayList<>();
        for (Group group : loadedGroups) {
            groupNames.add(group.getName());
        }
        Collections.sort(groupNames);
        return groupNames;
    }

    /**
     * Check if a player belongs to a specific group.
     * This checks both direct group membership and inherited groups.
     *
     * @param playerUUID The UUID of the player to check
     * @param groupName  The group name to check membership for
     * @return true if the player is in the specified group
     */
    public boolean playerInGroup(UUID playerUUID, String groupName) {
        if (!isAvailable()) {
            return false;
        }

        UserManager userManager = luckPerms.getUserManager();
        User user = userManager.getUser(playerUUID);

        if (user == null) {
            // User not loaded, attempt to load
            try {
                user = userManager.loadUser(playerUUID).join();
            } catch (Exception e) {
                FantasticKits.LOGGER.error("Failed to load LuckPerms user for UUID: {}", playerUUID, e);
                return false;
            }
        }

        if (user == null) {
            return false;
        }

        // Check if the user's primary group matches
        String primaryGroup = user.getPrimaryGroup();
        if (primaryGroup.equalsIgnoreCase(groupName)) {
            return true;
        }

        // Check inherited groups (direct membership nodes)
        Collection<Node> nodes = user.getNodes();
        for (Node node : nodes) {
            if (node instanceof InheritanceNode) {
                InheritanceNode inheritanceNode = (InheritanceNode) node;
                if (inheritanceNode.getGroupName().equalsIgnoreCase(groupName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get all groups a player belongs to.
     *
     * @param playerUUID The UUID of the player
     * @return A list of group names the player belongs to
     */
    public List<String> getPlayerGroups(UUID playerUUID) {
        if (!isAvailable()) {
            return Collections.emptyList();
        }

        UserManager userManager = luckPerms.getUserManager();
        User user = userManager.getUser(playerUUID);

        if (user == null) {
            try {
                user = userManager.loadUser(playerUUID).join();
            } catch (Exception e) {
                FantasticKits.LOGGER.error("Failed to load LuckPerms user for UUID: {}", playerUUID, e);
                return Collections.emptyList();
            }
        }

        if (user == null) {
            return Collections.emptyList();
        }

        List<String> groups = new ArrayList<>();
        groups.add(user.getPrimaryGroup());

        for (Node node : user.getNodes()) {
            if (node instanceof InheritanceNode) {
                InheritanceNode inheritanceNode = (InheritanceNode) node;
                String gName = inheritanceNode.getGroupName();
                if (!groups.contains(gName)) {
                    groups.add(gName);
                }
            }
        }

        return groups;
    }

    /**
     * Get the primary group of a player.
     *
     * @param playerUUID The UUID of the player
     * @return The primary group name, or "default" if not found
     */
    public String getPrimaryGroup(UUID playerUUID) {
        if (!isAvailable()) {
            return "default";
        }

        UserManager userManager = luckPerms.getUserManager();
        User user = userManager.getUser(playerUUID);

        if (user == null) {
            try {
                user = userManager.loadUser(playerUUID).join();
            } catch (Exception e) {
                FantasticKits.LOGGER.error("Failed to load LuckPerms user for UUID: {}", playerUUID, e);
                return "default";
            }
        }

        return user != null ? user.getPrimaryGroup() : "default";
    }

    /**
     * Check if a group exists in LuckPerms.
     *
     * @param groupName The name of the group to check
     * @return true if the group exists
     */
    public boolean groupExists(String groupName) {
        if (!isAvailable()) {
            return false;
        }

        GroupManager groupManager = luckPerms.getGroupManager();
        return groupManager.getGroup(groupName) != null;
    }

    /**
     * Get the display name of a group (if set), otherwise returns the group name.
     *
     * @param groupName The internal group name
     * @return The display name or the group name if no display name is set
     */
    public String getGroupDisplayName(String groupName) {
        if (!isAvailable()) {
            return groupName;
        }

        GroupManager groupManager = luckPerms.getGroupManager();
        Group group = groupManager.getGroup(groupName);
        if (group == null) {
            return groupName;
        }

        String displayName = group.getDisplayName();
        return displayName != null ? displayName : group.getName();
    }
}
