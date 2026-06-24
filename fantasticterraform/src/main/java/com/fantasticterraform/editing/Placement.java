package com.fantasticterraform.editing;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Colocacion de un bloque pendiente: posicion, estado destino y datos opcionales de
 * block entity (para pegar cofres, letreros, etc.). Un {@code state} nulo significa
 * "saltar esta posicion" (cuenta para el presupuesto por tick pero no escribe nada).
 */
public final class Placement {

    public final BlockPos pos;
    public final BlockState state;
    public final CompoundTag blockEntityData;

    public Placement(BlockPos pos, BlockState state, CompoundTag blockEntityData) {
        this.pos = pos;
        this.state = state;
        this.blockEntityData = blockEntityData;
    }

    public static Placement skip(BlockPos pos) {
        return new Placement(pos, null, null);
    }

    public static Placement of(BlockPos pos, BlockState state) {
        return new Placement(pos, state, null);
    }
}
