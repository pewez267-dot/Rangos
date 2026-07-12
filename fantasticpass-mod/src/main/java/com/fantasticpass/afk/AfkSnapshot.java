package com.fantasticpass.afk;

import net.minecraft.core.BlockPos;

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
      return this.position;
   }

   public float yRot() {
      return this.yRot;
   }

   public float xRot() {
      return this.xRot;
   }

   public long lastInteractionTick() {
      return this.lastInteractionTick;
   }

   public void updateBaseline(BlockPos position, float yRot, float xRot, long tick) {
      this.position = position;
      this.yRot = yRot;
      this.xRot = xRot;
      this.lastInteractionTick = tick;
   }

   public void markInteraction(long tick) {
      this.lastInteractionTick = tick;
   }
}
