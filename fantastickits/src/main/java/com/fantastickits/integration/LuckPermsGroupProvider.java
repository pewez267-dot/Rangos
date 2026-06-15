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

/**
 * Real {@link GroupProvider} backed by the official LuckPerms API for Forge.
 *
 * IMPORTANT: This class references LuckPerms classes directly. It must ONLY be
 * instantiated after confirming LuckPerms is loaded (see {@link LuckPermsIntegration}).
 * If LuckPerms is absent, instantiating this class would throw NoClassDefFoundError.
 *
 * All operations are read-only; this mod never modifies LuckPerms data.
 * Groups are always read at runtime — never hardcoded.
 */
public class LuckPermsGroupProvider implements GroupProvider {

    private final LuckPerms luckPerms;

    public LuckPermsGroupProvider() {
        // This call links the LuckPerms API. Only reached when the mod is present.
        this.luckPerms = LuckPermsProvider.get();
    }

    @Override
    public boolean isAvailable() {
        return luckPerms != null;
    }

    @Override
    public List<String> getAllGroups() {
        if (luckPerms == null) {
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

    @Override
    public boolean playerInGroup(UUID playerUUID, String groupName) {
        if (luckPerms == null) {
            return false;
        }
        User user = resolveUser(playerUUID);
        if (user == null) {
            return false;
        }

        String primaryGroup = user.getPrimaryGroup();
        if (primaryGroup != null && primaryGroup.equalsIgnoreCase(groupName)) {
            return true;
        }

        Collection<Node> nodes = user.getNodes();
        for (Node node : nodes) {
            if (node instanceof InheritanceNode inheritanceNode) {
                if (inheritanceNode.getGroupName().equalsIgnoreCase(groupName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<String> getPlayerGroups(UUID playerUUID) {
        if (luckPerms == null) {
            return Collections.emptyList();
        }
        User user = resolveUser(playerUUID);
        if (user == null) {
            return Collections.emptyList();
        }

        List<String> groups = new ArrayList<>();
        String primary = user.getPrimaryGroup();
        if (primary != null) {
            groups.add(primary);
        }

        for (Node node : user.getNodes()) {
            if (node instanceof InheritanceNode inheritanceNode) {
                String gName = inheritanceNode.getGroupName();
                if (!groups.contains(gName)) {
                    groups.add(gName);
                }
            }
        }
        return groups;
    }

    @Override
    public String getPrimaryGroup(UUID playerUUID) {
        if (luckPerms == null) {
            return "default";
        }
        User user = resolveUser(playerUUID);
        return user != null ? user.getPrimaryGroup() : "default";
    }

    @Override
    public boolean groupExists(String groupName) {
        if (luckPerms == null) {
            return false;
        }
        return luckPerms.getGroupManager().getGroup(groupName) != null;
    }

    @Override
    public String getGroupDisplayName(String groupName) {
        if (luckPerms == null) {
            return groupName;
        }
        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            return groupName;
        }
        String displayName = group.getDisplayName();
        return displayName != null ? displayName : group.getName();
    }

    /**
     * Resolve a User, loading from storage if not already cached.
     */
    private User resolveUser(UUID playerUUID) {
        UserManager userManager = luckPerms.getUserManager();
        User user = userManager.getUser(playerUUID);
        if (user == null) {
            try {
                user = userManager.loadUser(playerUUID).join();
            } catch (Exception e) {
                FantasticKits.LOGGER.error("Failed to load LuckPerms user for UUID: {}", playerUUID, e);
                return null;
            }
        }
        return user;
    }
}
