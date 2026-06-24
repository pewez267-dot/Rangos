package com.fantasticterraform.editing.shapes;

import net.minecraft.core.BlockPos;

/**
 * Forma rellenable usada como herramienta de edicion (esfera/cilindro/piramide),
 * independiente del modo de seleccion. Su geometria real se respeta: solo los
 * bloques con {@code contains == true} se rellenan, recortados ademas al volumen de
 * la seleccion activa.
 */
public interface FillShape {

    boolean contains(BlockPos pos);

    BlockPos getMin();

    BlockPos getMax();
}
