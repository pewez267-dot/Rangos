package com.fshop.zone;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Transient per-player cuboid selection built with the market wand. Not
 * persisted; it only lives for the session while an admin marks out a zone.
 */
public final class PlayerSelection {
   @Nullable
   private ResourceKey<Level> dimension;
   @Nullable
   private BlockPos pos1;
   @Nullable
   private BlockPos pos2;

   public void setPos1(ResourceKey<Level> dim, BlockPos pos) {
      if (this.dimension != null && !this.dimension.equals(dim)) {
         // Switching dimension invalidates the other corner.
         this.pos2 = null;
      }
      this.dimension = dim;
      this.pos1 = pos.immutable();
   }

   public void setPos2(ResourceKey<Level> dim, BlockPos pos) {
      if (this.dimension != null && !this.dimension.equals(dim)) {
         this.pos1 = null;
      }
      this.dimension = dim;
      this.pos2 = pos.immutable();
   }

   @Nullable
   public ResourceKey<Level> getDimension() {
      return this.dimension;
   }

   @Nullable
   public BlockPos getPos1() {
      return this.pos1;
   }

   @Nullable
   public BlockPos getPos2() {
      return this.pos2;
   }

   public boolean isComplete() {
      return this.dimension != null && this.pos1 != null && this.pos2 != null;
   }

   public BlockPos min() {
      return new BlockPos(
            Math.min(this.pos1.getX(), this.pos2.getX()),
            Math.min(this.pos1.getY(), this.pos2.getY()),
            Math.min(this.pos1.getZ(), this.pos2.getZ()));
   }

   public BlockPos max() {
      return new BlockPos(
            Math.max(this.pos1.getX(), this.pos2.getX()),
            Math.max(this.pos1.getY(), this.pos2.getY()),
            Math.max(this.pos1.getZ(), this.pos2.getZ()));
   }

   public long volume() {
      if (!isComplete()) {
         return 0L;
      }
      BlockPos mn = min();
      BlockPos mx = max();
      long dx = (long) (mx.getX() - mn.getX()) + 1L;
      long dy = (long) (mx.getY() - mn.getY()) + 1L;
      long dz = (long) (mx.getZ() - mn.getZ()) + 1L;
      return dx * dy * dz;
   }

   public Component describeVolume() {
      if (isComplete()) {
         return Component.literal(" — volumen: " + volume() + " bloques").withStyle(ChatFormatting.GRAY);
      }
      return Component.literal(" — falta la otra esquina").withStyle(ChatFormatting.DARK_GRAY);
   }

   public void clear() {
      this.dimension = null;
      this.pos1 = null;
      this.pos2 = null;
   }
}
