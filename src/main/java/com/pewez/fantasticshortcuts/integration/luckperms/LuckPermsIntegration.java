package com.pewez.fantasticshortcuts.integration.luckperms;

import com.pewez.fantasticshortcuts.FantasticShortcuts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Integración OPCIONAL con LuckPerms en entorno modded.
 *
 * <p>Se usa exclusivamente en modo <b>solo lectura</b>: detecta si LuckPerms está cargado como mod
 * y, en tal caso, lee el grupo primario y los grupos del jugador a través de su API oficial.
 *
 * <p>Para no introducir una dependencia de compilación obligatoria (el mod debe funcionar con o sin
 * LuckPerms), la API se invoca mediante reflexión sobre {@code net.luckperms.api.LuckPermsProvider}.
 *
 * <p>REGLA DE ORO: este mod jamás modifica, crea ni concede permisos. Aquí solo se LEE información
 * para auditoría y para resolver prioridades, nunca para autorizar.
 */
public final class LuckPermsIntegration {

    private static Boolean cachedPresent = null;

    private LuckPermsIntegration() {}

    /** {@code true} si LuckPerms está instalado como mod. */
    public static boolean isPresent() {
        if (cachedPresent == null) {
            cachedPresent = ModList.get() != null && ModList.get().isLoaded("luckperms");
        }
        return cachedPresent;
    }

    /**
     * Devuelve el grupo primario del jugador según LuckPerms, o {@code null} si LuckPerms no está
     * presente o el usuario no está cargado.
     */
    public static String primaryGroup(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        return primaryGroup(player.getUUID());
    }

    public static String primaryGroup(UUID uuid) {
        if (!isPresent() || uuid == null) {
            return null;
        }
        try {
            final Object user = loadUser(uuid);
            if (user == null) {
                return null;
            }
            return (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);
        } catch (Throwable t) {
            FantasticShortcuts.LOGGER.debug("[F-Shortcuts] LuckPerms primaryGroup no disponible: {}", t.toString());
            return null;
        }
    }

    /**
     * Devuelve los nombres de grupos del jugador (solo lectura). Lista vacía si no hay datos.
     */
    @SuppressWarnings("unchecked")
    public static List<String> groups(UUID uuid) {
        if (!isPresent() || uuid == null) {
            return Collections.emptyList();
        }
        try {
            final Object user = loadUser(uuid);
            if (user == null) {
                return Collections.emptyList();
            }
            // user.getInheritedGroups(user.getQueryOptions()) -> Collection<Group>
            final Object queryOptions = user.getClass().getMethod("getQueryOptions").invoke(user);
            final Object groups = user.getClass()
                    .getMethod("getInheritedGroups", Class.forName("net.luckperms.api.query.QueryOptions"))
                    .invoke(user, queryOptions);
            final java.util.Collection<Object> coll = (java.util.Collection<Object>) groups;
            final java.util.ArrayList<String> names = new java.util.ArrayList<>();
            for (Object g : coll) {
                names.add((String) g.getClass().getMethod("getName").invoke(g));
            }
            return names;
        } catch (Throwable t) {
            FantasticShortcuts.LOGGER.debug("[F-Shortcuts] LuckPerms groups no disponible: {}", t.toString());
            return Collections.emptyList();
        }
    }

    /** Carga (de forma síncrona si hace falta) el usuario de LuckPerms vía API reflejada. */
    private static Object loadUser(UUID uuid) throws Exception {
        final Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
        final Object luckPerms = provider.getMethod("get").invoke(null);
        final Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
        // Primero intentamos el usuario ya cargado en memoria (no bloqueante).
        Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
        if (user != null) {
            return user;
        }
        // Si no está en caché, lo cargamos esperando el CompletableFuture.
        final Object future = userManager.getClass().getMethod("loadUser", UUID.class).invoke(userManager, uuid);
        if (future instanceof java.util.concurrent.CompletableFuture<?> cf) {
            return cf.get();
        }
        return null;
    }
}
