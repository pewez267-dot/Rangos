package com.theplumteam.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.theplumteam.server.config.ServerConfig;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2561;

public class ShowColorSelectionOnJoinCommand {
   public static void register(CommandDispatcher<class_2168> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)class_2170.method_9247("blockpops")
            .then(
               ((LiteralArgumentBuilder)class_2170.method_9247("showcolorselectiononjoin").requires(source -> source.method_9259(2)))
                  .then(class_2170.method_9244("value", BoolArgumentType.bool()).executes(ShowColorSelectionOnJoinCommand::executeCommand))
            )
      );
   }

   private static int executeCommand(CommandContext<class_2168> context) {
      boolean value = BoolArgumentType.getBool(context, "value");
      class_2168 source = (class_2168)context.getSource();
      ServerConfig.getInstance().setShowColorSelectionOnJoin(value);
      source.method_9226(() -> class_2561.method_43470("Show color selection on join set to: " + value), true);
      return 1;
   }
}
