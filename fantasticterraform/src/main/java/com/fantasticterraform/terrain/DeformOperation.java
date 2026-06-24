package com.fantasticterraform.terrain;

import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.terrain.noise.SimplexNoise;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Deformacion: desplaza verticalmente la superficie de la region segun una curva
 * configurable. Crea colinas, depresiones o pendientes sobre superficies planas.
 */
public final class DeformOperation {

    public enum Curve {
        LINEAR,
        EASE_IN_OUT,
        NOISE
    }

    private DeformOperation() {
    }

    public static void apply(ServerPlayer player, ServerLevel level, SelectionShape sel,
                             Curve curve, double amplitude, long seed, Mask mask) {
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        TerrainUtil.Heightmap hm = TerrainUtil.buildHeightmap(level, sel);
        int w = hm.width;
        int d = hm.depth;
        SimplexNoise noise = new SimplexNoise(seed);

        int[][] target = new int[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                double tx = w <= 1 ? 0.0D : (double) x / (w - 1);
                double tz = d <= 1 ? 0.0D : (double) z / (d - 1);
                double factor;
                switch (curve) {
                    case EASE_IN_OUT:
                        factor = smoothstep(tx) - 0.5D; // pendiente suave centrada
                        break;
                    case NOISE:
                        factor = noise.fractal2D((hm.minX + x) * 0.08D, (hm.minZ + z) * 0.08D, 4, 0.5D, 2.0D);
                        break;
                    case LINEAR:
                    default:
                        factor = tx - 0.5D; // pendiente lineal centrada
                        break;
                }
                int delta = (int) Math.round(amplitude * factor);
                target[x][z] = hm.height[x][z] + delta;
            }
        }
        TerrainUtil.applyHeightmap(player, level, sel, "Deformar", target, hm, mask);
    }

    private static double smoothstep(double t) {
        t = Math.max(0.0D, Math.min(1.0D, t));
        return t * t * (3.0D - 2.0D * t);
    }
}
