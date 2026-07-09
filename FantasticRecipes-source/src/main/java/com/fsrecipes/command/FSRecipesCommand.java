package com.fsrecipes.command;

import com.fsrecipes.RecipeBans;
import com.fsrecipes.network.Net;
import com.fsrecipes.network.OpenScreenPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class FSRecipesCommand {

    private FSRecipesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("fsrecipes")
                .requires(src -> src.hasPermission(2))
                // /fsrecipes -> abre la GUI
                .executes(ctx -> openGui(ctx.getSource()))
                // /fsrecipes gui
                .then(Commands.literal("gui").executes(ctx -> openGui(ctx.getSource())))
                // /fsrecipes ban <item>
                .then(Commands.literal("ban")
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(ctx -> setBan(ctx.getSource(),
                                        ItemArgument.getItem(ctx, "item"), true))))
                // /fsrecipes unban <item>
                .then(Commands.literal("unban")
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(ctx -> setBan(ctx.getSource(),
                                        ItemArgument.getItem(ctx, "item"), false))))
                // /fsrecipes hand [ban]  -> banea (o desbanea) el item en la mano
                .then(Commands.literal("hand")
                        .executes(ctx -> setHand(ctx.getSource(), true))
                        .then(Commands.argument("ban", BoolArgumentType.bool())
                                .executes(ctx -> setHand(ctx.getSource(), BoolArgumentType.getBool(ctx, "ban")))))
                // /fsrecipes list
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                // /fsrecipes clear
                .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource()))));
    }

    private static int openGui(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("\u00a7cSolo un jugador puede abrir la GUI."));
            return 0;
        }
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenScreenPacket(new java.util.HashSet<>(RecipeBans.banned())));
        return 1;
    }

    private static int setBan(CommandSourceStack src, ItemInput input, boolean ban) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(input.getItem());
        if (id == null) {
            src.sendFailure(Component.literal("\u00a7cItem invalido."));
            return 0;
        }
        boolean changed = RecipeBans.setBanned(src.getServer(), id, ban);
        String verb = ban ? "\u00a7cbaneado" : "\u00a7adesbaneado";
        if (changed) {
            src.sendSuccess(() -> Component.literal("\u00a76[Recipes] \u00a7f" + id + " " + verb + "\u00a7f."), true);
        } else {
            src.sendSuccess(() -> Component.literal("\u00a77Sin cambios (" + id + " ya estaba asi)."), false);
        }
        return 1;
    }

    private static int setHand(CommandSourceStack src, boolean ban) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("\u00a7cSolo un jugador puede usar 'hand'."));
            return 0;
        }
        if (player.getMainHandItem().isEmpty()) {
            src.sendFailure(Component.literal("\u00a7cNo tienes nada en la mano."));
            return 0;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(player.getMainHandItem().getItem());
        if (id == null) {
            src.sendFailure(Component.literal("\u00a7cItem invalido."));
            return 0;
        }
        RecipeBans.setBanned(src.getServer(), id, ban);
        String verb = ban ? "\u00a7cbaneado" : "\u00a7adesbaneado";
        src.sendSuccess(() -> Component.literal("\u00a76[Recipes] \u00a7f" + id + " " + verb + "\u00a7f."), true);
        return 1;
    }

    private static int list(CommandSourceStack src) {
        List<String> ids = new ArrayList<>();
        RecipeBans.banned().forEach(id -> ids.add(id.toString()));
        java.util.Collections.sort(ids);
        if (ids.isEmpty()) {
            src.sendSuccess(() -> Component.literal("\u00a77No hay recetas baneadas."), false);
            return 1;
        }
        src.sendSuccess(() -> Component.literal("\u00a76[Recipes] \u00a7f" + ids.size() + " baneadas:"), false);
        for (String s : ids) {
            src.sendSuccess(() -> Component.literal("\u00a7c - \u00a7f" + s), false);
        }
        return ids.size();
    }

    private static int clear(CommandSourceStack src) {
        int n = RecipeBans.clearAll(src.getServer());
        src.sendSuccess(() -> Component.literal("\u00a76[Recipes] \u00a7aDesbaneadas todas (" + n + ")."), true);
        return 1;
    }
}
