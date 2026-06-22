package com.fantasticranks.network;

import com.fantasticranks.capability.RanksCapability;
import com.fantasticranks.data.NametagStyle;
import com.fantasticranks.data.PlayerRanksData;
import com.fantasticranks.data.RankDefinition;
import com.fantasticranks.data.RanksPackage;
import com.fantasticranks.data.RanksSavedData;
import com.fantasticranks.nametag.NametagData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side helper that computes a player's rank nametag state and broadcasts it to all
 * clients that can see the player (plus the player themselves) so the line updates in real
 * time without a reconnect.
 */
public final class NametagSync {

    private NametagSync() {
    }

    public static NametagData compute(ServerPlayer player) {
        PlayerRanksData data = RanksCapability.getData(player);
        if (data == null) {
            return new NametagData(0, false, "", new NametagStyle());
        }

        // Admin test preview overrides the real rank without touching progress.
        if (data.isPreviewActive()) {
            return new NametagData(data.getPreviewRankNumber(), true,
                    data.getPreviewRankName(), data.getPreviewStyle());
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return new NametagData(0, false, "", new NametagStyle());
        }

        RanksPackage pkg = RanksSavedData.get(server).getActivePackage();
        if (pkg == null || pkg.size() == 0) {
            return new NametagData(0, false, "", new NametagStyle());
        }

        int index = Math.max(0, Math.min(pkg.size() - 1, data.getCurrentRankIndex()));
        RankDefinition rank = pkg.get(index);
        if (rank == null) {
            return new NametagData(0, false, "", new NametagStyle());
        }

        return new NametagData(rank.getRankNumber(), true, rank.getRankName(), rank.getStyle());
    }

    public static void syncPlayer(ServerPlayer player) {
        NametagData data = compute(player);
        PacketHandler.sendToTrackingAndSelf(player, new NametagUpdatePacket(player.getUUID(), data));
    }
}
