package com.theplumteam.figure;

import com.theplumteam.figure.fabric.PlayerCollectionHelperImpl;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.injectables.annotations.ExpectPlatform.Transformed;
import net.minecraft.server.MinecraftServer;

public class PlayerCollectionHelper {
   public static final String WORLD_PLAYERS_COLLECTION_ID = "world_players";

   @ExpectPlatform
   @Transformed
   public static FigureCollection generate(MinecraftServer server) {
      return PlayerCollectionHelperImpl.generate(server);
   }

   @ExpectPlatform
   @Transformed
   public static void regenerateAndSyncPlayerCollection(MinecraftServer server) {
      PlayerCollectionHelperImpl.regenerateAndSyncPlayerCollection(server);
   }
}
