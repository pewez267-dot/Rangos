package com.revivemod.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.revivemod.ReviveMod;
import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;

/**
 * Commands.
 *
 * Player (no permission needed, only works while downed):
 *   /revive surrender              - give up and die now
 *   /revive self                   - self-revive paying XP levels
 *   /revive help                   - help
 *
 * Admin (op level 2):
 *   /revive status                 - list everyone currently downed
 *   /revive force <player>         - revive a player instantly
 *   /revive kill <player>          - end the downed timer (real death)
 *   /revive down <player>          - knock a player down for testing
 *   /revive set time <seconds>
 *   /revive set distance <blocks>
 *   /revive set channel <ticks>
 *   /revive reload
 */
public final class ReviveCommands {

    private ReviveCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            // Root requires nothing so downed players can surrender / self-revive.
            LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("revive");

            root.executes(ReviveCommands::help);
            root.then(CommandManager.literal("help").executes(ReviveCommands::help));

            // ---- player commands (no permission) ----
            root.then(CommandManager.literal("surrender").executes(ReviveCommands::surrender));
            root.then(CommandManager.literal("self").executes(ReviveCommands::selfRevive));

            // ---- admin commands (op level 2) ----
            root.then(CommandManager.literal("status")
                    .requires(s -> s.hasPermissionLevel(2)).executes(ReviveCommands::status));
            root.then(CommandManager.literal("reload")
                    .requires(s -> s.hasPermissionLevel(2)).executes(ReviveCommands::reload));

            root.then(CommandManager.literal("force")
                    .requires(s -> s.hasPermissionLevel(2))
                    .then(CommandManager.argument("targets", EntityArgumentType.players())
                            .executes(ReviveCommands::forceRevive)));
            root.then(CommandManager.literal("kill")
                    .requires(s -> s.hasPermissionLevel(2))
                    .then(CommandManager.argument("targets", EntityArgumentType.players())
                            .executes(ReviveCommands::forceKill)));
            root.then(CommandManager.literal("down")
                    .requires(s -> s.hasPermissionLevel(2))
                    .then(CommandManager.argument("targets", EntityArgumentType.players())
                            .executes(ReviveCommands::forceDown)));

            LiteralArgumentBuilder<ServerCommandSource> set = CommandManager.literal("set")
                    .requires(s -> s.hasPermissionLevel(2));
            set.then(CommandManager.literal("time")
                    .then(CommandManager.argument("seconds", IntegerArgumentType.integer(1, 6000))
                            .executes(ctx -> {
                                int v = IntegerArgumentType.getInteger(ctx, "seconds");
                                ReviveMod.getConfig().downTimeSeconds = v;
                                ReviveMod.saveConfig();
                                ctx.getSource().sendFeedback(() -> green("downTimeSeconds = " + v), true);
                                return Command.SINGLE_SUCCESS;
                            })));
            set.then(CommandManager.literal("distance")
                    .then(CommandManager.argument("blocks", DoubleArgumentType.doubleArg(1.0, 32.0))
                            .executes(ctx -> {
                                double v = DoubleArgumentType.getDouble(ctx, "blocks");
                                ReviveMod.getConfig().reviveDistance = v;
                                ReviveMod.saveConfig();
                                ctx.getSource().sendFeedback(() -> green("reviveDistance = " + v), true);
                                return Command.SINGLE_SUCCESS;
                            })));
            set.then(CommandManager.literal("channel")
                    .then(CommandManager.argument("ticks", IntegerArgumentType.integer(20, 6000))
                            .executes(ctx -> {
                                int v = IntegerArgumentType.getInteger(ctx, "ticks");
                                ReviveMod.getConfig().reviveTimeTicks = v;
                                ReviveMod.saveConfig();
                                ctx.getSource().sendFeedback(() -> green("reviveTimeTicks = " + v), true);
                                return Command.SINGLE_SUCCESS;
                            })));
            set.then(CommandManager.literal("selfcost")
                    .then(CommandManager.argument("levels", IntegerArgumentType.integer(0, 1000))
                            .executes(ctx -> {
                                int v = IntegerArgumentType.getInteger(ctx, "levels");
                                ReviveMod.getConfig().selfReviveLevelCost = v;
                                ReviveMod.saveConfig();
                                ctx.getSource().sendFeedback(() -> green("selfReviveLevelCost = " + v), true);
                                return Command.SINGLE_SUCCESS;
                            })));
            root.then(set);

