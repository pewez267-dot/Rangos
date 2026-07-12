package com.fantasticpass.commands;

import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.config.PassConfig;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.network.NametagSync;
import com.fantasticpass.network.OpenAdminScreenPacket;
import com.fantasticpass.network.OpenViewScreenPacket;
import com.fantasticpass.quest.DefaultQuests;
import com.fantasticpass.quest.QuestManager;
import com.fantasticpass.network.PacketHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class FsPassCommand {
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_PASS_IDS = (ctx, builder) -> {
      MinecraftServer server = ((CommandSourceStack)ctx.getSource()).getServer();
      return server != null ? SharedSuggestionProvider.suggest(PassSavedData.get(server).getPasses().keySet(), builder) : builder.buildFuture();
   };
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_EARNED_RANKS = (ctx, builder) -> {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         PlayerPassData data = PassCapability.getData(player);
         if (data != null) {
            return SharedSuggestionProvider.suggest(data.getEarnedRankIds(), builder);
         }
      } catch (CommandSyntaxException var4) {
      }

      return builder.buildFuture();
   };

   private FsPassCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                 "fspass"
                              )
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("create").requires(source -> source.hasPermission(4)))
                                    .executes(FsPassCommand::create)
                              ))
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("edit").requires(source -> source.hasPermission(4)))
                                 .then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_PASS_IDS).executes(FsPassCommand::edit))
                           ))
                        .then(
                           ((LiteralArgumentBuilder)Commands.literal("delete").requires(source -> source.hasPermission(4)))
                              .then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_PASS_IDS).executes(FsPassCommand::delete))
                        ))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("setpremium").requires(source -> source.hasPermission(4)))
                           .then(Commands.argument("player", EntityArgument.player()).executes(FsPassCommand::setPremium))
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("activate").requires(source -> source.hasPermission(4)))
                        .then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_PASS_IDS).executes(FsPassCommand::activate))
                  ))
               .then(Commands.literal("view").executes(FsPassCommand::view)))
            .then(
               Commands.literal("rango")
                  .then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_EARNED_RANKS).executes(FsPassCommand::rango))
            )
            .then(
               ((LiteralArgumentBuilder)Commands.literal("test").requires(source -> source.hasPermission(4))).executes(FsPassCommand::test)
            )
            .then(
               ((LiteralArgumentBuilder)Commands.literal("reset").requires(source -> source.hasPermission(4)))
                  .executes(FsPassCommand::resetSelf)
                  .then(Commands.argument("player", EntityArgument.player()).executes(FsPassCommand::resetPlayer))
            )
            .then(
               ((LiteralArgumentBuilder)Commands.literal("week").requires(source -> source.hasPermission(4)))
                  .then(Commands.argument("number", IntegerArgumentType.integer(1, 52)).executes(FsPassCommand::setWeek))
            )
      );
   }

   /**
    * Admin testing command (toggle). Turning it ON unlocks the whole pass
    * (premium, all tiers and weeks, fresh dailies) for the CURRENT SESSION only;
    * the real progress is snapshotted and is what persists, so nothing is saved.
    * Running it again restores the real progress immediately.
    */
   private static int test(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
      PassSavedData saved = PassSavedData.get(player.getServer());
      PassDefinition pass = saved.getActivePass();
      if (pass == null) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("fantasticpass.msg.no_active_pass"));
         return 0;
      }

      PlayerPassData data = PassCapability.getData(player);
      if (data == null) {
         return 0;
      }

      if (data.isTestMode()) {
         // Toggle OFF: restore the player's real progress for this session.
         data.exitTestMode();
         QuestManager.ensureDaily(player.getUUID(), data);
         NametagSync.syncPlayer(player);
         PacketHandler.sendToPlayer(player, new OpenViewScreenPacket(pass, data, QuestManager.pointsPerTier(pass)));
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("fantasticpass.msg.test_off"), false);
         return 1;
      }

      // Toggle ON: snapshot real progress, then unlock everything temporarily.
      data.enterTestMode();
      data.setPremium(true);
      data.setCurrentWeek(DefaultQuests.effectiveWeekCount(pass));
      data.setCurrentTier(pass.getTierCount());
      data.addPoints(pass.getTierCount() * QuestManager.pointsPerTier(pass) - data.getPoints());
      QuestManager.rerollDaily(player.getUUID(), data);
      NametagSync.syncPlayer(player);
      PacketHandler.sendToPlayer(player, new OpenViewScreenPacket(pass, data, QuestManager.pointsPerTier(pass)));
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("fantasticpass.msg.test_mode"), false);
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
      String id = StringArgumentType.getString(ctx, "id");
      PassSavedData saved = PassSavedData.get(player.getServer());
      PassDefinition pass = saved.getPass(id);
      if (pass == null) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("fantasticpass.msg.pass_not_found", new Object[]{id}));
         return 0;
      } else {
         PacketHandler.sendToPlayer(player, new OpenAdminScreenPacket(pass.copy()));
         return 1;
      }
   }

   /** Reset the caller's own pass progress back to zero. */
   private static int resetSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      return reset(ctx, ((CommandSourceStack)ctx.getSource()).getPlayerOrException());
   }

   /** Reset another player's pass progress back to zero. */
   private static int resetPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      return reset(ctx, EntityArgument.getPlayer(ctx, "player"));
   }

   private static int reset(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
      PassSavedData saved = PassSavedData.get(player.getServer());
      PassDefinition pass = saved.getActivePass();
      PlayerPassData data = PassCapability.getData(player);
      if (data == null) {
         return 0;
      }

      data.exitTestMode(); // drop any leftover test state so the wipe is real
      data.resetForNewSeason(saved.getActivePassId());
      QuestManager.ensureDaily(player.getUUID(), data);
      NametagSync.syncPlayer(player);
      if (pass != null) {
         PacketHandler.sendToPlayer(player, new OpenViewScreenPacket(pass, data, QuestManager.pointsPerTier(pass)));
      }

      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(() -> Component.translatable("fantasticpass.msg.reset", player.getGameProfile().getName()), true);
      return 1;
   }

   /** Manually set the caller's current (unlocked) week. */
   private static int setWeek(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
      int number = IntegerArgumentType.getInteger(ctx, "number");
      PassSavedData saved = PassSavedData.get(player.getServer());
      PassDefinition pass = saved.getActivePass();
      PlayerPassData data = PassCapability.getData(player);
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
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("fantasticpass.msg.week_set", finalWeek), false);
      return 1;
   }

   private static int delete(CommandContext<CommandSourceStack> ctx) {
      MinecraftServer server = ((CommandSourceStack)ctx.getSource()).getServer();
      String id = StringArgumentType.getString(ctx, "id");
      PassSavedData saved = PassSavedData.get(server);
      if (saved.deletePass(id)) {
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("fantasticpass.msg.pass_deleted", new Object[]{id}), true);
         return 1;
      } else {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("fantasticpass.msg.pass_not_found", new Object[]{id}));
         return 0;
      }
   }

   private static int setPremium(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
      PlayerPassData data = PassCapability.getData(target);
      if (data == null) {
         return 0;
      } else {
         data.setPremium(true);
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.translatable("fantasticpass.msg.premium_set", new Object[]{target.getGameProfile().getName()}), true);
         return 1;
      }
   }

   private static int activate(CommandContext<CommandSourceStack> ctx) {
      MinecraftServer server = ((CommandSourceStack)ctx.getSource()).getServer();
      String id = StringArgumentType.getString(ctx, "id");
      PassSavedData saved = PassSavedData.get(server);
      if (!saved.hasPass(id)) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("fantasticpass.msg.pass_not_found", new Object[]{id}));
         return 0;
      } else {
         saved.setActivePassId(id);

         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerPassData data = PassCapability.getData(player);
            if (data != null) {
               data.resetForNewSeason(id);
               NametagSync.syncPlayer(player);
            }
         }

         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("fantasticpass.msg.pass_activated", new Object[]{id}), true);
         return 1;
      }
   }

   private static int view(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
      PassSavedData saved = PassSavedData.get(player.getServer());
      PassDefinition pass = saved.getActivePass();
      if (pass == null) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("fantasticpass.msg.no_active_pass"));
         return 0;
      } else {
         PlayerPassData data = PassCapability.getData(player);
         if (data == null) {
            return 0;
         } else {
            QuestManager.ensureDaily(player.getUUID(), data);
            PacketHandler.sendToPlayer(player, new OpenViewScreenPacket(pass, data, QuestManager.pointsPerTier(pass)));
            return 1;
         }
      }
   }

   private static int rango(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
      String id = StringArgumentType.getString(ctx, "id");
      PlayerPassData data = PassCapability.getData(player);
      if (data == null) {
         return 0;
      } else if (id.equalsIgnoreCase("none") || id.equalsIgnoreCase("clear")) {
         data.setDisplayedRankId(null);
         NametagSync.syncPlayer(player);
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("fantasticpass.msg.rank_set", new Object[]{"none"}), false);
         return 1;
      } else if (!data.hasEarnedRank(id)) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("fantasticpass.msg.rank_not_owned", new Object[]{id}));
         return 0;
      } else {
         data.setDisplayedRankId(id);
         NametagSync.syncPlayer(player);
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("fantasticpass.msg.rank_set", new Object[]{id}), false);
         return 1;
      }
   }
}
