package com.fantastickits.integration;

import com.fantastickits.FantasticKits;
import net.minecraftforge.fml.ModList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Soft integration with LuckPerms.
 *
 * <p>This <em>facade</em> deliberately references no {@code net.luckperms.*} type, so
 * the class can always be loaded even when LuckPerms is absent. Every real API call is
 * confined to the nested {@link Hooks} class, which is only ever touched after
 * {@link ModList#isLoaded(String)} confirms LuckPerms is present. Any linkage or
 * runtime failure is caught and degrades to the "absent" behaviour.</p>
 *
 * <p>Group data is therefore read live from LuckPerms every time it is requested — the
 * mod never caches or hard-codes group names.</p>
 */
public final class LuckPermsIntegration {

    public static final String MOD_ID = "luckperms";

    private LuckPermsIntegration() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /** True only if LuckPerms is installed AND its API is currently available. */
    public static boolean isAvailable() {
        if (!isLoaded()) {
            return false;
        }
        try {
            return Hooks.ping();
        } catch (final Throwable t) {
            return false;
        }
    }

    /** All currently-loaded LuckPerms group names, sorted; empty when unavailable. */
    public static List<String> groupNames() {
        if (!isLoaded()) {
            return Collections.emptyList();
        }
        try {
            return Hooks.groupNames();
        } catch (final Throwable t) {
            FantasticKits.LOGGER.warn("[FantasticKits] No se pudo leer los grupos de LuckPerms: {}", t.toString());
            return Collections.emptyList();
        }
    }

    /**
     * Whether {@code uuid} belongs to {@code group}, resolving LuckPerms inheritance
     * (a member of a group that inherits {@code group} is considered a member). Returns
     * {@code false} when LuckPerms is unavailable or the user is not loaded.
     */
    public static boolean isMemberOf(final UUID uuid, final String group) {
        if (uuid == null || group == null || group.isBlank() || !isLoaded()) {
            return false;
        }
        try {
            return Hooks.isMemberOf(uuid, group);
        } catch (final Throwable t) {
            FantasticKits.LOGGER.warn("[FantasticKits] Fallo comprobando pertenencia a grupo '{}': {}", group, t.toString());
            return false;
        }
    }

    /**
     * Permission node namespace used to register a kit's commands onto its LuckPerms group.
     * Example: assigning {@code fly} to the group {@code vip} writes the node
     * {@code fantastickits.command.fly} to that group, so only its members (and groups that
     * inherit it) hold the node — which is exactly what {@code CommandGuard} checks.
     */
    public static final String COMMAND_NODE_PREFIX = "fantastickits.command.";

    /** @return the LuckPerms permission node that gates the given (normalized) command. */
    public static String commandNode(final String command) {
        return COMMAND_NODE_PREFIX + command;
    }

    /**
     * Registers a kit's commands as permission nodes on a LuckPerms group. For every command it
     * grants the internal node {@code fantastickits.command.<cmd>} plus, for each configured
     * prefix, the real node the target mod checks (e.g. {@code command.he.<cmd>} for
     * HennyEssentials). Previously-managed nodes (internal namespace + simple-label prefix nodes)
     * are replaced, so only that group (and its inheritors) end up holding them. Runs async on
     * LuckPerms' executor; failures are logged and never crash the server.
     */
    public static void syncGroupCommandNodes(final String group, final Collection<String> commands,
                                             final Collection<String> prefixes) {
        if (group == null || group.isBlank() || !isLoaded()) {
            return;
        }
        try {
            Hooks.syncGroupCommandNodes(group, commands, prefixes);
        } catch (final Throwable t) {
            FantasticKits.LOGGER.warn("[FantasticKits] No se pudieron sincronizar permisos en LuckPerms para '{}': {}", group, t.toString());
        }
    }

    /** Removes every managed command node (internal + given prefixes) from a group. */
    public static void clearGroupCommandNodes(final String group, final Collection<String> prefixes) {
        syncGroupCommandNodes(group, Collections.emptyList(), prefixes);
    }

    /**
     * Whether {@code uuid} may use {@code command}: true if they hold the internal node
     * {@code fantastickits.command.<cmd>} OR any configured-prefix node ({@code <prefix><cmd>}),
     * resolving LuckPerms inheritance/contexts. This avoids conflicts with the target mod's own
     * permission: anyone the mod would allow is allowed here too.
     */
    public static boolean hasCommandPermission(final UUID uuid, final String command,
                                               final Collection<String> prefixes) {
        if (uuid == null || command == null || command.isBlank() || !isLoaded()) {
            return false;
        }
        try {
            return Hooks.hasCommandPermission(uuid, command, prefixes);
        } catch (final Throwable t) {
            FantasticKits.LOGGER.warn("[FantasticKits] Fallo comprobando permiso de comando '{}': {}", command, t.toString());
            return false;
        }
    }

    /**
     * Isolated holder for the actual LuckPerms API usage. Loaded lazily and only from
     * guarded call sites, so {@code net.luckperms.*} is never resolved when the mod is
     * not installed.
     */
    private static final class Hooks {

        static boolean ping() {
            net.luckperms.api.LuckPermsProvider.get();
            return true;
        }

        static List<String> groupNames() {
            final net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            return api.getGroupManager().getLoadedGroups().stream()
                    .map(net.luckperms.api.model.group.Group::getName)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }

        static boolean isMemberOf(final UUID uuid, final String group) {
            final net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            final net.luckperms.api.model.user.User user = api.getUserManager().getUser(uuid);
            if (user == null) {
                // Not loaded in memory (e.g. fully offline). Online players are always loaded.
                return false;
            }
            if (user.getPrimaryGroup() != null && user.getPrimaryGroup().equalsIgnoreCase(group)) {
                return true;
            }
            final net.luckperms.api.query.QueryOptions options = user.getQueryOptions();
            for (final net.luckperms.api.model.group.Group inherited : user.getInheritedGroups(options)) {
                if (inherited.getName().equalsIgnoreCase(group)) {
                    return true;
                }
            }
            return false;
        }

        static void syncGroupCommandNodes(final String group, final Collection<String> commands,
                                          final Collection<String> prefixes) {
            final net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            api.getGroupManager().modifyGroup(group, g -> {
                // Remove only the nodes we manage: our internal namespace, plus simple-label nodes
                // under the configured prefixes (e.g. command.he.heal). Wildcards / dotted nodes
                // such as command.he.* or command.he.condition.* are left untouched.
                final List<net.luckperms.api.node.Node> toRemove = new java.util.ArrayList<>();
                for (final net.luckperms.api.node.Node node : g.data().toCollection()) {
                    if (isManagedNode(node.getKey(), prefixes)) {
                        toRemove.add(node);
                    }
                }
                for (final net.luckperms.api.node.Node node : toRemove) {
                    g.data().remove(node);
                }
                // Add the internal node plus the real node for each configured prefix.
                if (commands != null) {
                    for (final String command : commands) {
                        if (command == null || command.isBlank()) {
                            continue;
                        }
                        g.data().add(net.luckperms.api.node.types.PermissionNode.builder(
                                COMMAND_NODE_PREFIX + command).build());
                        if (prefixes != null) {
                            for (final String prefix : prefixes) {
                                if (prefix != null && !prefix.isBlank()) {
                                    g.data().add(net.luckperms.api.node.types.PermissionNode.builder(
                                            prefix + command).build());
                                }
                            }
                        }
                    }
                }
            }).exceptionally(t -> {
                FantasticKits.LOGGER.warn("[FantasticKits] Error guardando permisos del grupo '{}' en LuckPerms: {}", group, t.toString());
                return null;
            });
        }

        private static boolean isManagedNode(final String key, final Collection<String> prefixes) {
            if (key.startsWith(COMMAND_NODE_PREFIX)) {
                return true;
            }
            if (prefixes != null) {
                for (final String prefix : prefixes) {
                    if (prefix != null && !prefix.isBlank() && key.startsWith(prefix)
                            && isSimpleLabel(key.substring(prefix.length()))) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean isSimpleLabel(final String s) {
            if (s.isEmpty()) {
                return false;
            }
            for (int i = 0; i < s.length(); i++) {
                final char c = s.charAt(i);
                if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')) {
                    return false;
                }
            }
            return true;
        }

        static boolean hasCommandPermission(final UUID uuid, final String command, final Collection<String> prefixes) {
            final net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            final net.luckperms.api.model.user.User user = api.getUserManager().getUser(uuid);
            if (user == null) {
                return false;
            }
            final net.luckperms.api.query.QueryOptions options = api.getContextManager().getQueryOptions(user)
                    .orElseGet(net.luckperms.api.query.QueryOptions::nonContextual);
            final net.luckperms.api.cacheddata.CachedPermissionData data = user.getCachedData().getPermissionData(options);
            if (data.checkPermission(COMMAND_NODE_PREFIX + command).asBoolean()) {
                return true;
            }
            if (prefixes != null) {
                for (final String prefix : prefixes) {
                    if (prefix != null && !prefix.isBlank()
                            && data.checkPermission(prefix + command).asBoolean()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
