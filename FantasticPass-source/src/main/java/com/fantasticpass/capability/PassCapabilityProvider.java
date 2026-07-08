package com.fantasticpass.capability;

import com.fantasticpass.data.PlayerPassData;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public final class PassCapabilityProvider
implements ICapabilitySerializable<CompoundTag> {
    private final PlayerPassData data = new PlayerPassData();
    private LazyOptional<PlayerPassData> optional = LazyOptional.of(() -> this.data);

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == PassCapability.PASS_DATA) {
            // Si el LazyOptional fue invalidado, lo REVIVIMOS al proximo acceso. Antes quedaba
            // invalidado para siempre y getData() devolvia null, lo que podia hacer que el
            // progreso/rango del pase quedara inaccesible. El objeto 'data' se conserva.
            if (!this.optional.isPresent()) {
                this.optional = LazyOptional.of(() -> this.data);
            }
            return this.optional.cast();
        }
        return LazyOptional.empty();
    }

    public void invalidate() {
        this.optional.invalidate();
    }

    public CompoundTag serializeNBT() {
        return this.data.toNbtForSave();
    }

    public void deserializeNBT(CompoundTag nbt) {
        this.data.fromNbt(nbt);
    }
}
