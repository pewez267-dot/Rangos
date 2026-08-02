package com.fshop.shop;

import com.fshop.data.FShopSavedData;
import net.minecraft.server.level.ServerPlayer;

/** Small helper to check whether a player is currently inside a market zone. */
public final class Gate {
   private Gate() {
   }

   public static boolean inMarket(ServerPlayer player) {
      return FShopSavedData.get(player.serverLevel()).isInsideAnyZone(player);
   }
}
