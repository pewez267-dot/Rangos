package com.fantasticterraform.core;

import com.fantasticterraform.registry.ModItems;
import com.fantasticterraform.selection.SelectionWand;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Collections;

/**
 * Comando raiz {@code /fsterraform}. Literales hijos: {@code editor} y {@code exit}
 * (entrar/salir del modo editor, que abre el HUD) y {@code wand} (entregar la varita
 * de seleccion para usarla tambien en creativo, donde si es visible en la mano). Cada
 * literal exige {@code hasPermission(4)} (operador). Toda la edicion se controla desde
 * el HUD, no por comandos.
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
                        .executes(EditorCommand::exitEditor))
                .then(Commands.literal("wand")
                        .requires(source -> source.hasPermission(4))
                        .executes(EditorCommand::giveWandSelf)
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> giveWand(ctx, EntityArgument.getPlayers(ctx, "targets"))))));
    }

    private static int enterEditor(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
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

    private static int giveWandSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        return giveWand(ctx, Collections.singletonList(player));
    }

    private static int giveWand(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        if (!ctx.getSource().hasPermission(4)) {
            return 0;
        }
        int count = 0;
        for (ServerPlayer target : targets) {
            ItemStack wand = SelectionWand.tagged(new ItemStack(ModItems.SELECTION_WAND.get()));
            if (!target.getInventory().add(wand)) {
                target.drop(wand, false);
            }
            target.sendSystemMessage(Component.literal(
                    "\u00a7d\u2726 \u00a77Has recibido la \u00a7fVarita de Fantastic Terraform\u00a77. "
                            + "Sostenla y pulsa \u00a7eG\u00a77 para abrir los paneles."));
            count++;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "\u00a7aVarita entregada a " + targets.size() + " jugador(es)."), true);
        return count;
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
