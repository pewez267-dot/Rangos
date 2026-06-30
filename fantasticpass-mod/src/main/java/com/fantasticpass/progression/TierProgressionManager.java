package com.fantasticpass.progression;

import com.fantasticpass.afk.AfkTracker;
import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.config.PassConfig;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.network.NametagSync;
import com.fantasticpass.quest.QuestManager;
import com.fantasticpass.quest.QuestType;
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

         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerPassData data = PassCapability.getData(player);
            if (data == null) {
               continue;
            }

            if (pass != null && !pass.getId().equals(data.getActivePassId())) {
               data.resetForNewSeason(pass.getId());
               NametagSync.syncPlayer(player);
            }

            // Keep the daily quest set fresh, but NEVER award points for time
            // spent online (no AFK / passive XP). All progress comes from quests.
            QuestManager.ensureDaily(player.getUUID(), data);
         }
      }
   }
}
