package com.fantasticpass.capability;

import com.fantasticpass.data.PlayerPassData;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public final class PassCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
   private final PlayerPassData data = new PlayerPassData();
   private final LazyOptional<PlayerPassData> optional = LazyOptional.of(() -> this.data);

   @Nonnull
   public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
      return cap == PassCapability.PASS_DATA ? this.optional.cast() : LazyOptional.empty();
   }

   public void invalidate() {
      this.optional.invalidate();
   }

   public CompoundTag serializeNBT() {
      return this.data.toNbt();
   }

   public void deserializeNBT(CompoundTag nbt) {
      this.data.fromNbt(nbt);
   }
}
