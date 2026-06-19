package com.fantasticterraform.intelligent.population;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Regla de poblamiento: que colocar, bajo que condiciones (AND) y con que peso
 * probabilistico, modulado por un ruido de densidad para crear agrupaciones naturales.
 */
public final class PlacementRule {

    public final String id;
    public final boolean tree;
    public final BlockState toPlace;
    public final List<Condition> conditions;
    public final double baseWeight;
    public final double weightNoiseInfluence;
    /** Posiciones ya colocadas por esta regla (para la condicion de distancia). */
    public final List<BlockPos> placed = new ArrayList<>();

    public PlacementRule(String id, boolean tree, BlockState toPlace, List<Condition> conditions,
                         double baseWeight, double weightNoiseInfluence) {
        this.id = id;
        this.tree = tree;
        this.toPlace = toPlace;
        this.conditions = conditions;
        this.baseWeight = baseWeight;
        this.weightNoiseInfluence = weightNoiseInfluence;
    }

    public boolean applies(BlockPos surfacePos, ServerLevel level, double slope, double height, double moisture, double temperature) {
        for (Condition c : conditions) {
            if (!c.evaluate(surfacePos, level, slope, height, moisture, temperature)) {
                return false;
            }
        }
        return true;
    }

    /** Probabilidad final combinando el peso base con el ruido de densidad. */
    public double finalWeight(double density) {
        double w = baseWeight + density * weightNoiseInfluence;
        return Math.max(0.0D, Math.min(1.0D, w));
    }
}
