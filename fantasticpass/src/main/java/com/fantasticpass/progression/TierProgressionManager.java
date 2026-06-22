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

/**
 * Awards active play time and unlocks tiers. Runs once per real second (every 20
 * ticks) on the server thread. Rewards are <em>not</em> granted here — tiers only
 * unlock; the player claims rewards from the GUI ({@link RewardDispatcher}).
 */
public final class TierProgressionManager {

    private final AfkTracker afkTracker;
    private int tickCounter;

    public TierProgressionManager(AfkTracker afkTracker) {
        this.afkTracker = afkTracker;
    }

    public void serverTick(MinecraftServer server) {
        if (++tickCounter < 20) {
            return;
        }
        tickCounter = 0;

        PassSavedData saved = PassSavedData.get(server);
        PassDefinition pass = saved.getActivePass();
        if (pass == null) {
            return;
        }

        int minutesPerTier = pass.getMinutesPerTierOverride() > 0
                ? pass.getMinutesPerTierOverride()
                : PassConfig.MINUTES_PER_TIER.get();
        if (minutesPerTier <= 0) {
            minutesPerTier = 60;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerPassData data = PassCapability.getData(player);
            if (data == null) {
                continue;
            }

            // Align the player's record with the active pass. If the active pass changed
            // while they were elsewhere, their seasonal progress resets (ranks preserved).
            if (!pass.getId().equals(data.getActivePassId())) {
                data.resetForNewSeason(pass.getId());
                NametagSync.syncPlayer(player);
            }

            if (!afkTracker.isActive(player)) {
                continue;
            }

            int gainedMinutes = data.addActiveSeconds(1);
            if (gainedMinutes > 0) {
                int newTier = Math.min(PassDefinition.TIER_COUNT, data.getMinutesActive() / minutesPerTier);
                if (newTier > data.getCurrentTier()) {
                    data.setCurrentTier(newTier);
                    NametagSync.syncPlayer(player);
                }
            }
        }
    }
}
