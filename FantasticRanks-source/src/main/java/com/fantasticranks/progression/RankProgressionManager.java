/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 */
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
import net.minecraft.world.entity.player.Player;

public final class RankProgressionManager {
    private final AfkTracker afkTracker;
    private int tickCounter;

    public RankProgressionManager(AfkTracker afkTracker) {
        this.afkTracker = afkTracker;
    }

    public void serverTick(MinecraftServer server) {
        if (++this.tickCounter < 20) {
            return;
        }
        this.tickCounter = 0;
        RanksSavedData saved = RanksSavedData.get(server);
        long wipeGen = saved.getWipeGeneration();
        RanksPackage pkg = saved.getActivePackage();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int gainedMinutes;
            PlayerRanksData data = RanksCapability.getData((Player)player);
            if (data == null) continue;
            // Wipe pendiente: limpia el progreso de quien no haya aplicado el ultimo wipe (incluye a
            // los que estaban offline cuando se ejecuto /fsranks wipe: se limpian al reconectar).
            if (data.getWipeSeen() < wipeGen) {
                data.resetProgress(pkg != null ? pkg.getId() : "");
                data.setWipeSeen(wipeGen);
                NametagSync.syncPlayer(player);
            }
            if (pkg == null || pkg.size() == 0) continue;
            if (!pkg.getId().equals(data.getActivePackageId())) {
                data.resetProgress(pkg.getId());
                NametagSync.syncPlayer(player);
            }
            if (!this.afkTracker.isActive(player) || (gainedMinutes = data.addActiveSeconds(1)) <= 0 || !this.tryRankUp(player, data, pkg)) continue;
            NametagSync.syncPlayer(player);
        }
    }

    private boolean tryRankUp(ServerPlayer player, PlayerRanksData data, RanksPackage pkg) {
        RankDefinition next;
        boolean rankedUp = false;
        double hours = data.getHoursActive();
        while (data.getCurrentRankIndex() + 1 < pkg.size() && (next = pkg.get(data.getCurrentRankIndex() + 1)) != null && hours + 1.0E-9 >= next.getHoursRequired()) {
            data.setCurrentRankIndex(data.getCurrentRankIndex() + 1);
            rankedUp = true;
            String message = ((String)RanksConfig.RANK_UP_MESSAGE.get()).replace("{rank}", next.getRankName());
            player.sendSystemMessage((Component)Component.literal((String)message));
        }
        return rankedUp;
    }
}

