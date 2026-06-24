package com.fantasticterraform.intelligent.population;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Condicion de una regla de poblamiento. Todas las condiciones de una regla deben
 * cumplirse (AND) para que la regla pueda aplicar en esa columna.
 */
public interface Condition {

    boolean evaluate(BlockPos surfacePos, ServerLevel level, double slope, double height, double moisture, double temperature);
}
