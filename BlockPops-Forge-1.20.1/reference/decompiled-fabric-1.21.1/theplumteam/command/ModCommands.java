package com.theplumteam.command;

import com.mojang.brigadier.CommandDispatcher;
import com.theplumteam.BlockPopsMod;
import net.minecraft.class_2168;
import net.minecraft.class_7157;
import net.minecraft.class_2170.class_5364;

public class ModCommands {
   public static void register(CommandDispatcher<class_2168> dispatcher, class_7157 registryAccess, class_5364 environment) {
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
