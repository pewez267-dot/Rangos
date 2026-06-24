package com.fantasticterraform.intelligent.population.conditions;

import com.fantasticterraform.intelligent.population.Condition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** El valor de humedad (0..1) debe estar dentro del rango. */
public final class MoistureCondition implements Condition {

    private final double min;
    private final double max;

    public MoistureCondition(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean evaluate(BlockPos surfacePos, ServerLevel level, double slope, double height, double moisture, double temperature) {
        return moisture >= min && moisture <= max;
    }
}
