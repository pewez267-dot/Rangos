package com.fantasticpass.progression;

import com.fantasticpass.afk.AfkTracker;
import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.config.PassConfig;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.network.NametagSync;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TierProgressionManager {
   private final AfkTracker afkTracker;
   private int tickCounter;

   public TierProgressionManager(AfkTracker afkTracker) {
      this.afkTracker = afkTracker;
   }

   public void serverTick(MinecraftServer server) {
      if (++this.tickCounter >= 20) {
         this.tickCounter = 0;
         PassSavedData saved = PassSavedData.get(server);
         PassDefinition pass = saved.getActivePass();
         if (pass != null) {
            int minutesPerTier = pass.getMinutesPerTierOverride() > 0 ? pass.getMinutesPerTierOverride() : (Integer)PassConfig.MINUTES_PER_TIER.get();
            if (minutesPerTier <= 0) {
               minutesPerTier = 60;
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
               PlayerPassData data = PassCapability.getData(player);
               if (data != null) {
                  if (!pass.getId().equals(data.getActivePassId())) {
                     data.resetForNewSeason(pass.getId());
                     NametagSync.syncPlayer(player);
                  }

                  if (this.afkTracker.isActive(player)) {
                     int gainedMinutes = data.addActiveSeconds(1);
                     if (gainedMinutes > 0) {
                        int newTier = Math.min(100, data.getMinutesActive() / minutesPerTier);
                        if (newTier > data.getCurrentTier()) {
                           data.setCurrentTier(newTier);
                           NametagSync.syncPlayer(player);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
