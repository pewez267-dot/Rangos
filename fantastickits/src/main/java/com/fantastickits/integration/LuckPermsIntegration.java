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
     * Registers the given commands as permission nodes on a LuckPerms group, replacing any
     * previously-registered {@code fantastickits.command.*} nodes on that group. This is what
     * makes LuckPerms the source of truth: only that group (and its inheritors) will hold the
     * nodes, so only that rank passes the command guard. Runs asynchronously on LuckPerms'
     * own executor; failures are logged and never crash the server.
     */
    public static void syncGroupCommandNodes(final String group, final Collection<String> commands) {
        if (group == null || group.isBlank() || !isLoaded()) {
            return;
        }
        try {
            Hooks.syncGroupCommandNodes(group, commands);
        } catch (final Throwable t) {
            FantasticKits.LOGGER.warn("[FantasticKits] No se pudieron sincronizar permisos en LuckPerms para '{}': {}", group, t.toString());
        }
    }

    /** Removes every {@code fantastickits.command.*} node from a group (e.g. on kit deletion). */
    public static void clearGroupCommandNodes(final String group) {
        syncGroupCommandNodes(group, Collections.emptyList());
    }

    /**
     * Whether {@code uuid} holds the LuckPerms permission node for {@code command} (resolving
     * inheritance and contexts). This is the authoritative allow-check used by the command guard.
     */
    public static boolean hasCommandPermission(final UUID uuid, final String command) {
        if (uuid == null || command == null || command.isBlank() || !isLoaded()) {
            return false;
        }
        try {
            return Hooks.hasCommandPermission(uuid, command);
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

        static void syncGroupCommandNodes(final String group, final Collection<String> commands) {
            final net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            api.getGroupManager().modifyGroup(group, g -> {
                // Remove our previously-registered command nodes (namespace-scoped: never touches
                // any other permission the admin manages on the group).
                final List<net.luckperms.api.node.Node> toRemove = new java.util.ArrayList<>();
                for (final net.luckperms.api.node.Node node : g.data().toCollection()) {
                    if (node.getKey().startsWith(COMMAND_NODE_PREFIX)) {
                        toRemove.add(node);
                    }
                }
                for (final net.luckperms.api.node.Node node : toRemove) {
                    g.data().remove(node);
                }
                // Add the current set.
                if (commands != null) {
                    for (final String command : commands) {
                        if (command != null && !command.isBlank()) {
                            g.data().add(net.luckperms.api.node.types.PermissionNode.builder(
                                    COMMAND_NODE_PREFIX + command).build());
                        }
                    }
                }
            }).exceptionally(t -> {
                FantasticKits.LOGGER.warn("[FantasticKits] Error guardando permisos del grupo '{}' en LuckPerms: {}", group, t.toString());
                return null;
            });
        }

        static boolean hasCommandPermission(final UUID uuid, final String command) {
            final net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            final net.luckperms.api.model.user.User user = api.getUserManager().getUser(uuid);
            if (user == null) {
                return false;
            }
            final net.luckperms.api.query.QueryOptions options = api.getContextManager().getQueryOptions(user)
                    .orElseGet(net.luckperms.api.query.QueryOptions::nonContextual);
            return user.getCachedData().getPermissionData(options)
                    .checkPermission(COMMAND_NODE_PREFIX + command).asBoolean();
        }
    }
}
