package com.fantasticshortcuts.commands;

import com.fantasticshortcuts.FantasticShortcuts;
import com.fantasticshortcuts.data.Shortcut;
import com.fantasticshortcuts.gui.ModMenus;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The four and only commands of Fantastic Shortcuts, all GUI-driven and gated server-side
 * by {@link FantasticShortcuts#ADMIN_PERMISSION_LEVEL}:
 *
 * <pre>
 *   /fshortcuts          open the management GUI
 *   /fshortcuts create   open the editor for a new shortcut
 *   /fshortcuts edit     open the management GUI (select a shortcut and press Edit)
 *   /fshortcuts delete   open the management GUI (select a shortcut and press Delete)
 * </pre>
 */
public final class ShortcutsCommand {

    private ShortcutsCommand() {
    }

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fshortcuts")
                .requires(source -> source.hasPermission(FantasticShortcuts.ADMIN_PERMISSION_LEVEL))
                .executes(ShortcutsCommand::openMain)
                .then(Commands.literal("create").executes(ShortcutsCommand::openCreate))
                .then(Commands.literal("edit").executes(ShortcutsCommand::openMain))
                .then(Commands.literal("delete").executes(ShortcutsCommand::openMain)));
    }

    private static ServerPlayer requirePlayer(final CommandContext<CommandSourceStack> ctx) {
        try {
            return ctx.getSource().getPlayerOrException();
        } catch (final Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cEste comando debe ejecutarlo un jugador (abre una interfaz)."));
            return null;
        }
    }

    private static int openMain(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        ModMenus.openMain(player);
        return 1;
    }

    private static int openCreate(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        ModMenus.openEditor(player, new Shortcut());
        return 1;
    }
}
