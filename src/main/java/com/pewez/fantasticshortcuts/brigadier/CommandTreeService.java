package com.pewez.fantasticshortcuts.brigadier;

import com.pewez.fantasticshortcuts.FantasticShortcuts;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona el ciclo de vida de los nodos de atajo dentro del árbol de comandos del servidor y la
 * sincronización en vivo con los clientes.
 *
 * <p>Cuando se crea, edita o elimina un atajo desde la GUI, {@link #rebuildAndSync(MinecraftServer)}
 * reconstruye los nodos en el dispatcher activo (eliminando los anteriores mediante
 * {@link BrigadierReflection}) y reenvía el árbol de comandos a todos los jugadores con
 * {@code Commands.sendCommands}, de modo que el TAB y el autocompletado quedan actualizados al
 * instante, sin reinicios ni desincronización.
 */
public final class CommandTreeService {

    /** Aliases que NOSOTROS hemos añadido al árbol (para poder eliminarlos limpiamente). */
    private static final Set<String> OUR_ALIASES = ConcurrentHashMap.newKeySet();

    private CommandTreeService() {}

    /**
     * Registro inicial: se invoca desde {@code RegisterCommandsEvent} con el dispatcher recién
     * construido. Añade todos los atajos cargados.
     */
    public static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        OUR_ALIASES.clear();
        int count = 0;
        for (Shortcut s : ShortcutManager.get().list()) {
            if (ShortcutCommandBuilder.register(dispatcher, s)) {
                OUR_ALIASES.add(s.alias());
                count++;
            }
        }
        // Recalcula qué comandos deben ocultarse del TAB del cliente.
        ShortcutManager.get().recomputeReplaced();
        FantasticShortcuts.LOGGER.info("[F-Shortcuts] Registrados {} atajo(s) en el arbol de comandos.", count);
    }

    /**
     * Reconstrucción en caliente tras un cambio (crear/editar/eliminar). Quita los nodos antiguos,
     * añade los actuales, recalcula los ocultos y reenvía el árbol a todos los jugadores.
     */
    public static void rebuildAndSync(MinecraftServer server) {
        if (server == null) {
            return;
        }
        // Aseguramos ejecución en el hilo del servidor (modificamos estructuras compartidas).
        if (!server.isSameThread()) {
            server.execute(() -> rebuildAndSync(server));
            return;
        }

        final CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();

        // 1) Elimina los nodos que añadimos previamente.
        for (String alias : OUR_ALIASES) {
            BrigadierReflection.removeChild(dispatcher.getRoot(), alias);
        }
        OUR_ALIASES.clear();

        // 2) Vuelve a registrar todos los atajos actuales.
        for (Shortcut s : ShortcutManager.get().list()) {
            if (ShortcutCommandBuilder.register(dispatcher, s)) {
                OUR_ALIASES.add(s.alias());
            }
        }

        // 3) Recalcula los comandos a ocultar del TAB.
        ShortcutManager.get().recomputeReplaced();

        // 4) Reenvía el árbol de comandos a cada jugador (TAB/autocompletado sincronizados).
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }
}
