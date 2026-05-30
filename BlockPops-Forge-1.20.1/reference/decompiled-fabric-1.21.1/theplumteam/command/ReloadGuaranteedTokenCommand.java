package com.theplumteam.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import com.theplumteam.network.SyncTokenDataPacket;
import com.theplumteam.server.ServerTickHandler;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReloadGuaranteedTokenCommand {
   private static final Logger LOGGER = LoggerFactory.getLogger(ReloadGuaranteedTokenCommand.class);

   public static void register(CommandDispatcher<class_2168> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)class_2170.method_9247("blockpops")
            .then(
               ((LiteralArgumentBuilder)class_2170.method_9247("reloadguaranteed").requires(source -> source.method_9259(2)))
                  .executes(ReloadGuaranteedTokenCommand::executeCommand)
            )
      );
   }

   private static int executeCommand(CommandContext<class_2168> context) {
      class_2168 source = (class_2168)context.getSource();

      try {
         class_3222 player = source.method_9207();
         IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
         discovery.setUsedTodaySpecialToken(false);
         PlayerDataManager.markDirty(player, discovery);
         BlockPopsMod.logDebug("Reloaded guaranteed token for player {}", player.method_5477().getString());
         long gameTime = player.method_51469().method_8510();
         long nextRegularTime = discovery.getNextRegularTokenTime();
         long ticksUntilNext = Math.max(0L, nextRegularTime - gameTime);
         long millisUntilReset = ServerTickHandler.calculateMillisUntilNextReset();
         SyncTokenDataPacket.sendToPlayer(player, discovery.getRegularTokens(), ticksUntilNext, !discovery.hasUsedTodaySpecialToken(), millisUntilReset);
         source.method_9226(() -> class_2561.method_43470("Reloaded guaranteed token"), true);
         return 1;
      } catch (Exception var12) {
         source.method_9213(class_2561.method_43470("This command can only be executed by a player"));
         LOGGER.error("Error executing reloadguaranteed command", var12);
         return 0;
      }
   }
}
