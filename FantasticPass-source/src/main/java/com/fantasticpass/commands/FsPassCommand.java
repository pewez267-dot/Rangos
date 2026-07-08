package com.fantasticpass.commands;

import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.interop.FantasticRanksInterop;
import com.fantasticpass.network.NametagSync;
import com.fantasticpass.network.OpenAdminScreenPacket;
import com.fantasticpass.network.OpenViewScreenPacket;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.quest.DefaultQuests;
import com.fantasticpass.quest.QuestManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class FsPassCommand {
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PASS_IDS = (ctx, builder) -> {
        MinecraftServer server = ctx.getSource().getServer();
        return server != null ? SharedSuggestionProvider.suggest(PassSavedData.get(server).getPasses().keySet(), builder) : builder.buildFuture();
    };
    // Sugerencias unificadas: none + rangos del pase ganados + rangos de tiempo (mod Ranks) ganados.
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_EARNED_RANKS = (ctx, builder) -> {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            List<String> ids = new ArrayList<>();
            ids.add("none");
            PlayerPassData data = PassCapability.getData((Player) player);
            if (data != null) {
                ids.addAll(data.getEarnedRankIds());
            }
            ids.addAll(FantasticRanksInterop.getEarnedRankIds((Player) player));
            return SharedSuggestionProvider.suggest(ids, builder);
        } catch (CommandSyntaxException ignored) {
        }
        return builder.buildFuture();
    };

    private FsPassCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fspass")
                .then(Commands.literal("create").requires(source -> source.hasPermission(4)).executes(FsPassCommand::create))
                .then(Commands.literal("edit").requires(source -> source.hasPermission(4)).then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_PASS_IDS).executes(FsPassCommand::edit)))
                .then(Commands.literal("delete").requires(source -> source.hasPermission(4)).then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_PASS_IDS).executes(FsPassCommand::delete)))
                .then(Commands.literal("setpremium").requires(source -> source.hasPermission(4)).then(Commands.argument("player", EntityArgument.player()).executes(FsPassCommand::setPremium)))
                .then(Commands.literal("activate").requires(source -> source.hasPermission(4)).then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_PASS_IDS).executes(FsPassCommand::activate)))
                .then(Commands.literal("view").executes(FsPassCommand::view))
                .then(Commands.literal("rango").then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_EARNED_RANKS).executes(FsPassCommand::rango)))
                .then(Commands.literal("test").requires(source -> source.hasPermission(4)).executes(FsPassCommand::test))
                .then(Commands.literal("reset").requires(source -> source.hasPermission(4)).executes(FsPassCommand::resetSelf).then(Commands.argument("player", EntityArgument.player()).executes(FsPassCommand::resetPlayer)))
                .then(Commands.literal("reload").requires(source -> source.hasPermission(4)).executes(FsPassCommand::reload))
                .then(Commands.literal("level").requires(source -> source.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> FsPassCommand.giveLevel(ctx, EntityArgument.getPlayer(ctx, "player"), 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-999, 999))
                                        .executes(ctx -> FsPassCommand.giveLevel(ctx, EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"))))))
                .then(Commands.literal("week").requires(source -> source.hasPermission(4)).then(Commands.argument("number", IntegerArgumentType.integer(1, 52)).executes(FsPassCommand::setWeek))));
    }

    private static int test(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PassSavedData saved = PassSavedData.get(player.getServer());
        PassDefinition pass = saved.getActivePass();
        if (pass == null) {
            ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.no_active_pass"));
            return 0;
        }
        PlayerPassData data = PassCapability.getData((Player) player);
        if (data == null) {
            return 0;
        }
        if (data.isTestMode()) {
            data.exitTestMode();
            QuestManager.ensureDaily(player.getUUID(), data);
            NametagSync.syncPlayer(player);
            PacketHandler.sendToPlayer(player, new OpenViewScreenPacket(pass, data, QuestManager.pointsPerTier(pass)));
            ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.test_off"), false);
            return 1;
        }
        data.enterTestMode();
        data.setPremium(true);
        data.setCurrentWeek(DefaultQuests.effectiveWeekCount(pass));
        data.setCurrentTier(pass.getTierCount());
        data.addPoints(pass.getTierCount() * QuestManager.pointsPerTier(pass) - data.getPoints());
        QuestManager.rerollDaily(player.getUUID(), data);
        NametagSync.syncPlayer(player);
        PacketHandler.sendToPlayer(player, new OpenViewScreenPacket(pass, data, QuestManager.pointsPerTier(pass)));
        ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.test_mode"), false);
        return 1;
    }

    private static int create(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PassDefinition fresh = new PassDefinition("", "Pase Nuevo");
        PacketHandler.sendToPlayer(player, new OpenAdminScreenPacket(fresh));
        return 1;
    }

    private static int edit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id");
        PassSavedData saved = PassSavedData.get(player.getServer());
        PassDefinition pass = saved.getPass(id);
        if (pass == null) {
            ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.pass_not_found", id));
            return 0;
        }
        PacketHandler.sendToPlayer(player, new OpenAdminScreenPacket(pass.copy()));
        return 1;
    }

    private static int resetSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return FsPassCommand.reset(ctx, ctx.getSource().getPlayerOrException());
    }

    private static int resetPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return FsPassCommand.reset(ctx, EntityArgument.getPlayer(ctx, "player"));
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        PassSavedData saved = PassSavedData.get(player.getServer());
        PlayerPassData data = PassCapability.getData((Player) player);
        if (data == null) {
            return 0;
        }
        data.exitTestMode();
        data.resetForNewSeason(saved.getActivePassId());
        QuestManager.ensureDaily(player.getUUID(), data);
        NametagSync.syncPlayer(player);
        // Ya NO se abre la GUI al resetear (solo mensaje de confirmacion).
        ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.reset", player.getGameProfile().getName()), true);
        return 1;
    }

    /**
     * Aplica la configuracion actual del pase activo a TODOS los jugadores en linea, en tiempo real,
     * SIN borrar su progreso. Util despues de editar el pase en la GUI: en vez de reset a todos,
     * solo /fspass reload y se actualiza sobre la marcha (colores de nivel, rangos, quests, limites).
     */
    private static int reload(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        if (server == null) {
            return 0;
        }
        PassSavedData saved = PassSavedData.get(server);
        PassDefinition pass = saved.getActivePass();
        if (pass == null) {
            ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.no_active_pass"));
            return 0;
        }
        int maxTier = pass.getTierCount();
        int maxWeek = DefaultQuests.effectiveWeekCount(pass);
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerPassData data = PassCapability.getData((Player) player);
            if (data == null) {
                continue;
            }
            // Ajusta a los nuevos limites del pase SIN borrar progreso.
            if (data.getCurrentTier() > maxTier) {
                data.setCurrentTier(maxTier);
            }
            if (data.getCurrentWeek() > maxWeek) {
                data.setCurrentWeek(maxWeek);
            }
            if (data.getCurrentWeek() < 1) {
                data.setCurrentWeek(1);
            }
            // Re-genera las quests segun la config nueva y re-sincroniza el nametag (colores/rangos) en vivo.
            QuestManager.ensureDaily(player.getUUID(), data);
            NametagSync.syncPlayer(player);
            ++count;
        }
        int finalCount = count;
        ctx.getSource().sendSuccess(() -> Component.literal("\u00a78[\u00a76Pase\u00a78] \u00a77Configuracion recargada y aplicada a \u00a7f" + finalCount + "\u00a77 jugador(es) en linea."), true);
        return 1;
    }

    /**
     * Sube (o baja, con cantidad negativa) el nivel del pase de un jugador. Deja los tiers reclamables
     * (el jugador reclama sus recompensas en la GUI). Ajusta los puntos para que coincidan con el nivel.
     */
    private static int giveLevel(CommandContext<CommandSourceStack> ctx, ServerPlayer target, int amount) {
        PassSavedData saved = PassSavedData.get(target.getServer());
        PassDefinition pass = saved.getActivePass();
        if (pass == null) {
            ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.no_active_pass"));
            return 0;
        }
        PlayerPassData data = PassCapability.getData((Player) target);
        if (data == null) {
            return 0;
        }
        int ppt = QuestManager.pointsPerTier(pass);
        int newTier = Math.max(0, Math.min(pass.getTierCount(), data.getCurrentTier() + amount));
        data.setCurrentTier(newTier);
        int targetPoints = newTier * ppt;
        if (data.getPoints() < targetPoints) {
            data.addPoints(targetPoints - data.getPoints());
        } else if (amount < 0) {
            data.setPoints(targetPoints);
        }
        NametagSync.syncPlayer(target);
        int finalTier = newTier;
        ctx.getSource().sendSuccess(() -> Component.literal("\u00a78[\u00a76Pase\u00a78] \u00a77" + target.getGameProfile().getName() + " ahora esta en nivel \u00a7f" + finalTier + "\u00a77 del pase."), true);
        target.sendSystemMessage(Component.literal("\u00a76\u2726 Tu nivel del pase ahora es \u00a7e" + finalTier));
        return 1;
    }

    private static int setWeek(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int number = IntegerArgumentType.getInteger(ctx, "number");
        PassSavedData saved = PassSavedData.get(player.getServer());
        PassDefinition pass = saved.getActivePass();
        PlayerPassData data = PassCapability.getData((Player) player);
        if (data == null) {
            return 0;
        }
        int max = DefaultQuests.effectiveWeekCount(pass);
        int week = Math.max(1, Math.min(max, number));
        data.setCurrentWeek(week);
        QuestManager.ensureDaily(player.getUUID(), data);
        NametagSync.syncPlayer(player);
        // Ya NO se abre la GUI al cambiar de semana (solo view y test abren la pantalla).
        int finalWeek = week;
        ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.week_set", finalWeek), false);
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        String id = StringArgumentType.getString(ctx, "id");
        PassSavedData saved = PassSavedData.get(server);
        if (saved.deletePass(id)) {
            ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.pass_deleted", id), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.pass_not_found", id));
        return 0;
    }

    private static int setPremium(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PlayerPassData data = PassCapability.getData((Player) target);
        if (data == null) {
            return 0;
        }
        data.setPremium(true);
        ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.premium_set", target.getGameProfile().getName()), true);
        return 1;
    }

    private static int activate(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        String id = StringArgumentType.getString(ctx, "id");
        PassSavedData saved = PassSavedData.get(server);
        if (!saved.hasPass(id)) {
            ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.pass_not_found", id));
            return 0;
        }
        saved.setActivePassId(id);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerPassData data = PassCapability.getData((Player) player);
            if (data == null) {
                continue;
            }
            data.resetForNewSeason(id);
            NametagSync.syncPlayer(player);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.pass_activated", id), true);
        return 1;
    }

    private static int view(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PassSavedData saved = PassSavedData.get(player.getServer());
        PassDefinition pass = saved.getActivePass();
        if (pass == null) {
            ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.no_active_pass"));
            return 0;
        }
        PlayerPassData data = PassCapability.getData((Player) player);
        if (data == null) {
            return 0;
        }
        QuestManager.ensureDaily(player.getUUID(), data);
        PacketHandler.sendToPlayer(player, new OpenViewScreenPacket(pass, data, QuestManager.pointsPerTier(pass)));
        return 1;
    }

    private static int rango(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id");
        PlayerPassData data = PassCapability.getData((Player) player);
        if (data == null) {
            return 0;
        }
        if (id.equalsIgnoreCase("none") || id.equalsIgnoreCase("clear")) {
            data.setDisplayedRankId(null);
            NametagSync.syncPlayer(player);
            ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.rank_set", "none"), false);
            return 1;
        }
        boolean ownsPassRank = data.hasEarnedRank(id);
        boolean ownsTimeRank = !ownsPassRank && FantasticRanksInterop.getRankDescriptor((Player) player, id) != null;
        if (!ownsPassRank && !ownsTimeRank) {
            ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.rank_not_owned", id));
            return 0;
        }
        data.setDisplayedRankId(id);
        NametagSync.syncPlayer(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.rank_set", id), false);
        return 1;
    }
}
