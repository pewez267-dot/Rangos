package com.revivemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.revivemod.RevivemodForge;
import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import java.util.Collection;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ReviveCommands {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher dispatcher = event.getDispatcher();
        LiteralArgumentBuilder root = Commands.literal((String)"revive");
        root.executes(ReviveCommands::help);
        root.then(Commands.literal((String)"help").executes(ReviveCommands::help));
        root.then(Commands.literal((String)"surrender").executes(ReviveCommands::surrender));
        root.then(Commands.literal((String)"self").executes(ReviveCommands::selfRevive));
        root.then(((LiteralArgumentBuilder)Commands.literal((String)"status").requires(s -> s.hasPermission(2))).executes(ReviveCommands::status));
        root.then(((LiteralArgumentBuilder)Commands.literal((String)"reload").requires(s -> s.hasPermission(2))).executes(ReviveCommands::reload));
        root.then(((LiteralArgumentBuilder)Commands.literal((String)"force").requires(s -> s.hasPermission(2))).then(Commands.argument((String)"targets", (ArgumentType)EntityArgument.players()).executes(ReviveCommands::forceRevive)));
        root.then(((LiteralArgumentBuilder)Commands.literal((String)"kill").requires(s -> s.hasPermission(2))).then(Commands.argument((String)"targets", (ArgumentType)EntityArgument.players()).executes(ReviveCommands::forceKill)));
        root.then(((LiteralArgumentBuilder)Commands.literal((String)"down").requires(s -> s.hasPermission(2))).then(Commands.argument((String)"targets", (ArgumentType)EntityArgument.players()).executes(ReviveCommands::forceDown)));
        LiteralArgumentBuilder set = (LiteralArgumentBuilder)Commands.literal((String)"set").requires(s -> s.hasPermission(2));
        set.then(Commands.literal((String)"time").then(Commands.argument((String)"seconds", (ArgumentType)IntegerArgumentType.integer((int)1, (int)6000)).executes(ctx -> {
            int v;
            RevivemodForge.getConfig().downTimeSeconds = v = IntegerArgumentType.getInteger((CommandContext)ctx, (String)"seconds");
            RevivemodForge.saveConfig();
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> ReviveCommands.green("downTimeSeconds = " + v), true);
            return 1;
        })));
        set.then(Commands.literal((String)"distance").then(Commands.argument((String)"blocks", (ArgumentType)DoubleArgumentType.doubleArg((double)1.0, (double)32.0)).executes(ctx -> {
            double v;
            RevivemodForge.getConfig().reviveDistance = v = DoubleArgumentType.getDouble((CommandContext)ctx, (String)"blocks");
            RevivemodForge.saveConfig();
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> ReviveCommands.green("reviveDistance = " + v), true);
            return 1;
        })));
        set.then(Commands.literal((String)"channel").then(Commands.argument((String)"ticks", (ArgumentType)IntegerArgumentType.integer((int)20, (int)6000)).executes(ctx -> {
            int v;
            RevivemodForge.getConfig().reviveTimeTicks = v = IntegerArgumentType.getInteger((CommandContext)ctx, (String)"ticks");
            RevivemodForge.saveConfig();
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> ReviveCommands.green("reviveTimeTicks = " + v), true);
            return 1;
        })));
        set.then(Commands.literal((String)"selfcost").then(Commands.argument((String)"levels", (ArgumentType)IntegerArgumentType.integer((int)0, (int)1000)).executes(ctx -> {
            int v;
            RevivemodForge.getConfig().selfReviveLevelCost = v = IntegerArgumentType.getInteger((CommandContext)ctx, (String)"levels");
            RevivemodForge.saveConfig();
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> ReviveCommands.green("selfReviveLevelCost = " + v), true);
            return 1;
        })));
        root.then((ArgumentBuilder)set);
        dispatcher.register(root);
    }

    private static int surrender(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayer();
        if (p == null) {
            return 0;
        }
        if (!DownManager.isDown(p)) {
            p.displayClientMessage((Component)Component.literal((String)"No est\u00e1s desangr\u00e1ndote.").withStyle(ChatFormatting.RED), false);
            return 0;
        }
        p.displayClientMessage((Component)Component.literal((String)"Te has rendido.").withStyle(ChatFormatting.DARK_RED), false);
        DownManager.forceDeath(p, p.damageSources().genericKill());
        return 1;
    }

    private static int selfRevive(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayer();
        if (p == null) {
            return 0;
        }
        if (!DownManager.isDown(p)) {
            p.displayClientMessage((Component)Component.literal((String)"No est\u00e1s desangr\u00e1ndote.").withStyle(ChatFormatting.RED), false);
            return 0;
        }
        if (!RevivemodForge.getConfig().allowSelfRevive) {
            p.displayClientMessage((Component)Component.literal((String)"El auto-revivir est\u00e1 desactivado.").withStyle(ChatFormatting.RED), false);
            return 0;
        }
        int cost = RevivemodForge.getConfig().selfReviveLevelCost;
        if (p.experienceLevel < cost) {
            p.displayClientMessage((Component)Component.literal((String)("Necesitas " + cost + " niveles de experiencia (tienes " + p.experienceLevel + ").")).withStyle(ChatFormatting.RED), false);
            return 0;
        }
        if (DownManager.selfRevive(p)) {
            p.displayClientMessage((Component)Component.literal((String)("Te has revivido por " + cost + " niveles.")).withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        return 0;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = (CommandSourceStack)ctx.getSource();
        src.sendSuccess(() -> Component.literal((String)"--- Revive Mod ---").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}), false);
        src.sendSuccess(() -> Component.literal((String)"/revive surrender").withStyle(ChatFormatting.YELLOW).append((Component)Component.literal((String)" - rendirte y morir (estando desangr\u00e1ndote)").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal((String)"/revive self").withStyle(ChatFormatting.YELLOW).append((Component)Component.literal((String)" - auto-revivirte pagando niveles").withStyle(ChatFormatting.GRAY)), false);
        if (src.hasPermission(2)) {
            src.sendSuccess(() -> Component.literal((String)"/revive status | force | kill | down | set | reload").withStyle(ChatFormatting.AQUA), false);
        }
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = (CommandSourceStack)ctx.getSource();
        if (DownManager.all().isEmpty()) {
            src.sendSuccess(() -> Component.literal((String)"No hay jugadores desangr\u00e1ndose.").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal((String)"Desangr\u00e1ndose:").withStyle(ChatFormatting.GOLD), false);
        for (DownState st : DownManager.all()) {
            ServerPlayer p = src.getServer().getPlayerList().getPlayer(st.playerUuid);
            String name = p != null ? p.getGameProfile().getName() : st.playerUuid.toString();
            int sec = (st.remainingTicks + 19) / 20;
            src.sendSuccess(() -> Component.literal((String)(" - " + name + " (" + sec + "s restantes)")).withStyle(ChatFormatting.YELLOW), false);
        }
        return DownManager.all().size();
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        RevivemodForge.reloadConfig();
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> ReviveCommands.green("Config recargada."), true);
        return 1;
    }

    private static int forceRevive(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, (String)"targets");
        int n = 0;
        for (ServerPlayer p : targets) {
            if (!DownManager.isDown(p)) continue;
            DownManager.revive(p);
            ++n;
        }
        int count = n;
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> ReviveCommands.green("Revividos " + count + " jugador(es)."), true);
        return n;
    }

    private static int forceKill(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, (String)"targets");
        int n = 0;
        for (ServerPlayer p : targets) {
            if (!DownManager.isDown(p)) continue;
            DownManager.forceDeath(p, p.damageSources().genericKill());
            ++n;
        }
        int count = n;
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> ReviveCommands.green("Eliminados " + count + " noqueado(s)."), true);
        return n;
    }

    private static int forceDown(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, (String)"targets");
        int n = 0;
        for (ServerPlayer p : targets) {
            if (DownManager.isDown(p) || !p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            DownManager.knockDown(p, p.damageSources().generic());
            ++n;
        }
        int count = n;
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> ReviveCommands.green("Noqueados " + count + " jugador(es)."), true);
        return n;
    }

    private static Component green(String s) {
        return Component.literal((String)s).withStyle(ChatFormatting.GREEN);
    }
}

