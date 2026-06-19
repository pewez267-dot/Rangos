package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Melt (derretir/limpiar): elimina bloques que sobresalen, picos y restos flotantes
 * contando cuantos de sus 26 vecinos son solidos. Si un bloque tiene pocos vecinos
 * solidos se retira (aire). La intensidad controla la agresividad (umbral de vecinos).
 * Ideal para pulir terreno tras esculpir y quitar "dientes" o islas de un bloque.
 */
public final class MeltBrush implements Brush {

    @Override
    public String id() {
        return "melt";
    }

    @Override
    public String displayName() {
        return "Derretir";
    }

    @Override
    public List<Placement> computePlacements(ServerLevel level, BlockPos center, BrushSettings s) {
        int r = s.radius;
        double r2 = (double) r * r;
        RandomSource rng = BrushUtil.rng(center);
        BlockState air = Blocks.AIR.defaultBlockState();
        // Umbral de vecinos solidos por debajo del cual se elimina (intensidad mas alta = mas agresivo).
        int threshold = 8 + (int) Math.round(Math.max(0.0D, Math.min(1.0D, s.intensity)) * 9.0D); // 8..17 de 26
        List<Placement> out = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > r2 + 1.0E-6D) {
                        continue;
                    }
                    int wx = center.getX() + dx;
                    int wy = center.getY() + dy;
                    int wz = center.getZ() + dz;
                    if (level.getBlockState(cursor.set(wx, wy, wz)).isAir()) {
                        continue;
                    }
                    double w = s.falloff.weight(Math.sqrt(d2), r);
                    if (w <= 0.0D || (w < 1.0D && rng.nextDouble() > w)) {
                        continue;
                    }
                    int solid = 0;
                    for (int ox = -1; ox <= 1; ox++) {
                        for (int oy = -1; oy <= 1; oy++) {
                            for (int oz = -1; oz <= 1; oz++) {
                                if (ox == 0 && oy == 0 && oz == 0) {
                                    continue;
                                }
                                if (!level.getBlockState(cursor.set(wx + ox, wy + oy, wz + oz)).isAir()) {
                                    solid++;
                                }
                            }
                        }
                    }
                    if (solid < threshold) {
                        out.add(Placement.of(new BlockPos(wx, wy, wz), air));
                    }
                }
            }
        }
        return out;
    }
}
