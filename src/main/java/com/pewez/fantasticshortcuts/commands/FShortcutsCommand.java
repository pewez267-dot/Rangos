package com.pewez.fantasticshortcuts.commands;

import com.pewez.fantasticshortcuts.brigadier.CommandTreeService;
import com.pewez.fantasticshortcuts.gui.GuiTab;
import com.pewez.fantasticshortcuts.network.OpenEditorPacket;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando base {@code /fshortcuts}.
 *
 * <p>Toda la gestión (crear, editar, eliminar) ocurre DENTRO de la GUI: no hay subcomandos de chat
 * para el CRUD. Estos comandos solo abren la pantalla profesional enviando un
 * {@link OpenEditorPacket} al jugador.
 *
 * <ul>
 *     <li>{@code /fshortcuts} -> abre el editor en la pestaña "Lista".</li>
 *     <li>{@code /fshortcuts create} -> abre el editor en la pestaña "Crear".</li>
 *     <li>{@code /fshortcuts reload} -> recarga shortcuts.json desde disco y resincroniza.</li>
 * </ul>
 *
 * <p>Además, este método registra TODOS los atajos del usuario como nodos de comando
 * ({@link CommandTreeService#registerAll}).
 */
public final class FShortcutsCommand {

    private FShortcutsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 1) Comando base de gestión (abre la GUI). Protegido con permiso 4.
        dispatcher.register(Commands.literal("fshortcuts")
                .requires(s -> s.hasPermission(4))
                .executes(ctx -> open(ctx, GuiTab.LIST))
                .then(Commands.literal("create").executes(ctx -> open(ctx, GuiTab.CREATE)))
                .then(Commands.literal("list").executes(ctx -> open(ctx, GuiTab.LIST)))
                .then(Commands.literal("settings").executes(ctx -> open(ctx, GuiTab.SETTINGS)))
                .then(Commands.literal("reload").executes(FShortcutsCommand::reload)));

        // 2) Registro de todos los atajos del usuario como nodos del árbol de comandos.
        CommandTreeService.registerAll(dispatcher);
    }

    private static int open(CommandContext<CommandSourceStack> ctx, GuiTab tab) {
        final ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        OpenEditorPacket.open(player, tab.ordinal());
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        ShortcutManager.get().reload();
        if (ShortcutManager.get().server() != null) {
            CommandTreeService.rebuildAndSync(ShortcutManager.get().server());
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§7[§bF-Shortcuts§7] §aRecargados " + ShortcutManager.get().size() + " atajo(s)."), true);
        return 1;
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> ctx) {
        try {
            return ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§c[F-Shortcuts] Este comando debe ejecutarlo un jugador."));
            return null;
        }
    }
}
