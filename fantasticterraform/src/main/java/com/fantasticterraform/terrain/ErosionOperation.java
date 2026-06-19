package com.fantasticterraform.terrain;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Erosion termica simplificada sobre el heightmap. Si la diferencia de altura entre
 * columnas vecinas supera el angulo de reposo (talus), se mueve una fraccion del
 * material de la columna alta hacia la baja. Se itera el numero de pasadas indicado
 * sobre el array de alturas y solo al final se aplica al mundo una vez.
 */
public final class ErosionOperation {

    private static final int[][] NEIGHBORS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private ErosionOperation() {
    }

    public static void apply(ServerPlayer player, ServerLevel level, SelectionShape sel,
                             int passes, double talus, double factor, Mask mask) {
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        int maxPasses = TerraformConfig.GENERAL.maxErosionPasses.get();
        int n = Math.max(1, Math.min(maxPasses, passes));
        double t = Math.max(0.0D, talus);
        double f = Math.max(0.0D, Math.min(1.0D, factor));

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
            double[][] delta = new double[w][d];
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    if (!hm.hasColumn(x, z)) {
                        continue;
                    }
                    for (int[] nb : NEIGHBORS) {
                        int nx = x + nb[0];
                        int nz = z + nb[1];
                        if (nx < 0 || nz < 0 || nx >= w || nz >= d || !hm.hasColumn(nx, nz)) {
                            continue;
                        }
                        double diff = h[x][z] - h[nx][nz];
                        if (diff > t) {
                            double move = (diff - t) * 0.5D * f;
                            delta[x][z] -= move;
                            delta[nx][nz] += move;
                        }
                    }
                }
            }
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    h[x][z] += delta[x][z];
                }
            }
        }

        int[][] target = new int[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                target[x][z] = (int) Math.round(h[x][z]);
            }
        }
        TerrainUtil.applyHeightmap(player, level, sel, "Erosionar", target, hm, mask);
    }
}
