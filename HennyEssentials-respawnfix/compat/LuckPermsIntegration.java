package com.henny.hennyessentials.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

/**
 * Soft integration with LuckPerms.
 *
 * <p>IMPORTANT — respawn race condition fix:
 * When a player presses "respawn", the server recreates the {@link net.minecraft.server.level.ServerPlayer}
 * entity and LuckPerms re-attaches its user capability to that brand new entity. There is a very short
 * window in which the capability / user data is not ready yet. If we query LuckPerms during that window,
 * LuckPerms throws {@code IllegalStateException: Capability missing for <uuid>} (and {@code getQueryOptions}
 * returns an empty {@link java.util.Optional}). Previously that exception escaped HennyEssentials and broke
 * the respawn flow, leaving the player frozen in the air.</p>
 *
 * <p>To guarantee this can never happen again, every public method here is fully defensive:
 * <ul>
 *   <li>it first checks that the LuckPerms provider is available and the user is actually loaded,</li>
 *   <li>it never calls {@code orElseThrow()} on query options — it falls back to the static query options,</li>
 *   <li>and it wraps the whole body in a try/catch so any transient error returns a safe default
 *       instead of propagating and interrupting gameplay (respawn, chat, commands, etc.).</li>
 * </ul>
 * The correct value is still returned the moment LuckPerms has finished loading the player, so the only
 * visible effect of the race window is that permissions/prefix briefly resolve to their defaults.</p>
 */
public class LuckPermsIntegration {
    public static boolean isLuckPermsLoaded = false;

    /** Safely obtain the LuckPerms API instance, or {@code null} if it is not ready. */
    private static LuckPerms provider() {
        if (!isLuckPermsLoaded) {
            return null;
        }
        try {
            return LuckPermsProvider.get();
        } catch (Throwable t) {
            // LuckPerms not registered yet.
            return null;
        }
    }

    /**
     * Returns the user only if LuckPerms has fully loaded it. Returns {@code null} during the
     * post-respawn / pre-login window so callers fall back to safe defaults instead of crashing.
     */
    private static User loadedUser(LuckPerms luckPerms, UUID uuid) {
        if (luckPerms == null || uuid == null) {
            return null;
        }
        try {
            if (!luckPerms.getUserManager().isLoaded(uuid)) {
                return null;
            }
            return luckPerms.getUserManager().getUser(uuid);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Resolves query options for a user without ever throwing. If the player-dependent context is not
     * ready yet (the "Capability missing" window), this falls back to the static query options so we
     * still get a usable, non-crashing result.
     */
    private static QueryOptions safeQueryOptions(LuckPerms luckPerms, User user) {
        try {
            return luckPerms.getContextManager().getQueryOptions(user)
                    .orElseGet(() -> luckPerms.getContextManager().getStaticQueryOptions());
        } catch (Throwable t) {
            try {
                return luckPerms.getContextManager().getStaticQueryOptions();
            } catch (Throwable inner) {
                return QueryOptions.nonContextual();
            }
        }
    }

    public static boolean checkPermission(UUID uuid, String perm) {
        try {
            LuckPerms luckPerms = provider();
            User user = loadedUser(luckPerms, uuid);
            if (user == null) {
                return false;
            }
            CachedPermissionData data = user.getCachedData().getPermissionData(safeQueryOptions(luckPerms, user));
            return data.checkPermission(perm).asBoolean();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean checkPermission(UUID uuid, List<String> perms) {
        try {
            LuckPerms luckPerms = provider();
            User user = loadedUser(luckPerms, uuid);
            if (user == null) {
                return false;
            }
            CachedPermissionData data = user.getCachedData().getPermissionData(safeQueryOptions(luckPerms, user));
            for (String perm : perms) {
                if (data.checkPermission(perm).asBoolean()) {
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String getPrefix(UUID uuid) {
        try {
            LuckPerms luckPerms = provider();
            User user = loadedUser(luckPerms, uuid);
            if (user == null) {
                return "";
            }
            CachedMetaData metaData = user.getCachedData().getMetaData(safeQueryOptions(luckPerms, user));
            return metaData.getPrefix() != null ? metaData.getPrefix() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static String getSuffix(UUID uuid) {
        try {
            LuckPerms luckPerms = provider();
            User user = loadedUser(luckPerms, uuid);
            if (user == null) {
                return "";
            }
            CachedMetaData metaData = user.getCachedData().getMetaData(safeQueryOptions(luckPerms, user));
            return metaData.getSuffix() != null ? metaData.getSuffix() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static int getMaxHomeLimit(UUID uuid) {
        try {
            LuckPerms luckPerms = provider();
            User user = loadedUser(luckPerms, uuid);
            if (user == null) {
                return 0;
            }
            CachedPermissionData data = user.getCachedData().getPermissionData(safeQueryOptions(luckPerms, user));
            return data.getPermissionMap().entrySet().stream().filter(entry -> {
                return ((Boolean) entry.getValue()).booleanValue();
            }).map((v0) -> {
                return v0.getKey();
            }).filter(perm -> {
                return perm.startsWith("home.limit.");
            }).map(perm2 -> {
                return perm2.substring("home.limit.".length());
            }).mapToInt(limitStr -> {
                try {
                    return Integer.parseInt(limitStr);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }).filter(num -> {
                return num >= 0;
            }).max().orElse(0);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static List<String> getUserPermissions(UUID uuid) {
        try {
            LuckPerms luckPerms = provider();
            User user = loadedUser(luckPerms, uuid);
            if (user == null) {
                return new ArrayList<>();
            }
            CachedPermissionData data = user.getCachedData().getPermissionData(safeQueryOptions(luckPerms, user));
            return (List) data.getPermissionMap().entrySet().stream().filter((v0) -> {
                return v0.getValue();
            }).map((v0) -> {
                return v0.getKey();
            }).collect(Collectors.toList());
        } catch (Throwable t) {
            return new ArrayList<>();
        }
    }

    public static boolean hasPermission(UUID playerUUID, String permission) {
        try {
            if (isLuckPermsLoaded) {
                return checkPermission(playerUUID, permission);
            }
            return false;
        } catch (Throwable e) {
            return false;
        }
    }
}
