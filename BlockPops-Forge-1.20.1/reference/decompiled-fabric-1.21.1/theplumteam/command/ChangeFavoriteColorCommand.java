package com.theplumteam.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.network.OpenFavoriteColorScreenPacket;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeFavoriteColorCommand {
   private static final Logger LOGGER = LoggerFactory.getLogger(ChangeFavoriteColorCommand.class);

   public static void register(CommandDispatcher<class_2168> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)class_2170.method_9247("blockpops")
            .then(class_2170.method_9247("changefavoritecolor").executes(ChangeFavoriteColorCommand::executeCommand))
      );
   }

   private static int executeCommand(CommandContext<class_2168> context) {
      class_2168 source = (class_2168)context.getSource();

      try {
         class_3222 player = source.method_9207();
         OpenFavoriteColorScreenPacket.sendToPlayer(player);
         source.method_9226(() -> class_2561.method_43470("Opening favorite color selection..."), false);
         BlockPopsMod.logDebug("Player {} opened the favorite color selection screen", player.method_5477().getString());
         return 1;
      } catch (Exception var3) {
         source.method_9213(class_2561.method_43470("This command can only be executed by a player"));
         LOGGER.error("Error executing changefavoritecolor command", var3);
         return 0;
      }
   }
}
