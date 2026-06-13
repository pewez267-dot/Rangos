package com.theplumteam.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import com.theplumteam.network.SyncTokenDataPacket;
import com.theplumteam.server.ServerTickHandler;
import com.theplumteam.server.config.ServerConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReloadRegularTokensCommand {
   private static final Logger LOGGER = LoggerFactory.getLogger(ReloadRegularTokensCommand.class);

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         Commands.literal("blockpops")
            .then(Commands.literal("reloadregular").requires(source -> source.hasPermission(2)).executes(ReloadRegularTokensCommand::executeCommand))
      );
   }

   private static int executeCommand(CommandContext<CommandSourceStack> context) {
      CommandSourceStack source = context.getSource();

      try {
         ServerPlayer player = source.getPlayerOrException();
         IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
         int maxTokens = ServerConfig.getInstance().getMaxRegularTokens();
         discovery.setRegularTokens(maxTokens);
         discovery.setNextRegularTokenTime(0L);
         PlayerDataManager.markDirty(player, discovery);
         BlockPopsMod.logDebug("Reloaded regular tokens for player {}", player.getName().getString());
         long gameTime = player.serverLevel().getGameTime();
         long nextRegularTime = discovery.getNextRegularTokenTime();
         long ticksUntilNext = Math.max(0L, nextRegularTime - gameTime);
         long millisUntilReset = ServerTickHandler.calculateMillisUntilNextReset();
         SyncTokenDataPacket.sendToPlayer(player, discovery.getRegularTokens(), ticksUntilNext, !discovery.hasUsedTodaySpecialToken(), millisUntilReset);
         source.sendSuccess(() -> Component.literal("Reloaded regular tokens to " + maxTokens), true);
         return 1;
      } catch (Exception var13) {
         source.sendFailure(Component.literal("This command can only be executed by a player"));
         LOGGER.error("Error executing reloadregular command", var13);
         return 0;
      }
   }
}
