package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Blend (fundido): suaviza los LIMITES entre materiales. Para cada bloque del radio se
 * mira a sus seis vecinos (incluido el aire) y se adopta el material mayoritario. Esto
 * "derrite" transiciones duras (p. ej. bordes de piedra/tierra/aire) dando un acabado
 * 3D organico sin alterar el relieve global. Funciona en cualquier orientacion (no solo
 * en la superficie como los brushes de heightmap).
 */
public final class BlendBrush implements Brush {

    private static final int[][] NB = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    @Override
    public String id() {
        return "blend";
    }

    @Override
    public String displayName() {
        return "Fundir (Blend)";
    }

    @Override
    public List<Placement> computePlacements(ServerLevel level, BlockPos center, BrushSettings s) {
        int r = s.radius;
        double r2 = (double) r * r;
        RandomSource rng = BrushUtil.rng(center);
        BlockState air = Blocks.AIR.defaultBlockState();
        List<Placement> out = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > r2 + 1.0E-6D) {
                        continue;
                    }
                    double w = s.falloff.weight(Math.sqrt(d2), r);
                    if (w <= 0.0D || (w < 1.0D && rng.nextDouble() > w)) {
                        continue;
                    }
                    int wx = center.getX() + dx;
                    int wy = center.getY() + dy;
                    int wz = center.getZ() + dz;
                    BlockState current = level.getBlockState(cursor.set(wx, wy, wz));

                    Map<Block, Integer> votes = new HashMap<>();
                    int airVotes = 0;
                    for (int[] o : NB) {
                        BlockState ns = level.getBlockState(cursor.set(wx + o[0], wy + o[1], wz + o[2]));
                        if (ns.isAir()) {
                            airVotes++;
                        } else {
                            votes.merge(ns.getBlock(), 1, Integer::sum);
                        }
                    }
                    // Mayoria: si el aire gana, vaciar; si no, adoptar el bloque mayoritario.
                    Block bestBlock = null;
                    int bestCount = 0;
                    for (Map.Entry<Block, Integer> e : votes.entrySet()) {
                        if (e.getValue() > bestCount) {
                            bestCount = e.getValue();
                            bestBlock = e.getKey();
                        }
                    }
                    BlockState result;
                    if (airVotes > bestCount) {
                        result = air;
                    } else if (bestBlock != null) {
                        result = bestBlock.defaultBlockState();
                    } else {
                        continue;
                    }
                    if (!result.equals(current)) {
                        out.add(Placement.of(new BlockPos(wx, wy, wz), result));
                    }
                }
            }
        }
        return out;
    }
}
