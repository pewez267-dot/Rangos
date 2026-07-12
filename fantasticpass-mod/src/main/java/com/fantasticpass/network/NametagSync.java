package com.fantasticpass.network;

import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.data.PassRankReward;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.interop.FantasticRanksInterop;
import com.fantasticpass.nametag.NametagData;
import net.minecraft.server.level.ServerPlayer;

public final class NametagSync {
   private NametagSync() {
   }

   public static NametagData compute(ServerPlayer player) {
      PlayerPassData data = PassCapability.getData(player);
      int level = data != null ? data.getCurrentTier() : 0;
      if (data != null && data.getDisplayedRankId() != null) {
         PassRankReward reward = data.getEarnedRank(data.getDisplayedRankId());
         if (reward != null) {
            return new NametagData(level, true, true, reward.getRankDisplayText(), reward.getStyle(), "");
         }
      }

      String fantasticRank = FantasticRanksInterop.getFormattedRank(player);
      return fantasticRank != null
         ? new NametagData(level, true, false, "", new NametagStyle(), fantasticRank)
         : new NametagData(level, false, true, "", new NametagStyle(), "");
   }

   public static void syncPlayer(ServerPlayer player) {
      NametagData data = compute(player);
      PacketHandler.sendToTrackingAndSelf(player, new NametagUpdatePacket(player.getUUID(), data));
   }
}
