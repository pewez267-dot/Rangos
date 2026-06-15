package com.fantastickits.integration;

import com.fantastickits.FantasticKits;
import net.minecraftforge.fml.ModList;

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
    }
}
