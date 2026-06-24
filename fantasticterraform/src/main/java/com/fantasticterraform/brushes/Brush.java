package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Herramienta de escultura tipo pincel. Cada brush calcula la lista de bloques que
 * desea modificar alrededor del punto de click. La validacion de contencion en la
 * seleccion activa la realiza {@link BrushManager} de forma centralizada: si
 * cualquier bloque del brush queda fuera de la seleccion, la operacion se rechaza.
 */
public interface Brush {

    String id();

    String displayName();

    /** Calcula las colocaciones candidatas leyendo el mundo actual. */
    List<Placement> computePlacements(ServerLevel level, BlockPos center, BrushSettings settings);
}
