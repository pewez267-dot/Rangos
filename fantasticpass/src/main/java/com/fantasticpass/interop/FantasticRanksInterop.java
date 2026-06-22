package com.fantasticpass.interop;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Optional, reflection-only bridge to the "fantasticranks" mod. Every access is wrapped
 * so a missing mod or a different API shape can never crash Fantastic Pass.
 *
 * <p>Because the concrete Fantastic Ranks API is not a compile-time dependency, this
 * class probes a small set of conventional entry points and returns {@code null} when
 * none are available. The returned string, when present, is expected to contain legacy
 * {@code §} formatting codes and is rendered as-is.
 */
public final class FantasticRanksInterop {

    private static Boolean loaded;
    private static boolean resolved;
    private static Method apiMethodByUuid;
    private static Method apiMethodByPlayer;
    private static Object apiInstance;

    private FantasticRanksInterop() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            try {
                loaded = ModList.get() != null && ModList.get().isLoaded("fantasticranks");
            } catch (Throwable t) {
                loaded = Boolean.FALSE;
            }
        }
        return loaded;
    }

    /**
     * @return a legacy-formatted rank string for the player, or {@code null} if Fantastic
     *         Ranks is not installed, exposes no compatible API, or has no rank for them.
     */
    @Nullable
    public static String getFormattedRank(Player player) {
        if (player == null || !isLoaded()) {
            return null;
        }
        resolveApi();
        try {
            if (apiMethodByPlayer != null) {
                Object result = apiMethodByPlayer.invoke(apiInstance, player);
                return asString(result);
            }
            if (apiMethodByUuid != null) {
                Object result = apiMethodByUuid.invoke(apiInstance, player.getUUID());
                return asString(result);
            }
        } catch (Throwable ignored) {
            // Any failure is non-fatal; treat as "no rank available".
        }
        return null;
    }

    private static void resolveApi() {
        if (resolved) {
            return;
        }
        resolved = true;
        // Probe a few conventional API class names without hard-linking to them.
        String[] candidateClasses = {
                "com.fantasticranks.api.FantasticRanksAPI",
                "com.fantasticranks.FantasticRanksAPI",
                "net.fantasticranks.api.FantasticRanksAPI"
        };
        for (String className : candidateClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                apiInstance = resolveInstance(clazz);
                apiMethodByPlayer = findMethod(clazz, Player.class,
                        "getFormattedRank", "getDisplayRank", "getRankPrefix", "getRankString");
                apiMethodByUuid = findMethod(clazz, UUID.class,
                        "getFormattedRank", "getDisplayRank", "getRankPrefix", "getRankString");
                if (apiMethodByPlayer != null || apiMethodByUuid != null) {
                    return;
                }
            } catch (Throwable ignored) {
                // Try the next candidate.
            }
        }
    }

    @Nullable
    private static Object resolveInstance(Class<?> clazz) {
        // Static-only APIs do not need an instance.
        for (String getter : new String[]{"getInstance", "instance", "get"}) {
            try {
                Method m = clazz.getMethod(getter);
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    return m.invoke(null);
                }
            } catch (Throwable ignored) {
                // continue
            }
        }
        return null;
    }

    @Nullable
    private static Method findMethod(Class<?> clazz, Class<?> paramType, String... names) {
        for (String name : names) {
            try {
                Method m = clazz.getMethod(name, paramType);
                m.setAccessible(true);
                return m;
            } catch (Throwable ignored) {
                // continue
            }
        }
        return null;
    }

    @Nullable
    private static String asString(@Nullable Object result) {
        if (result == null) {
            return null;
        }
        String s = result.toString();
        return s.isEmpty() ? null : s;
    }
}
