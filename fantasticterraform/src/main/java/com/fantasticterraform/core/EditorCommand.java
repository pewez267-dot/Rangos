package com.fantasticterraform.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando raiz {@code /fsterraform} con exactamente dos literales hijos: {@code editor}
 * y {@code exit}. Cada uno exige {@code hasPermission(4)} (operador) por su cuenta.
 * No existe ningun subcomando adicional: toda la edicion se controla desde el HUD.
 */
public final class EditorCommand {

    private EditorCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fsterraform")
                .then(Commands.literal("editor")
                        .requires(source -> source.hasPermission(4))
                        .executes(EditorCommand::enterEditor))
                .then(Commands.literal("exit")
                        .requires(source -> source.hasPermission(4))
                        .executes(EditorCommand::exitEditor)));
    }

    private static int enterEditor(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        // Doble validacion server-side del nivel de permisos OP.
        if (!ctx.getSource().hasPermission(4)) {
            player.sendSystemMessage(Component.literal("\u00a7cSolo operadores (nivel 4) pueden usar Fantastic Terraform."));
            return 0;
        }
        return EditorModeManager.get().enter(player) ? 1 : 0;
    }

    private static int exitEditor(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        if (!ctx.getSource().hasPermission(4)) {
            player.sendSystemMessage(Component.literal("\u00a7cSolo operadores (nivel 4) pueden usar Fantastic Terraform."));
            return 0;
        }
        return EditorModeManager.get().exit(player) ? 1 : 0;
    }

    private static ServerPlayer playerOrNull(CommandContext<CommandSourceStack> ctx) {
        try {
            return ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(Component.literal("Este comando debe ejecutarlo un jugador."));
            return null;
        }
    }
}
