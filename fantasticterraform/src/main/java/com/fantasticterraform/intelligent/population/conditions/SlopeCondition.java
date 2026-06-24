package com.fantasticterraform.intelligent.population.conditions;

import com.fantasticterraform.intelligent.population.Condition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** La pendiente de la columna debe ser menor o igual al maximo dado. */
public final class SlopeCondition implements Condition {

    private final double maxSlope;

    public SlopeCondition(double maxSlope) {
        this.maxSlope = maxSlope;
    }

    @Override
    public boolean evaluate(BlockPos surfacePos, ServerLevel level, double slope, double height, double moisture, double temperature) {
        return slope <= maxSlope;
    }
}
