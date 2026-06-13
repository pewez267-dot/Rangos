package com.theplumteam.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.block.PopBlockColor;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetFavoriteColorCommand {
   private static final Logger LOGGER = LoggerFactory.getLogger(GetFavoriteColorCommand.class);

   public static void register(CommandDispatcher<class_2168> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)class_2170.method_9247("blockpops")
            .then(class_2170.method_9247("getfavoritecolor").executes(GetFavoriteColorCommand::executeCommand))
      );
   }

   private static int executeCommand(CommandContext<class_2168> context) {
      class_2168 source = (class_2168)context.getSource();

      try {
         class_3222 player = source.method_9207();
         IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
         if (!discovery.hasChosenFavoriteColor()) {
            source.method_9226(
               () -> class_2561.method_43470("You have not chosen a favorite color yet. Use /blockpops changefavoritecolor to choose one."), false
            );
            BlockPopsMod.logDebug("Player {} checked their favorite color but hasn't chosen one yet", player.method_5477().getString());
         } else {
            PopBlockColor favoriteColor = discovery.getFavoriteColor();
            if (favoriteColor != null) {
               String colorName = formatColorName(favoriteColor.method_15434());
               source.method_9226(() -> class_2561.method_43470("Your favorite color is: " + colorName), false);
               BlockPopsMod.logDebug("Player {} checked their favorite color: {}", player.method_5477().getString(), favoriteColor.method_15434());
            } else {
               source.method_9213(
                  class_2561.method_43470("Error: Favorite color data is corrupted. Please choose a new color with /blockpops changefavoritecolor")
               );
               LOGGER.warn("Player {} has chosen a favorite color but the data is null", player.method_5477().getString());
            }
         }

         return 1;
      } catch (Exception var6) {
         source.method_9213(class_2561.method_43470("This command can only be executed by a player"));
         LOGGER.error("Error executing getfavoritecolor command", var6);
         return 0;
      }
   }

   private static String formatColorName(String colorName) {
      String[] words = colorName.split("_");
      StringBuilder formatted = new StringBuilder();

      for (int i = 0; i < words.length; i++) {
         if (i > 0) {
            formatted.append(" ");
         }

         formatted.append(words[i].substring(0, 1).toUpperCase());
         formatted.append(words[i].substring(1));
      }

      return formatted.toString();
   }
}
