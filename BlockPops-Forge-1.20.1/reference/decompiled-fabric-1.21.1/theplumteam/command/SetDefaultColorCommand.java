package com.theplumteam.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2172;
import net.minecraft.class_2561;

public class SetDefaultColorCommand {
   private static final SuggestionProvider<class_2168> COLOR_SUGGESTIONS = (context, builder) -> class_2172.method_9264(
         Stream.of(PopBlockColor.values()).map(PopBlockColor::method_15434), builder
      );

   public static void register(CommandDispatcher<class_2168> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)class_2170.method_9247("blockpops")
            .then(
               ((LiteralArgumentBuilder)class_2170.method_9247("setdefaultcolor").requires(source -> source.method_9259(2)))
                  .then(class_2170.method_9244("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS).executes(SetDefaultColorCommand::executeCommand))
            )
      );
   }

   private static int executeCommand(CommandContext<class_2168> context) {
      String colorName = StringArgumentType.getString(context, "color");
      class_2168 source = (class_2168)context.getSource();

      try {
         PopBlockColor color = PopBlockColor.valueOf(colorName.toUpperCase());
         ServerConfig.getInstance().setDefaultPlayerColor(color);
         BlockPopsMod.logDebug("Default player color set to: {}", color.method_15434());
         if (source.method_9211() != null) {
            FigureCollection updatedCollection = PlayerCollectionHelper.generate(source.method_9211());
            CollectionRegistry.registerDynamicCollection(updatedCollection);
            List<FigureCollection> dynamicCollections = new ArrayList<>();
            dynamicCollections.add(updatedCollection);
            SyncDynamicCollectionsPacket.sendToAllPlayers(source.method_9211(), dynamicCollections);
            BlockPopsMod.logDebug("Synced updated World Players collection to all players");
         }

         source.method_9226(() -> class_2561.method_43470("Set default player collection color to: " + color.method_15434()), true);
         return 1;
      } catch (IllegalArgumentException var6) {
         source.method_9213(
            class_2561.method_43470(
               "Invalid color name: "
                  + colorName
                  + ". Valid colors: original, black, blue, brown, cyan, gray, green, light_blue, light_gray, lime, magenta, orange, pink, purple, red, yellow"
            )
         );
         return 0;
      }
   }
}
