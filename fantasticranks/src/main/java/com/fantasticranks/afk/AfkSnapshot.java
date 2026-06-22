package com.fantasticranks.afk;

import net.minecraft.core.BlockPos;

/**
 * Mutable per-player activity baseline used by {@link AfkTracker}. Only ever read and
 * written from the server thread.
 */
public final class AfkSnapshot {

    private BlockPos position;
    private float yRot;
    private float xRot;
    private long lastInteractionTick;

    public AfkSnapshot(BlockPos position, float yRot, float xRot, long lastInteractionTick) {
        this.position = position;
        this.yRot = yRot;
        this.xRot = xRot;
        this.lastInteractionTick = lastInteractionTick;
    }

    public BlockPos position() {
        return position;
    }

    public float yRot() {
        return yRot;
    }

    public float xRot() {
        return xRot;
    }

    public long lastInteractionTick() {
        return lastInteractionTick;
    }

    /** Updates the movement/rotation baseline and marks this tick as active. */
    public void updateBaseline(BlockPos position, float yRot, float xRot, long tick) {
        this.position = position;
        this.yRot = yRot;
        this.xRot = xRot;
        this.lastInteractionTick = tick;
    }

    /** Marks this tick as active without moving the movement/rotation baseline. */
    public void markInteraction(long tick) {
        this.lastInteractionTick = tick;
    }
}
