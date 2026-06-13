package com.theplumteam.data.fabric;

import net.minecraft.class_1657;
import net.minecraft.class_2487;

public class PlayerDataManagerImpl {
   public static class_2487 getPersistentData(class_1657 player) {
      return StateSaverAndLoader.getPlayerState(player);
   }
}
