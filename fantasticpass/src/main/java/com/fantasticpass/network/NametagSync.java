package com.fantasticpass.network;

import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.data.PassRankReward;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.interop.FantasticRanksInterop;
import com.fantasticpass.nametag.NametagData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side helper that computes a player's nametag state and broadcasts it to all
 * clients that can see the player (plus the player themselves) so the extra rank line
 * updates in real time without a reconnect.
 */
public final class NametagSync {

    private NametagSync() {
    }

    public static NametagData compute(ServerPlayer player) {
        PlayerPassData data = PassCapability.getData(player);
        int level = data != null ? data.getCurrentTier() : 0;

        // 1) Explicitly displayed pass rank (if the player still owns it).
        if (data != null && data.getDisplayedRankId() != null) {
            PassRankReward reward = data.getEarnedRank(data.getDisplayedRankId());
            if (reward != null) {
                return new NametagData(level, true, true,
                        reward.getRankDisplayText(), reward.getStyle(), "");
            }
        }

        // 2) Fall back to Fantastic Ranks if installed and it has a rank for the player.
        String fantasticRank = FantasticRanksInterop.getFormattedRank(player);
        if (fantasticRank != null) {
            return new NametagData(level, true, false, "", new NametagStyle(), fantasticRank);
        }

        // 3) Nothing to show: no extra line.
        return new NametagData(level, false, true, "", new NametagStyle(), "");
    }

    public static void syncPlayer(ServerPlayer player) {
        NametagData data = compute(player);
        PacketHandler.sendToTrackingAndSelf(player, new NametagUpdatePacket(player.getUUID(), data));
    }
}
