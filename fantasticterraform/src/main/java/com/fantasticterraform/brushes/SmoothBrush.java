package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Brush de suavizado de relieve. Promedia las alturas con un KERNEL GAUSSIANO 5x5
 * (resultado mucho mas natural que el promedio plano) y mezcla cada columna hacia ese
 * promedio segun la intensidad atenuada por el FALLOFF (el centro se suaviza mas que el
 * borde, evitando crear un escalon en el limite del brush).
 */
public final class SmoothBrush implements Brush {

    // Kernel gaussiano 5x5 (sigma ~1) normalizado por su suma al usarse.
    private static final double[][] KERNEL = {
            {1, 4, 6, 4, 1},
            {4, 16, 24, 16, 4},
            {6, 24, 36, 24, 6},
            {4, 16, 24, 16, 4},
            {1, 4, 6, 4, 1}
    };

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
        double maxBlend = Math.max(0.0D, Math.min(1.0D, s.intensity));

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
                double wsum = 0.0D;
                for (int kx = -2; kx <= 2; kx++) {
                    for (int kz = -2; kz <= 2; kz++) {
                        int nx = x + kx;
                        int nz = z + kz;
                        if (nx < 0 || nz < 0 || nx >= size || nz >= size || !lh.hasColumn(nx, nz)) {
                            continue;
                        }
                        double k = KERNEL[kx + 2][kz + 2];
                        sum += h[nx][nz] * k;
                        wsum += k;
                    }
                }
                double avg = wsum > 0 ? sum / wsum : h[x][z];
                // Atenuacion radial: el borde del brush apenas se altera (sin escalon).
                double dx = x - s.radius;
                double dz = z - s.radius;
                double fall = s.falloff.weight(Math.sqrt(dx * dx + dz * dz), s.radius);
                double blend = maxBlend * fall;
                target[x][z] = (int) Math.round(h[x][z] + (avg - h[x][z]) * blend);
            }
        }
        return BrushUtil.toPlacements(level, lh, target);
    }
}