            dispatcher.register(root);
        });
    }

    // ---- player commands ----

    private static int surrender(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        if (!DownManager.isDown(p)) {
            p.sendMessage(Text.literal("No estas noqueado.").formatted(Formatting.RED), false);
            return 0;
        }
        p.sendMessage(Text.literal("Te has rendido.").formatted(Formatting.DARK_RED), false);
        DownManager.forceDeath(p, p.getDamageSources().genericKill());
        return Command.SINGLE_SUCCESS;
    }

    private static int selfRevive(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        if (!DownManager.isDown(p)) {
            p.sendMessage(Text.literal("No estas noqueado.").formatted(Formatting.RED), false);
            return 0;
        }
        if (!ReviveMod.getConfig().allowSelfRevive) {
            p.sendMessage(Text.literal("El auto-revivir esta desactivado.").formatted(Formatting.RED), false);
            return 0;
        }
        int cost = ReviveMod.getConfig().selfReviveLevelCost;
        if (p.experienceLevel < cost) {
            p.sendMessage(Text.literal("Necesitas " + cost + " niveles de experiencia (tienes " + p.experienceLevel + ").")
                    .formatted(Formatting.RED), false);
            return 0;
        }
        if (DownManager.selfRevive(p)) {
            p.sendMessage(Text.literal("Te has revivido por " + cost + " niveles.").formatted(Formatting.GREEN), false);
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }

    // ---- shared ----

    private static int help(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("--- Revive Mod ---").formatted(Formatting.GOLD, Formatting.BOLD), false);
        src.sendFeedback(() -> Text.literal("/revive surrender").formatted(Formatting.YELLOW)
                .append(Text.literal(" - rendirte y morir (estando noqueado)").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/revive self").formatted(Formatting.YELLOW)
                .append(Text.literal(" - auto-revivirte pagando niveles").formatted(Formatting.GRAY)), false);
        if (src.hasPermissionLevel(2)) {
            src.sendFeedback(() -> Text.literal("/revive status | force | kill | down | set | reload")
                    .formatted(Formatting.AQUA), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int status(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (DownManager.all().isEmpty()) {
            src.sendFeedback(() -> Text.literal("No hay jugadores noqueados.").formatted(Formatting.GRAY), false);
            return 0;
        }
        src.sendFeedback(() -> Text.literal("Noqueados:").formatted(Formatting.GOLD), false);
        for (DownState st : DownManager.all()) {
            ServerPlayerEntity p = src.getServer().getPlayerManager().getPlayer(st.playerUuid);
            String name = p != null ? p.getGameProfile().getName() : st.playerUuid.toString();
            int sec = (st.remainingTicks + 19) / 20;
            src.sendFeedback(() -> Text.literal(" - " + name + " (" + sec + "s restantes)")
                    .formatted(Formatting.YELLOW), false);
        }
        return DownManager.all().size();
    }

    private static int reload(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx) {
        ReviveMod.reloadConfig();
        ctx.getSource().sendFeedback(() -> green("Config recargada."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int forceRevive(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
        int n = 0;
        for (ServerPlayerEntity p : targets) {
            if (DownManager.isDown(p)) {
                DownManager.revive(p);
                n++;
            }
        }
        final int count = n;
        ctx.getSource().sendFeedback(() -> green("Revividos " + count + " jugador(es)."), true);
        return n;
    }

    private static int forceKill(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
        int n = 0;
        for (ServerPlayerEntity p : targets) {
            if (DownManager.isDown(p)) {
                DownManager.forceDeath(p, p.getDamageSources().genericKill());
                n++;
            }
        }
        final int count = n;
        ctx.getSource().sendFeedback(() -> green("Eliminados " + count + " noqueado(s)."), true);
        return n;
    }

    private static int forceDown(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
        int n = 0;
        for (ServerPlayerEntity p : targets) {
            if (!DownManager.isDown(p) && p.isAlive() && !p.isCreative() && !p.isSpectator()) {
                DownManager.knockDown(p, p.getDamageSources().generic());
                n++;
            }
        }
        final int count = n;
        ctx.getSource().sendFeedback(() -> green("Noqueados " + count + " jugador(es)."), true);
        return n;
    }

    private static Text green(String s) {
        return Text.literal(s).formatted(Formatting.GREEN);
    }
}
