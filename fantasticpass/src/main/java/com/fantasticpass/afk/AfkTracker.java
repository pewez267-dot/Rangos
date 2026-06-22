package com.fantasticpass.afk;

import com.fantasticpass.config.PassConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Self-contained anti-AFK detector. Samples each online player every
 * {@code check_interval_ticks} for changes in block position, horizontal rotation,
 * and vertical rotation; explicit interactions also count as activity. A player is
 * considered active while the time since their last activity is below the configured
 * threshold.
 *
 * <p>All state is confined to the server thread (server tick + Forge gameplay events).
 */
public final class AfkTracker {

    private final Map<UUID, AfkSnapshot> snapshots = new HashMap<>();
    private long serverTick;

    /** Advances the tracker. Must be called once per server tick (END phase). */
    public void serverTick(MinecraftServer server) {
        serverTick++;

        int interval = Math.max(1, PassConfig.CHECK_INTERVAL_TICKS.get());
        if (serverTick % interval != 0L) {
            return;
        }

        double minRotation = PassConfig.MIN_ROTATION_CHANGE_DEGREES.get();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sample(player, minRotation);
        }
    }

    private void sample(ServerPlayer player, double minRotation) {
        UUID id = player.getUUID();
        BlockPos pos = player.blockPosition();
        float yRot = player.getYRot();
        float xRot = player.getXRot();

        AfkSnapshot snapshot = snapshots.get(id);
        if (snapshot == null) {
            snapshots.put(id, new AfkSnapshot(pos, yRot, xRot, serverTick));
            return;
        }

        boolean moved = !snapshot.position().equals(pos);
        boolean rotatedHorizontal = angleDifference(snapshot.yRot(), yRot) >= minRotation;
        boolean rotatedVertical = angleDifference(snapshot.xRot(), xRot) >= minRotation;

        if (moved || rotatedHorizontal || rotatedVertical) {
            snapshot.updateBaseline(pos, yRot, xRot, serverTick);
        }
        // Otherwise leave the baseline intact so slow, sub-threshold drift eventually
        // accumulates past the threshold and is correctly counted as activity.
    }

    /** Registers an explicit interaction (click, attack, break, chat, command). */
    public void registerInteraction(ServerPlayer player) {
        UUID id = player.getUUID();
        AfkSnapshot snapshot = snapshots.get(id);
        if (snapshot == null) {
            snapshots.put(id, new AfkSnapshot(player.blockPosition(), player.getYRot(), player.getXRot(), serverTick));
        } else {
            snapshot.markInteraction(serverTick);
        }
    }

    /**
     * @return {@code true} if the player is currently active (not AFK).
     */
    public boolean isActive(ServerPlayer player) {
        AfkSnapshot snapshot = snapshots.get(player.getUUID());
        if (snapshot == null) {
            // First sample not taken yet: treat as active so progress is not unfairly withheld.
            return true;
        }
        long thresholdTicks = (long) Math.max(1, PassConfig.AFK_THRESHOLD_SECONDS.get()) * 20L;
        return (serverTick - snapshot.lastInteractionTick()) < thresholdTicks;
    }

    public void remove(UUID id) {
        snapshots.remove(id);
    }

    public void clear() {
        snapshots.clear();
    }

    private static double angleDifference(float a, float b) {
        return Math.abs(Mth.degreesDifference(a, b));
    }
}
