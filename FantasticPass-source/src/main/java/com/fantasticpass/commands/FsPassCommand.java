/*
 * Decompiled with CFR 0.152.
 */
package com.fantasticpass.commands;

import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.network.NametagSync;
import com.fantasticpass.network.OpenAdminScreenPacket;
import com.fantasticpass.network.OpenViewScreenPacket;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.quest.DefaultQuests;
import com.fantasticpass.quest.QuestManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
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
        MinecraftServer server = ((CommandSourceStack)ctx.getSource()).getServer();
        return server != null ? SharedSuggestionProvider.suggest(PassSavedData.get(server).getPasses().keySet(), (SuggestionsBuilder)builder) : builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_EARNED_RANKS = (ctx, builder) -> {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<String>();
        ids.add("none");
        try {
            ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
            PlayerPassData data = PassCapability.getData((Player)player);
            if (data != null) {
                ids.addAll(data.getEarnedRankIds());
            }
        }
        catch (CommandSyntaxException commandSyntaxException) {
            // empty catch block
        }
        if (((CommandSourceStack)ctx.getSource()).hasPermission(2)) {
            MinecraftServer server = ((CommandSourceStack)ctx.getSource()).getServer();
            if (server != null) {
                PassDefinition pass = PassSavedData.get(server).getActivePass();
                if (pass != null) {
                    for (com.fantasticpass.data.TierDefinition t : pass.getTiers()) {
                        if (t == null || !t.hasRankReward()) continue;
                        ids.add(t.getRankReward().getRankId());
                    }
                }
            }
        }
        return SharedSuggestionProvider.suggest(ids, (SuggestionsBuilder)builder);
    };

    private FsPassCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"fspass").then(((LiteralArgumentBuilder)Commands.literal((String)"create").requires(source -> source.hasPermission(4))).executes(FsPassCommand::create))).then(((LiteralArgumentBuilder)Commands.literal((String)"edit").requires(source -> source.hasPermission(4))).then(Commands.argument((String)"id", (ArgumentType)StringArgumentType.string()).suggests(SUGGEST_PASS_IDS).executes(FsPassCommand::edit)))).then(((LiteralArgumentBuilder)Commands.literal((String)"delete").requires(source -> source.hasPermission(4))).then(Commands.argument((String)"id", (ArgumentType)StringArgumentType.string()).suggests(SUGGEST_PASS_IDS).executes(FsPassCommand::delete)))).then(((LiteralArgumentBuilder)Commands.literal((String)"setpremium").requires(source -> source.hasPermission(4))).then(Commands.argument((String)"player", (ArgumentType)EntityArgument.player()).executes(FsPassCommand::setPremium)))).then(((LiteralArgumentBuilder)Commands.literal((String)"activate").requires(source -> source.hasPermission(4))).then(Commands.argument((String)"id", (ArgumentType)StringArgumentType.string()).suggests(SUGGEST_PASS_IDS).executes(FsPassCommand::activate)))).then(Commands.literal((String)"view").executes(FsPassCommand::view))).then(Commands.literal((String)"rango").then(Commands.argument((String)"id", (ArgumentType)StringArgumentType.string()).suggests(SUGGEST_EARNED_RANKS).executes(FsPassCommand::rango))).then(((LiteralArgumentBuilder)Commands.literal((String)"test").requires(source -> source.hasPermission(4))).executes(FsPassCommand::test)).then(((LiteralArgumentBuilder)Commands.literal((String)"reset").requires(source -> source.hasPermission(4))).executes(FsPassCommand::resetSelf).then(Commands.argument((String)"player", (ArgumentType)EntityArgument.player()).executes(FsPassCommand::resetPlayer))).then(((LiteralArgumentBuilder)Commands.literal((String)"week").requires(source -> source.hasPermission(4))).then(Commands.argument((String)"number", (ArgumentType)IntegerArgumentType.integer((int)1, (int)52)).executes(FsPassCommand::setWeek))));
    }

    private static int test(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
        PassSavedData saved = PassSavedData.get(player.getServer());
        PassDefinition pass = saved.getActivePass();
        if (pass == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"fantasticpass.msg.no_active_pass"));
            return 0;
        }
        PlayerPassData data = PassCapability.getData((Player)player);
        if (data == null) {
            return 0;
        }
        if (data.isTestMode()) {
            data.exitTestMode();
            QuestManager.ensureDaily(player.getUUID(), data);
            NametagSync.syncPlayer(player);
            PacketHandler.sendToPlayer(player, new OpenViewScreenPacket(pass, data, QuestManager.pointsPerTier(pass)));
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"fantasticpass.msg.test_off"), false);
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
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"fantasticpass.msg.test_mode"), false);
        return 1;
    }

    private static int create(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
        PassDefinition fresh = new PassDefinition("", "New Pass");
        PacketHandler.sendToPlayer(player, new OpenAdminScreenPacket(fresh));
        return 1;
    }

    private static int edit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
        String id = StringArgumentType.getString(ctx, (String)"id");
        PassSavedData saved = PassSavedData.get(player.getServer());
        PassDefinition pass = saved.getPass(id);
        if (pass == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"fantasticpass.msg.pass_not_found", (Object[])new Object[]{id}));
            return 0;
        }
        PacketHandler.sendToPlayer(player, new OpenAdminScreenPacket(pass.copy()));
        return 1;
    }

    private static int resetSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return FsPassCommand.reset(ctx, ((CommandSourceStack)ctx.getSource()).getPlayerOrException());
    }

    private static int resetPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return FsPassCommand.reset(ctx, EntityArgument.getPlayer(ctx, (String)"player"));
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        PassSavedData saved = PassSavedData.get(player.getServer());
        PassDefinition pass = saved.getActivePass();
        PlayerPassData data = PassCapability.getData((Player)player);
        if (data == null) {
            return 0;
        }
        data.exitTestMode();
        data.resetForNewSeason(saved.getActivePassId());
        QuestManager.ensureDaily(player.getUUID(), data);
        NametagSync.syncPlayer(player);
        if (pass != null) {
            PacketHandler.sendToPlayer(player, new OpenViewScreenPacket(pass, data, QuestManager.pointsPerTier(pass)));
        }
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"fantasticpass.msg.reset", (Object[])new Object[]{player.getGameProfile().getName()}), true);
        return 1;
    }

    private static int setWeek(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
        int number = IntegerArgumentType.getInteger(ctx, (String)"number");
        PassSavedData saved = PassSavedData.get(player.getServer());
        PassDefinition pass = saved.getActivePass();
        PlayerPassData data = PassCapability.getData((Player)player);
        if (data == null) {
            return 0;
        }
        int max = DefaultQuests.effectiveWeekCount(pass);
        int week = Math.max(1, Math.min(max, number));
        data.setCurrentWeek(week);
        QuestManager.ensureDaily(player.getUUID(), data);
        NametagSync.syncPlayer(player);
        if (pass != null) {
            PacketHandler.sendToPlayer(player, new OpenViewScreenPacket(pass, data, QuestManager.pointsPerTier(pass)));
        }
        int finalWeek = week;
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"fantasticpass.msg.week_set", (Object[])new Object[]{finalWeek}), false);
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ((CommandSourceStack)ctx.getSource()).getServer();
        String id = StringArgumentType.getString(ctx, (String)"id");
        PassSavedData saved = PassSavedData.get(server);
        if (saved.deletePass(id)) {
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"fantasticpass.msg.pass_deleted", (Object[])new Object[]{id}), true);
            return 1;
        }
        ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"fantasticpass.msg.pass_not_found", (Object[])new Object[]{id}));
        return 0;
    }

    private static int setPremium(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, (String)"player");
        PlayerPassData data = PassCapability.getData((Player)target);
        if (data == null) {
            return 0;
        }
        data.setPremium(true);
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"fantasticpass.msg.premium_set", (Object[])new Object[]{target.getGameProfile().getName()}), true);
        return 1;
    }

    private static int activate(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ((CommandSourceStack)ctx.getSource()).getServer();
        String id = StringArgumentType.getString(ctx, (String)"id");
        PassSavedData saved = PassSavedData.get(server);
        if (!saved.hasPass(id)) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"fantasticpass.msg.pass_not_found", (Object[])new Object[]{id}));
            return 0;
        }
        saved.setActivePassId(id);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerPassData data = PassCapability.getData((Player)player);
            if (data == null) continue;
            data.resetForNewSeason(id);
            NametagSync.syncPlayer(player);
        }
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"fantasticpass.msg.pass_activated", (Object[])new Object[]{id}), true);
        return 1;
    }

    private static int view(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
        PassSavedData saved = PassSavedData.get(player.getServer());
        PassDefinition pass = saved.getActivePass();
        if (pass == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"fantasticpass.msg.no_active_pass"));
            return 0;
        }
        PlayerPassData data = PassCapability.getData((Player)player);
        if (data == null) {
            return 0;
        }
        QuestManager.ensureDaily(player.getUUID(), data);
        PacketHandler.sendToPlayer(player, new OpenViewScreenPacket(pass, data, QuestManager.pointsPerTier(pass)));
        return 1;
    }

    private static int rango(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
        String id = StringArgumentType.getString(ctx, (String)"id");
        PlayerPassData data = PassCapability.getData((Player)player);
        if (data == null) {
            return 0;
        }
        if (id.equalsIgnoreCase("none") || id.equalsIgnoreCase("clear")) {
            data.setDisplayedRankId(null);
            NametagSync.syncPlayer(player);
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"fantasticpass.msg.rank_set", (Object[])new Object[]{"none"}), false);
            return 1;
        }
        String rankId = id;
        if (!data.hasEarnedRank(rankId)) {
            boolean granted = false;
            if (((CommandSourceStack)ctx.getSource()).hasPermission(2)) {
                MinecraftServer server = player.getServer();
                PassDefinition pass = server != null ? PassSavedData.get(server).getActivePass() : null;
                if (pass != null) {
                    for (com.fantasticpass.data.TierDefinition t : pass.getTiers()) {
                        if (t == null || !t.hasRankReward() || !t.getRankReward().getRankId().equalsIgnoreCase(rankId)) continue;
                        data.addEarnedRank(t.getRankReward());
                        rankId = t.getRankReward().getRankId();
                        granted = true;
                        break;
                    }
                }
            }
            if (!granted && !data.hasEarnedRank(rankId)) {
                String failId = rankId;
                ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"fantasticpass.msg.rank_not_owned", (Object[])new Object[]{failId}));
                return 0;
            }
        }
        String finalId = rankId;
        data.setDisplayedRankId(finalId);
        NametagSync.syncPlayer(player);
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"fantasticpass.msg.rank_set", (Object[])new Object[]{finalId}), false);
        return 1;
    }
}

