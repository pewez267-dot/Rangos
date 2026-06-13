package com.theplumteam.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.block.PopBlockColor;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.figure.PlayerCollectionHelper;
import com.theplumteam.network.SyncDynamicCollectionsPacket;
import com.theplumteam.server.config.ServerConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class SetDefaultColorCommand {
   private static final SuggestionProvider<CommandSourceStack> COLOR_SUGGESTIONS = (context, builder) -> SharedSuggestionProvider.suggest(
         Stream.of(PopBlockColor.values()).map(PopBlockColor::getSerializedName), builder
      );

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         Commands.literal("blockpops")
            .then(
               Commands.literal("setdefaultcolor").requires(source -> source.hasPermission(2))
                  .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS).executes(SetDefaultColorCommand::executeCommand))
            )
      );
   }

   private static int executeCommand(CommandContext<CommandSourceStack> context) {
      String colorName = StringArgumentType.getString(context, "color");
      CommandSourceStack source = context.getSource();

      try {
         PopBlockColor color = PopBlockColor.valueOf(colorName.toUpperCase());
         ServerConfig.getInstance().setDefaultPlayerColor(color);
         BlockPopsMod.logDebug("Default player color set to: {}", color.getSerializedName());
         if (source.getServer() != null) {
            FigureCollection updatedCollection = PlayerCollectionHelper.generate(source.getServer());
            CollectionRegistry.registerDynamicCollection(updatedCollection);
            List<FigureCollection> dynamicCollections = new ArrayList<>();
            dynamicCollections.add(updatedCollection);
            SyncDynamicCollectionsPacket.sendToAllPlayers(source.getServer(), dynamicCollections);
            BlockPopsMod.logDebug("Synced updated World Players collection to all players");
         }

         source.sendSuccess(() -> Component.literal("Set default player collection color to: " + color.getSerializedName()), true);
         return 1;
      } catch (IllegalArgumentException var6) {
         source.sendFailure(
            Component.literal(
               "Invalid color name: "
                  + colorName
                  + ". Valid colors: original, black, blue, brown, cyan, gray, green, light_blue, light_gray, lime, magenta, orange, pink, purple, red, yellow"
            )
         );
         return 0;
      }
   }
}
