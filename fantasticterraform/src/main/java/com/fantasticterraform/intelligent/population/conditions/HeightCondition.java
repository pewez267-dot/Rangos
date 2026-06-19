package com.fantasticterraform.intelligent.population.conditions;

import com.fantasticterraform.intelligent.population.Condition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** La altura de la superficie debe estar dentro del rango Y. */
public final class HeightCondition implements Condition {

    private final double minY;
    private final double maxY;

    public HeightCondition(double minY, double maxY) {
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    public boolean evaluate(BlockPos surfacePos, ServerLevel level, double slope, double height, double moisture, double temperature) {
        return height >= minY && height <= maxY;
    }
}
