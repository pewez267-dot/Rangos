package com.fantasticranks.capability;

import com.fantasticranks.data.PlayerRanksData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Attaches a single {@link PlayerRanksData} instance to a player and serializes it to NBT.
 */
public final class RanksCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

    private final PlayerRanksData data = new PlayerRanksData();
    private final LazyOptional<PlayerRanksData> optional = LazyOptional.of(() -> data);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == RanksCapability.RANKS_DATA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.toNbt();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.fromNbt(nbt);
    }
}
