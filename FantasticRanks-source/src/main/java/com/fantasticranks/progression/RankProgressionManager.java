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
                data.wipeRank(pkg != null ? pkg.getId() : "");
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
        double hours = data.getHoursActive();
        int start = data.getCurrentRankIndex();
        // Indice objetivo segun horas: el rango mas alto cuyo requisito de horas se cumple.
        int target = start;
        while (target + 1 < pkg.size()) {
            RankDefinition next = pkg.get(target + 1);
            if (next == null || hours + 1.0E-9 < next.getHoursRequired()) {
                break;
            }
            ++target;
        }
        // Jugador wipeado (start < 0): permanece SIN rango hasta ganar un rango REAL (indice >= 1);
        // no se le vuelve a poner el rango base automaticamente. Si solo alcanza el base (0), sigue sin rango.
        if (start < 0 && target <= 0) {
            return false;
        }
        if (target <= start) {
            return false;
        }
        int from = start < 0 ? 1 : start + 1;
        for (int i = from; i <= target; ++i) {
            RankDefinition r = pkg.get(i);
            if (r != null) {
                String message = ((String)RanksConfig.RANK_UP_MESSAGE.get()).replace("{rank}", r.getRankName());
                player.sendSystemMessage((Component)Component.literal((String)message));
            }
        }
        data.setCurrentRankIndex(target);
        return true;
    }
}

