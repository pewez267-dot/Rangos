package com.fantastic.kits.luckperms;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.Reference;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Single integration point with LuckPerms.
 * <p>
 * Design rules:
 * <ul>
 *     <li>The mod must <b>never</b> crash when LuckPerms is missing.
 *         All LP code paths are guarded with {@link #available}.</li>
 *     <li>Kit access is gated by <b>primary group strict equality</b>; we never
 *         consult inheritance or weights for that check, even if LuckPerms has
 *         hierarchies set up. This is enforced by {@link #isPrimaryGroupExactly}.</li>
 *     <li>When a kit is saved, we publish kit/command nodes to the owning
 *         group through {@link #syncKitToGroup(String, String, List)} so other
 *         tools (web panels, audit trails) can inspect them.</li>
 * </ul>
 */
public final class LuckPermsHook {

    private static final String NODE_KIT = Reference.MOD_ID + ".kit.";          // <id>
    private static final String NODE_KIT_CMD = Reference.MOD_ID + ".cmd.";      // <kitId>.<cmd>

    private boolean available;
    private LuckPerms api;

    public boolean available() { return available; }
    public LuckPerms api() { return api; }

    /**
     * Tries to load the LuckPerms API. Errors are swallowed and downgrade to a
     * console warning so the server keeps running.
     */
    public void tryAttach() {
        try {
            this.api = LuckPermsProvider.get();
            this.available = (this.api != null);
            if (this.available) {
                int groups = this.api.getGroupManager().getLoadedGroups().size();
                FantasticKits.LOGGER.info("[{}] LuckPerms detected. Loaded groups: {}",
                        Reference.MOD_NAME, groups);
            } else {
                FantasticKits.LOGGER.warn("[{}] LuckPerms API returned null. Group features disabled.",
                        Reference.MOD_NAME);
            }
        } catch (NoClassDefFoundError | IllegalStateException | Throwable t) {
            this.available = false;
            this.api = null;
            FantasticKits.LOGGER.warn("[{}] LuckPerms not installed. Group-based features will be disabled. " +
                    "Install LuckPerms-Forge to unlock VIP/Prime/Elite/etc. kits.", Reference.MOD_NAME);
            if (FantasticKits.config() != null && FantasticKits.config().luckPermsRequired) {
                FantasticKits.LOGGER.error("[{}] luckPermsRequired=true. Refusing to provide kit features.",
                        Reference.MOD_NAME);
            }
        }
    }

    // ----------------------------------------------------------------------
    // Read-only queries
    // ----------------------------------------------------------------------

    /**
     * @return all loaded LuckPerms groups, as a stable, sorted list. Empty if
     * LuckPerms is not installed.
     */
    public List<GroupInfo> listGroups() {
        if (!available) return Collections.emptyList();
        try {
            List<GroupInfo> groups = new ArrayList<>();
            for (Group g : api.getGroupManager().getLoadedGroups()) {
                int weight = g.getWeight().orElse(0);
                String display = Optional.ofNullable(g.getDisplayName()).orElse(g.getName());
                List<String> inheritance = g.getNodes().stream()
                        .filter(InheritanceNode.class::isInstance)
                        .map(InheritanceNode.class::cast)
                        .map(InheritanceNode::getGroupName)
                        .collect(Collectors.toList());
                List<String> perms = g.getNodes().stream()
                        .filter(PermissionNode.class::isInstance)
                        .map(PermissionNode.class::cast)
                        .map(PermissionNode::getPermission)
                        .collect(Collectors.toList());
                groups.add(new GroupInfo(g.getName(), display, weight, inheritance, perms));
            }
            groups.sort((a, b) -> Integer.compare(b.weight(), a.weight()));
            return groups;
        } catch (Throwable t) {
            FantasticKits.LOGGER.error("Failed to list LuckPerms groups", t);
            return Collections.emptyList();
        }
    }

    /**
     * @return the player's primary LuckPerms group name, or the configured default
     * group if LuckPerms is not available.
     */
    public String primaryGroup(UUID playerId) {
        if (!available) {
            return FantasticKits.config().defaultGroupName;
        }
        try {
            User user = api.getUserManager().getUser(playerId);
            if (user == null) {
                return FantasticKits.config().defaultGroupName;
            }
            String pg = user.getPrimaryGroup();
            return (pg == null || pg.isBlank()) ? FantasticKits.config().defaultGroupName : pg;
        } catch (Throwable t) {
            return FantasticKits.config().defaultGroupName;
        }
    }

    /**
     * The single source of truth for "can this player claim/use this kit".
     * <p>
     * Compares the player's primary group string-equality against the kit's
     * owning group. Inheritance, weights and permission nodes are ignored on
     * purpose: this is the contract requested by the spec
     * ({@code strictGroupMatching=true}).
     */
    public boolean isPrimaryGroupExactly(UUID playerId, String requiredGroup) {
        if (requiredGroup == null || requiredGroup.isBlank()) return false;
        if (FantasticKits.config().strictGroupMatching) {
            String pg = primaryGroup(playerId);
            return pg != null && pg.equalsIgnoreCase(requiredGroup);
        }
        // Strict matching is the default and recommended mode. The relaxed
        // branch still requires a primary group match - we deliberately never
        // fall back to inheritance.
        String pg = primaryGroup(playerId);
        return pg != null && pg.equalsIgnoreCase(requiredGroup);
    }

    // ----------------------------------------------------------------------
    // Group <-> kit synchronisation (write side)
    // ----------------------------------------------------------------------

    /**
     * Publishes the kit and its commands to the given LuckPerms group. The
     * structure is:
     * <pre>
     *   fantastickits.kit.&lt;kitId&gt;
     *   fantastickits.cmd.&lt;kitId&gt;.&lt;command&gt;
     * </pre>
     * Existing nodes for the same kit are wiped first so command lists stay in
     * sync after edits.
     */
    public void syncKitToGroup(String groupName, String kitId, List<String> commands) {
        if (!available || groupName == null || groupName.isBlank()) return;
        try {
            Group group = api.getGroupManager().getGroup(groupName);
            if (group == null) {
                FantasticKits.LOGGER.warn("Cannot sync kit '{}' to unknown group '{}'.", kitId, groupName);
                return;
            }
            String kitNode = NODE_KIT + sanitize(kitId);
            String cmdPrefix = NODE_KIT_CMD + sanitize(kitId) + ".";

            // Remove old kit/cmd nodes
            group.data().clear(node -> {
                String key = node.getKey();
                return key.equals(kitNode) || key.startsWith(cmdPrefix);
            });

            // Add fresh nodes
            DataMutateResult r1 = group.data().add(Node.builder(kitNode).value(true).build());
            for (String cmd : commands) {
                DataMutateResult r2 = group.data().add(Node.builder(cmdPrefix + sanitize(cmd))
                        .value(true).build());
                if (!r2.wasSuccessful() && r2 != DataMutateResult.FAIL_ALREADY_HAS) {
                    FantasticKits.LOGGER.warn("Failed to publish cmd node {} -> {}", cmd, r2);
                }
            }

            api.getGroupManager().saveGroup(group);
        } catch (Throwable t) {
            FantasticKits.LOGGER.error("LuckPerms sync failed for kit '{}'", kitId, t);
        }
    }

    /**
     * Drops every node generated by the mod for the given kit across all groups.
     * Used by {@code /fkits delete}.
     */
    public void revokeKit(String kitId) {
        if (!available) return;
        try {
            String kitNode = NODE_KIT + sanitize(kitId);
            String cmdPrefix = NODE_KIT_CMD + sanitize(kitId) + ".";
            for (Group group : api.getGroupManager().getLoadedGroups()) {
                int removed = group.data().clear(node -> {
                    String key = node.getKey();
                    return key.equals(kitNode) || key.startsWith(cmdPrefix);
                });
                if (removed > 0) {
                    api.getGroupManager().saveGroup(group);
                }
            }
        } catch (Throwable t) {
            FantasticKits.LOGGER.error("LuckPerms revoke failed for kit '{}'", kitId, t);
        }
    }

    private static String sanitize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
    }
}
