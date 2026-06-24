package com.fantasticterraform.history;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Un unico cambio de bloque reversible: posicion, estado anterior, estado nuevo y,
 * si lo habia, los datos del block entity anterior (para no perder cofres, letreros,
 * spawners, etc. al deshacer).
 */
public final class BlockChange {

    public final BlockPos pos;
    public final BlockState previousState;
    public final BlockState newState;
    public final CompoundTag previousBlockEntityData;

    public BlockChange(BlockPos pos, BlockState previousState, BlockState newState, CompoundTag previousBlockEntityData) {
        this.pos = pos.immutable();
        this.previousState = previousState;
        this.newState = newState;
        this.previousBlockEntityData = previousBlockEntityData;
    }
}
