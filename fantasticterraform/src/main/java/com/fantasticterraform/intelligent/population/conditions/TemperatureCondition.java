package com.fantasticterraform.intelligent.population.conditions;

import com.fantasticterraform.intelligent.population.Condition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** El valor de temperatura (0..1) debe estar dentro del rango. */
public final class TemperatureCondition implements Condition {

    private final double min;
    private final double max;

    public TemperatureCondition(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean evaluate(BlockPos surfacePos, ServerLevel level, double slope, double height, double moisture, double temperature) {
        return temperature >= min && temperature <= max;
    }
}
