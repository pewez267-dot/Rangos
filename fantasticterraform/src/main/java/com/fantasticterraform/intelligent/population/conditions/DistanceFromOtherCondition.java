package com.fantasticterraform.intelligent.population.conditions;

import com.fantasticterraform.intelligent.population.Condition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Exige una distancia minima respecto a otras instancias ya colocadas del mismo tipo
 * (referencia compartida con la regla), para evitar amontonamiento.
 */
public final class DistanceFromOtherCondition implements Condition {

    private final List<BlockPos> placedRef;
    private final int minDistance;

    public DistanceFromOtherCondition(List<BlockPos> placedRef, int minDistance) {
        this.placedRef = placedRef;
        this.minDistance = minDistance;
    }

    @Override
    public boolean evaluate(BlockPos surfacePos, ServerLevel level, double slope, double height, double moisture, double temperature) {
        long minSq = (long) minDistance * minDistance;
        for (BlockPos p : placedRef) {
            if (p.distSqr(surfacePos) < minSq) {
                return false;
            }
        }
        return true;
    }
}
