package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Brush de suavizado: aplica el algoritmo de suavizado por heightmap solo dentro del
 * radio del brush. Promedia las alturas de las columnas vecinas dentro del disco y
 * mezcla hacia el promedio segun la intensidad.
 */
public final class SmoothBrush implements Brush {

    @Override
    public String id() {
        return "smooth";
    }

    @Override
    public String displayName() {
        return "Suavizado";
    }

    @Override
    public List<Placement> computePlacements(ServerLevel level, BlockPos center, BrushSettings s) {
        BrushUtil.LocalHeightmap lh = BrushUtil.build(level, center, s.radius);
        int size = lh.size;
        double blend = Math.max(0.0D, Math.min(1.0D, s.intensity));

        double[][] h = new double[size][size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                h[x][z] = lh.height[x][z];
            }
        }

        int[][] target = new int[size][size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                if (!lh.hasColumn(x, z)) {
                    target[x][z] = lh.height[x][z];
                    continue;
                }
                double sum = 0.0D;
                int count = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int nx = x + dx;
                        int nz = z + dz;
                        if (nx < 0 || nz < 0 || nx >= size || nz >= size || !lh.hasColumn(nx, nz)) {
                            continue;
                        }
                        sum += h[nx][nz];
                        count++;
                    }
                }
                double avg = count > 0 ? sum / count : h[x][z];
                target[x][z] = (int) Math.round(h[x][z] + (avg - h[x][z]) * blend);
            }
        }
        return BrushUtil.toPlacements(level, lh, target);
    }
}
