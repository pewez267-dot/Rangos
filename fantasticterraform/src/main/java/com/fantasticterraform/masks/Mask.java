package com.fantasticterraform.masks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Filtro que decide si un bloque concreto puede ser afectado por una operacion.
 * Las mascaras se combinan con AND (ver {@link MaskManager}).
 */
@FunctionalInterface
public interface Mask {

    /** {@code true} si el bloque en {@code pos} pasa el filtro y puede editarse. */
    boolean test(ServerLevel level, BlockPos pos);
}
