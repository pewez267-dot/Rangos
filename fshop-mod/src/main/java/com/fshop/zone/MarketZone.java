package com.fshop.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A persisted market zone: an axis-aligned cuboid in a specific dimension.
 * Players standing inside any zone gain access to the /fshop commands.
 */
public final class MarketZone {
   private final String name;
   private final ResourceKey<Level> dimension;
   private final BlockPos min;
   private final BlockPos max;

   public MarketZone(String name, ResourceKey<Level> dimension, BlockPos min, BlockPos max) {
      this.name = name;
      this.dimension = dimension;
      this.min = min.immutable();
      this.max = max.immutable();
   }

   public String getName() {
      return this.name;
   }

   public ResourceKey<Level> getDimension() {
      return this.dimension;
   }

   public BlockPos getMin() {
      return this.min;
   }

   public BlockPos getMax() {
      return this.max;
   }

   public boolean contains(ResourceKey<Level> dim, double x, double y, double z) {
      if (!this.dimension.equals(dim)) {
         return false;
      }
      return x >= this.min.getX() && x <= this.max.getX() + 1
            && y >= this.min.getY() && y <= this.max.getY() + 1
            && z >= this.min.getZ() && z <= this.max.getZ() + 1;
   }

   public long volume() {
      long dx = (long) (max.getX() - min.getX()) + 1L;
      long dy = (long) (max.getY() - min.getY()) + 1L;
      long dz = (long) (max.getZ() - min.getZ()) + 1L;
      return dx * dy * dz;
   }

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putString("name", this.name);
      tag.putString("dim", this.dimension.location().toString());
      tag.putLong("min", this.min.asLong());
      tag.putLong("max", this.max.asLong());
      return tag;
   }

   public static MarketZone fromNbt(CompoundTag tag) {
      ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
            new ResourceLocation(tag.getString("dim")));
      return new MarketZone(
            tag.getString("name"),
            dim,
            BlockPos.of(tag.getLong("min")),
            BlockPos.of(tag.getLong("max")));
   }
}
