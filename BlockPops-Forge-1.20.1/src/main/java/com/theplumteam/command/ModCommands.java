package com.theplumteam.command;

import com.mojang.brigadier.CommandDispatcher;
import com.theplumteam.BlockPopsMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandBuildContext;

public class ModCommands {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
      BlockPopsMod.logDebug("Registering BlockPops commands...");
      GetFavoriteColorCommand.register(dispatcher);
      ReloadRegularTokensCommand.register(dispatcher);
      ReloadGuaranteedTokenCommand.register(dispatcher);
      ChangeFavoriteColorCommand.register(dispatcher);
      GetBoxCommand.register(dispatcher);
      SetDefaultColorCommand.register(dispatcher);
      ShowColorSelectionOnJoinCommand.register(dispatcher);
      BlockPopsMod.logDebug("BlockPops commands registered");
   }
}
