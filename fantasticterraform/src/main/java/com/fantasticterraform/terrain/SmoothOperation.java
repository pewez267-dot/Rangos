package com.fantasticterraform.terrain;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Suavizado por heightmap. Para cada columna XZ se promedia la altura de superficie
 * en un kernel (3x3 si radius=1, 5x5 si radius=2) y se ajusta gradualmente hacia ese
 * promedio segun la intensidad. Las pasadas se aplican sobre el array de alturas
 * (operacion pura) y solo al final se escribe el mundo una vez, evitando encadenar
 * trabajos asincronos.
 */
public final class SmoothOperation {

    private SmoothOperation() {
    }

    public static void apply(ServerPlayer player, ServerLevel level, SelectionShape sel,
                             int kernelRadius, double intensity, int passes, Mask mask) {
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        int radius = Math.max(1, Math.min(2, kernelRadius));
        double blend = Math.max(0.0D, Math.min(1.0D, intensity));
        int maxPasses = TerraformConfig.GENERAL.maxSmoothPasses.get();
        int n = Math.max(1, Math.min(maxPasses, passes));

        TerrainUtil.Heightmap hm = TerrainUtil.buildHeightmap(level, sel);
        int w = hm.width;
        int d = hm.depth;

        double[][] h = new double[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                h[x][z] = hm.height[x][z];
            }
        }

        for (int pass = 0; pass < n; pass++) {
            double[][] next = new double[w][d];
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    if (!hm.hasColumn(x, z)) {
                        next[x][z] = h[x][z];
                        continue;
                    }
                    double sum = 0.0D;
                    int count = 0;
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            int nx = x + dx;
                            int nz = z + dz;
                            if (nx < 0 || nz < 0 || nx >= w || nz >= d || !hm.hasColumn(nx, nz)) {
                                continue;
                            }
                            sum += h[nx][nz];
                            count++;
                        }
                    }
                    double avg = count > 0 ? sum / count : h[x][z];
                    next[x][z] = h[x][z] + (avg - h[x][z]) * blend;
                }
            }
            h = next;
        }

        int[][] target = new int[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                target[x][z] = (int) Math.round(h[x][z]);
            }
        }
        TerrainUtil.applyHeightmap(player, level, sel, "Suavizar", target, hm, mask);
    }
}
