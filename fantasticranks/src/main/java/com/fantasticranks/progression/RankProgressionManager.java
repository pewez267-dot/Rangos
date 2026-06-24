package com.fantasticranks.progression;

import com.fantasticranks.afk.AfkTracker;
import com.fantasticranks.capability.RanksCapability;
import com.fantasticranks.config.RanksConfig;
import com.fantasticranks.data.PlayerRanksData;
import com.fantasticranks.data.RankDefinition;
import com.fantasticranks.data.RanksPackage;
import com.fantasticranks.data.RanksSavedData;
import com.fantasticranks.network.NametagSync;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Awards active play time and ranks players up automatically. Runs once per real second
 * (every 20 ticks) on the server thread. A rank-up requires no player action: the next
 * rank's {@code hoursRequired} threshold being met triggers it.
 */
public final class RankProgressionManager {

    private final AfkTracker afkTracker;
    private int tickCounter;

    public RankProgressionManager(AfkTracker afkTracker) {
        this.afkTracker = afkTracker;
    }

    public void serverTick(MinecraftServer server) {
        if (++tickCounter < 20) {
            return;
        }
        tickCounter = 0;

        RanksPackage pkg = RanksSavedData.get(server).getActivePackage();
        if (pkg == null || pkg.size() == 0) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerRanksData data = RanksCapability.getData(player);
            if (data == null) {
                continue;
            }

            // Align with the active package; a changed package resets seasonal progress.
            if (!pkg.getId().equals(data.getActivePackageId())) {
                data.resetProgress(pkg.getId());
                NametagSync.syncPlayer(player);
            }

            if (!afkTracker.isActive(player)) {
                continue;
            }

            int gainedMinutes = data.addActiveSeconds(1);
            if (gainedMinutes <= 0) {
                continue;
            }

            if (tryRankUp(player, data, pkg)) {
                NametagSync.syncPlayer(player);
            }
        }
    }

    private boolean tryRankUp(ServerPlayer player, PlayerRanksData data, RanksPackage pkg) {
        boolean rankedUp = false;
        double hours = data.getHoursActive();

        while (data.getCurrentRankIndex() + 1 < pkg.size()) {
            RankDefinition next = pkg.get(data.getCurrentRankIndex() + 1);
            if (next == null) {
                break;
            }
            if (hours + 1.0E-9D >= next.getHoursRequired()) {
                data.setCurrentRankIndex(data.getCurrentRankIndex() + 1);
                rankedUp = true;
                String message = RanksConfig.RANK_UP_MESSAGE.get().replace("{rank}", next.getRankName());
                player.sendSystemMessage(Component.literal(message));
            } else {
                break;
            }
        }
        return rankedUp;
    }
}
