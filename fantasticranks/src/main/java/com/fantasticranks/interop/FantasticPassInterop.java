package com.fantasticranks.interop;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Optional, reflection-only bridge to the "fantasticpass" mod. Every access is wrapped so
 * a missing mod or a different API shape can never crash Fantastic Ranks.
 *
 * <p>Coordination is decided client-side, per frame: Fantastic Ranks yields its nametag
 * slot whenever Fantastic Pass is already rendering a line for that player (i.e. the
 * player has an active Fantastic Pass displayed rank). Because Fantastic Pass only renders
 * a line when it has something to show, checking its client cache is both correct and
 * always up to date, with no packet-timing races. Fantastic Ranks never exposes an API to
 * Fantastic Pass, so Fantastic Pass's own Fantastic-Ranks fallback stays inert and the two
 * mods never draw the same line twice.
 */
public final class FantasticPassInterop {

    private static Boolean loaded;
    private static boolean clientResolved;
    private static Method cacheGetMethod;
    private static Method hasLineMethod;

    private FantasticPassInterop() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            try {
                loaded = ModList.get() != null && ModList.get().isLoaded("fantasticpass");
            } catch (Throwable t) {
                loaded = Boolean.FALSE;
            }
        }
        return loaded;
    }

    /**
     * Client-side. @return {@code true} if Fantastic Pass is currently rendering a nametag
     * line for the given player, meaning Fantastic Ranks should yield the slot.
     */
    public static boolean shouldCedeNametag(UUID playerId) {
        if (playerId == null || !isLoaded()) {
            return false;
        }
        resolveClient();
        if (cacheGetMethod == null || hasLineMethod == null) {
            return false;
        }
        try {
            Object data = cacheGetMethod.invoke(null, playerId);
            if (data == null) {
                return false;
            }
            Object hasLine = hasLineMethod.invoke(data);
            return hasLine instanceof Boolean && (Boolean) hasLine;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void resolveClient() {
        if (clientResolved) {
            return;
        }
        clientResolved = true;
        try {
            Class<?> cacheClass = Class.forName("com.fantasticpass.nametag.ClientNametagCache");
            Class<?> dataClass = Class.forName("com.fantasticpass.nametag.NametagData");
            cacheGetMethod = cacheClass.getMethod("get", UUID.class);
            hasLineMethod = dataClass.getMethod("hasLine");
        } catch (Throwable ignored) {
            cacheGetMethod = null;
            hasLineMethod = null;
        }
    }
}
