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
 * Admin commands:
 *   /revive help
 *   /revive status                 - list everyone currently downed
 *   /revive force <player>         - revive a player instantly
 *   /revive kill <player>          - end the downed timer (real death)
 *   /revive down <player>          - knock a player down for testing
 *   /revive set time <seconds>     - change downTimeSeconds
 *   /revive set distance <blocks>  - change reviveDistance
 *   /revive set channel <ticks>    - change reviveTimeTicks
 *   /revive reload                 - re-read config from disk
 */
public final class ReviveCommands {

    private ReviveCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("revive")
                    .requires(src -> src.hasPermissionLevel(2));

            root.executes(ReviveCommands::help);
            root.then(CommandManager.literal("help").executes(ReviveCommands::help));
            root.then(CommandManager.literal("status").executes(ReviveCommands::status));
            root.then(CommandManager.literal("reload").executes(ReviveCommands::reload));

            root.then(CommandManager.literal("force")
                    .then(CommandManager.argument("targets", EntityArgumentType.players())
                            .executes(ReviveCommands::forceRevive)));
            root.then(CommandManager.literal("kill")
                    .then(CommandManager.argument("targets", EntityArgumentType.players())
                            .executes(ReviveCommands::forceKill)));
            root.then(CommandManager.literal("down")
                    .then(CommandManager.argument("targets", EntityArgumentType.players())
                            .executes(ReviveCommands::forceDown)));

            LiteralArgumentBuilder<ServerCommandSource> set = CommandManager.literal("set");
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
            root.then(set);

            dispatcher.register(root);
        });
    }

    private static int help(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("--- Revive Mod ---").formatted(Formatting.GOLD, Formatting.BOLD), false);
        src.sendFeedback(() -> Text.literal("/revive status").formatted(Formatting.YELLOW)
                .append(Text.literal(" - lista jugadores noqueados").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/revive force <jugador>").formatted(Formatting.YELLOW)
                .append(Text.literal(" - revivir al jugador").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/revive kill <jugador>").formatted(Formatting.YELLOW)
                .append(Text.literal(" - matar al jugador noqueado").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/revive down <jugador>").formatted(Formatting.YELLOW)
                .append(Text.literal(" - noquear (test)").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/revive set time|distance|channel <valor>").formatted(Formatting.YELLOW), false);
        src.sendFeedback(() -> Text.literal("/revive reload").formatted(Formatting.YELLOW)
                .append(Text.literal(" - recargar config").formatted(Formatting.GRAY)), false);
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
