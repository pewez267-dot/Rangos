package com.fantasticranks.capability;

import com.fantasticranks.data.PlayerRanksData;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public final class RanksCapabilityProvider
implements ICapabilitySerializable<CompoundTag> {
    private final PlayerRanksData data = new PlayerRanksData();
    private LazyOptional<PlayerRanksData> optional = LazyOptional.of(() -> this.data);

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == RanksCapability.RANKS_DATA) {
            // Si el LazyOptional fue invalidado (p. ej. tras cambio de dimension, muerte, o al
            // dispararse el listener de invalidacion), lo REVIVIMOS al proximo acceso. Antes quedaba
            // invalidado para siempre y getData() devolvia null -> el rango del jugador desaparecia.
            // El objeto 'data' se conserva, asi que no se pierde el progreso.
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
        return this.data.toNbt();
    }

    public void deserializeNBT(CompoundTag nbt) {
        this.data.fromNbt(nbt);
    }
}
