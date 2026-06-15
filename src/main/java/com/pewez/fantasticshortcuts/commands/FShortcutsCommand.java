package com.pewez.fantasticshortcuts.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pewez.fantasticshortcuts.network.FSShortcutsNetwork;
import com.pewez.fantasticshortcuts.network.OpenEditorPacket;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import com.pewez.fantasticshortcuts.util.ChatPrefix;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

/**
 * The management command: {@code /fshortcuts}.
 *
 * Following the FantasticCrates/FantasticSpawners pattern:
 *   /fshortcuts        -> opens the editor GUI (Lista tab)
 *   /fshortcuts create -> opens the editor GUI (Crear tab)
 *   /fshortcuts reload -> reloads shortcuts from disk (only text command)
 *
 * All CRUD (create, edit, delete) is done exclusively through the GUI.
 */
public final class FShortcutsCommand {

    private FShortcutsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fshortcuts")
                .requires(source -> source.hasPermission(4))
                .executes(context -> openEditor(context, "lista"))
                .then(Commands.literal("create")
                        .executes(context -> openEditor(context, "crear")))
                .then(Commands.literal("reload")
                        .executes(FShortcutsCommand::reload)));
    }

    private static int openEditor(CommandContext<CommandSourceStack> context, String tab) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        FSShortcutsNetwork.sendToClient(player,
                new OpenEditorPacket(new ArrayList<>(ShortcutManager.get().all()), tab));
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        ShortcutManager.get().reload();
        com.pewez.fantasticshortcuts.FantasticShortcutsMod.liveSync(context.getSource().getServer());
        context.getSource().sendSuccess(() -> ChatPrefix.success(
                "Reloaded. " + ShortcutManager.get().all().size() + " shortcuts loaded."), true);
        return 1;
    }
}
