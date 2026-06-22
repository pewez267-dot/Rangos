package com.fantasticpass.capability;

import com.fantasticpass.data.PlayerPassData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Attaches a single {@link PlayerPassData} instance to a player and serializes it to NBT.
 */
public final class PassCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

    private final PlayerPassData data = new PlayerPassData();
    private final LazyOptional<PlayerPassData> optional = LazyOptional.of(() -> data);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == PassCapability.PASS_DATA) {
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
