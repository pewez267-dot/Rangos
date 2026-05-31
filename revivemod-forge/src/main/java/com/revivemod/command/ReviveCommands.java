package com.revivemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.revivemod.RevivemodForge;
import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;

/**
 * /revive command tree - help, surrender, self, status, reload, force, kill, down and the
 * set sub-commands (time, distance, channel, selfcost). Replaces Fabric's CommandRegistrationCallback.
 */
public final class ReviveCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("revive");
        root.executes(ReviveCommands::help);
        root.then(Commands.literal("help").executes(ReviveCommands::help));
        root.then(Commands.literal("surrender").executes(ReviveCommands::surrender));
        root.then(Commands.literal("self").executes(ReviveCommands::selfRevive));
        root.then(Commands.literal("status").requires(s -> s.hasPermission(2)).executes(ReviveCommands::status));
        root.then(Commands.literal("reload").requires(s -> s.hasPermission(2)).executes(ReviveCommands::reload));
        root.then(Commands.literal("force").requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players()).executes(ReviveCommands::forceRevive)));
        root.then(Commands.literal("kill").requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players()).executes(ReviveCommands::forceKill)));
        root.then(Commands.literal("down").requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players()).executes(ReviveCommands::forceDown)));

        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set").requires(s -> s.hasPermission(2));
        set.then(Commands.literal("time").then(Commands.argument("seconds", IntegerArgumentType.integer(1, 6000)).executes(ctx -> {
            int v = IntegerArgumentType.getInteger(ctx, "seconds");
            RevivemodForge.getConfig().downTimeSeconds = v;
            RevivemodForge.saveConfig();
            ctx.getSource().sendSuccess(() -> green("downTimeSeconds = " + v), true);
            return 1;
        })));
        set.then(Commands.literal("distance").then(Commands.argument("blocks", DoubleArgumentType.doubleArg(1.0, 32.0)).executes(ctx -> {
            double v = DoubleArgumentType.getDouble(ctx, "blocks");
            RevivemodForge.getConfig().reviveDistance = v;
            RevivemodForge.saveConfig();
            ctx.getSource().sendSuccess(() -> green("reviveDistance = " + v), true);
            return 1;
        })));
        set.then(Commands.literal("channel").then(Commands.argument("ticks", IntegerArgumentType.integer(20, 6000)).executes(ctx -> {
            int v = IntegerArgumentType.getInteger(ctx, "ticks");
            RevivemodForge.getConfig().reviveTimeTicks = v;
            RevivemodForge.saveConfig();
            ctx.getSource().sendSuccess(() -> green("reviveTimeTicks = " + v), true);
            return 1;
        })));
        set.then(Commands.literal("selfcost").then(Commands.argument("levels", IntegerArgumentType.integer(0, 1000)).executes(ctx -> {
            int v = IntegerArgumentType.getInteger(ctx, "levels");
            RevivemodForge.getConfig().selfReviveLevelCost = v;
            RevivemodForge.saveConfig();
            ctx.getSource().sendSuccess(() -> green("selfReviveLevelCost = " + v), true);
            return 1;
        })));
        root.then(set);

        dispatcher.register(root);
    }

    private static int surrender(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) {
            return 0;
        }
        if (!DownManager.isDown(p)) {
            p.displayClientMessage(Component.literal("No est\u00e1s desangr\u00e1ndote.").withStyle(ChatFormatting.RED), false);
            return 0;
        }
        p.displayClientMessage(Component.literal("Te has rendido.").withStyle(ChatFormatting.DARK_RED), false);
        DownManager.forceDeath(p, p.damageSources().genericKill());
        return 1;
    }

    private static int selfRevive(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) {
            return 0;
        }
        if (!DownManager.isDown(p)) {
            p.displayClientMessage(Component.literal("No est\u00e1s desangr\u00e1ndote.").withStyle(ChatFormatting.RED), false);
            return 0;
        }
        if (!RevivemodForge.getConfig().allowSelfRevive) {
            p.displayClientMessage(Component.literal("El auto-revivir est\u00e1 desactivado.").withStyle(ChatFormatting.RED), false);
            return 0;
        }
        int cost = RevivemodForge.getConfig().selfReviveLevelCost;
        if (p.experienceLevel < cost) {
            p.displayClientMessage(Component.literal("Necesitas " + cost + " niveles de experiencia (tienes " + p.experienceLevel + ").")
                    .withStyle(ChatFormatting.RED), false);
            return 0;
        }
        if (DownManager.selfRevive(p)) {
            p.displayClientMessage(Component.literal("Te has revivido por " + cost + " niveles.").withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        return 0;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.literal("--- Revive Mod ---").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        src.sendSuccess(() -> Component.literal("/revive surrender").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - rendirte y morir (estando desangr\u00e1ndote)").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/revive self").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - auto-revivirte pagando niveles").withStyle(ChatFormatting.GRAY)), false);
        if (src.hasPermission(2)) {
            src.sendSuccess(() -> Component.literal("/revive status | force | kill | down | set | reload").withStyle(ChatFormatting.AQUA), false);
        }
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (DownManager.all().isEmpty()) {
            src.sendSuccess(() -> Component.literal("No hay jugadores desangr\u00e1ndose.").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Desangr\u00e1ndose:").withStyle(ChatFormatting.GOLD), false);
        for (DownState st : DownManager.all()) {
            ServerPlayer p = src.getServer().getPlayerList().getPlayer(st.playerUuid);
            String name = p != null ? p.getGameProfile().getName() : st.playerUuid.toString();
            int sec = (st.remainingTicks + 19) / 20;
            src.sendSuccess(() -> Component.literal(" - " + name + " (" + sec + "s restantes)").withStyle(ChatFormatting.YELLOW), false);
        }
        return DownManager.all().size();
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        RevivemodForge.reloadConfig();
        ctx.getSource().sendSuccess(() -> green("Config recargada."), true);
        return 1;
    }

    private static int forceRevive(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int n = 0;
        for (ServerPlayer p : targets) {
            if (!DownManager.isDown(p)) {
                continue;
            }
            DownManager.revive(p);
            ++n;
        }
        int count = n;
        ctx.getSource().sendSuccess(() -> green("Revividos " + count + " jugador(es)."), true);
        return n;
    }

    private static int forceKill(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int n = 0;
        for (ServerPlayer p : targets) {
            if (!DownManager.isDown(p)) {
                continue;
            }
            DownManager.forceDeath(p, p.damageSources().genericKill());
            ++n;
        }
        int count = n;
        ctx.getSource().sendSuccess(() -> green("Eliminados " + count + " noqueado(s)."), true);
        return n;
    }

    private static int forceDown(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int n = 0;
        for (ServerPlayer p : targets) {
            if (DownManager.isDown(p) || !p.isAlive() || p.isCreative() || p.isSpectator()) {
                continue;
            }
            DownManager.knockDown(p, p.damageSources().generic());
            ++n;
        }
        int count = n;
        ctx.getSource().sendSuccess(() -> green("Noqueados " + count + " jugador(es)."), true);
        return n;
    }

    private static Component green(String s) {
        return Component.literal(s).withStyle(ChatFormatting.GREEN);
    }
}
