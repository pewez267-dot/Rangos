package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Brush de erosion: aplica erosion termica simplificada solo dentro del radio del
 * brush. Mueve material de columnas altas a vecinas bajas cuando la diferencia supera
 * un angulo de reposo, escalado por la intensidad.
 */
public final class ErodeBrush implements Brush {

    private static final int[][] NEIGHBORS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    @Override
    public String id() {
        return "erode";
    }

    @Override
    public String displayName() {
        return "Erosion";
    }

    @Override
    public List<Placement> computePlacements(ServerLevel level, BlockPos center, BrushSettings s) {
        BrushUtil.LocalHeightmap lh = BrushUtil.build(level, center, s.radius);
        int size = lh.size;
        double factor = Math.max(0.0D, Math.min(1.0D, s.intensity));
        double talus = 1.0D;

        double[][] h = new double[size][size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                h[x][z] = lh.height[x][z];
            }
        }

        double[][] delta = new double[size][size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                if (!lh.hasColumn(x, z)) {
                    continue;
                }
                double dcx = x - lh.radius;
                double dcz = z - lh.radius;
                double fall = s.falloff.weight(Math.sqrt(dcx * dcx + dcz * dcz), lh.radius);
                for (int[] nb : NEIGHBORS) {
                    int nx = x + nb[0];
                    int nz = z + nb[1];
                    if (nx < 0 || nz < 0 || nx >= size || nz >= size || !lh.hasColumn(nx, nz)) {
                        continue;
                    }
                    double diff = h[x][z] - h[nx][nz];
                    if (diff > talus) {
                        double move = (diff - talus) * 0.5D * factor * fall;
                        delta[x][z] -= move;
                        delta[nx][nz] += move;
                    }
                }
            }
        }

        int[][] target = new int[size][size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                target[x][z] = (int) Math.round(h[x][z] + delta[x][z]);
            }
        }
        return BrushUtil.toPlacements(level, lh, target);
    }
}
