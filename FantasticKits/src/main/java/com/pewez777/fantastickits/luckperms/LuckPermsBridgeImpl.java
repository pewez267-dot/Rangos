/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.luckperms;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;

/**
 * The <strong>only</strong> class in Fantastic Kits permitted to import
 * {@code net.luckperms.api.*}.
 *
 * <p>It is never referenced statically anywhere else; {@link LuckPermsHook}
 * instantiates it by reflection only after confirming LuckPerms is present.
 * Consequently the JVM will not attempt to resolve any LuckPerms symbol when
 * the plugin is missing.</p>
 */
public final class LuckPermsBridgeImpl implements LuckPermsBridge {

    /** Prefix for every node this mod writes to LuckPerms groups. */
    private static final String NODE_PREFIX = "fantastickits.kit.";

    /** Public no-argument constructor required for reflective instantiation. */
    public LuckPermsBridgeImpl() {
    }

    private static LuckPerms api() {
        return LuckPermsProvider.get();
    }

    @Override
    public List<GroupInfo> getAllGroups() {
        List<GroupInfo> result = new ArrayList<>();
        LuckPerms lp = api();
        for (Group group : lp.getGroupManager().getLoadedGroups()) {
            int weight = group.getWeight().orElse(0);

            List<String> inherited = new ArrayList<>();
            for (InheritanceNode node : group.getNodes(NodeType.INHERITANCE)) {
                inherited.add(node.getGroupName());
            }

            List<String> permissions = new ArrayList<>();
            for (PermissionNode node : group.getNodes(NodeType.PERMISSION)) {
                permissions.add(node.getPermission());
            }

            result.add(new GroupInfo(
                    group.getName(),
                    group.getFriendlyName(),
                    weight,
                    inherited,
                    permissions));
        }
        return result;
    }

    @Override
    public Optional<String> getPrimaryGroup(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        LuckPerms lp = api();
        User user = lp.getUserManager().getUser(playerId);
        if (user == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(user.getPrimaryGroup());
    }

    @Override
    public void publishKitNodes(String groupName, String kitId, List<String> commands) {
        if (groupName == null || kitId == null) {
            return;
        }
        LuckPerms lp = api();
        Group group = lp.getGroupManager().getGroup(groupName);
        if (group == null) {
            return;
        }

        // Marker node: this group owns this kit.
        group.data().add(PermissionNode.builder(NODE_PREFIX + kitId).value(true).build());

        // One node per owned command.
        if (commands != null) {
            for (String command : commands) {
                String key = NODE_PREFIX + kitId + ".command." + sanitize(command);
                group.data().add(PermissionNode.builder(key).value(true).build());
            }
        }

        lp.getGroupManager().saveGroup(group);
    }

    @Override
    public void revokeKitNodes(String groupName, String kitId) {
        if (groupName == null || kitId == null) {
            return;
        }
        LuckPerms lp = api();
        Group group = lp.getGroupManager().getGroup(groupName);
        if (group == null) {
            return;
        }

        final String prefix = NODE_PREFIX + kitId;
        // NodeMap#clear(Predicate) returns void in the LuckPerms API - do NOT
        // assign its result.
        group.data().clear(node -> node.getKey().startsWith(prefix));

        lp.getGroupManager().saveGroup(group);
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return "unknown";
        }
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String out = sb.toString();
        return out.isEmpty() ? "unknown" : out;
    }
}
