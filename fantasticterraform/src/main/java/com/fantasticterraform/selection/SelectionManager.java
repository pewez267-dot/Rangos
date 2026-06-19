package com.fantasticterraform.selection;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro server-side de las selecciones activas, indexado por UUID de jugador.
 * La seleccion es un estado persistente del jugador (no se borra al alejarse).
 */
public final class SelectionManager {

    private static final Map<UUID, PlayerSelection> SELECTIONS = new ConcurrentHashMap<>();

    private SelectionManager() {
    }

    public static PlayerSelection get(ServerPlayer player) {
        return get(player.getUUID());
    }

    public static PlayerSelection get(UUID id) {
        return SELECTIONS.computeIfAbsent(id, k -> new PlayerSelection());
    }

    public static PlayerSelection getIfPresent(UUID id) {
        return SELECTIONS.get(id);
    }

    public static void clear(ServerPlayer player) {
        PlayerSelection sel = SELECTIONS.get(player.getUUID());
        if (sel != null) {
            sel.clear();
        }
    }

    /** Elimina por completo el estado (al salir del modo editor). */
    public static void remove(ServerPlayer player) {
        SELECTIONS.remove(player.getUUID());
    }
}
