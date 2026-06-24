package com.fantasticterraform.selection;

import net.minecraft.core.BlockPos;

/**
 * Geometria real de una seleccion. Toda iteracion sobre bloques debe recorrer el
 * bounding box ({@link #getMin()}..{@link #getMax()}) como limite exterior pero
 * aplicar la operacion unicamente donde {@link #contains(BlockPos)} sea {@code true}.
 *
 * <p>Una esfera NUNCA rellena su bounding box cubico: solo los bloques cuyo
 * {@code contains} es verdadero forman parte del volumen real.</p>
 */
public interface SelectionShape {

    /** Geometria exacta: si el bloque pertenece o no al volumen real de la forma. */
    boolean contains(BlockPos pos);

    /** Esquina minima (inclusive) del bounding box que envuelve la forma. */
    BlockPos getMin();

    /** Esquina maxima (inclusive) del bounding box que envuelve la forma. */
    BlockPos getMax();

    /** Numero (estimado o exacto) de bloques que componen el volumen real. */
    long getVolume();

    /** Tipo de geometria. */
    SelectionType getType();
}
