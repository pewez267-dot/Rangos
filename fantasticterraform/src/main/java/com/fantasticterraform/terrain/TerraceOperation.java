package com.fantasticterraform.terrain;

import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Terrazas: cuantiza la altura de la superficie en escalones de tamano fijo, creando
 * mesetas escalonadas tipo arrozal/cantera. Cada columna se redondea al multiplo de
 * {@code step} mas cercano respecto a la base de la seleccion. Solo afecta columnas con
 * superficie existente y se escribe en una sola pasada.
 */
public final class TerraceOperation {

    private TerraceOperation() {
    }

    public static void apply(ServerPlayer player, ServerLevel level, SelectionShape sel, int step, Mask mask) {
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        int s = Math.max(2, step);
        TerrainUtil.Heightmap hm = TerrainUtil.buildHeightmap(level, sel);
        int w = hm.width;
        int d = hm.depth;
        int base = sel.getMin().getY();

        int[][] target = new int[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (!hm.hasColumn(x, z)) {
                    target[x][z] = hm.height[x][z];
                    continue;
                }
                int rel = hm.height[x][z] - base;
                int stepped = Math.round((float) rel / s) * s;
                target[x][z] = base + stepped;
            }
        }
        TerrainUtil.applyHeightmap(player, level, sel, "Terrazas", target, hm, mask);
    }
}
