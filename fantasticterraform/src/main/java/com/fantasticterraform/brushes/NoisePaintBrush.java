package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.terrain.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * NoisePaint: pinta la superficie con DOS bloques distribuidos por un campo de ruido
 * coherente (parches naturales en vez de aleatorio puro). Ideal para mezclar
 * cesped/tierra, piedra/musgo, arena/grava, etc. La "Mezcla" controla cuanta superficie
 * recibe el bloque secundario; la profundidad permite pintar varias capas.
 */
public final class NoisePaintBrush implements Brush {

    @Override
    public String id() {
        return "noise";
    }

    @Override
    public String displayName() {
        return "Pintar (Ruido)";
    }

    @Override
    public List<Placement> computePlacements(ServerLevel level, BlockPos center, BrushSettings s) {
        int radius = s.radius;
        double r2 = (double) radius * radius;
        int depth = Math.max(1, s.depth);
        RandomSource rng = BrushUtil.rng(center);
        SimplexNoise noise = new SimplexNoise(center.asLong());
        // Umbral: mix=0 -> casi todo primario; mix=1 -> casi todo secundario.
        double threshold = 1.0D - 2.0D * Math.max(0.0D, Math.min(1.0D, s.mix));
        double scale = 0.18D;

        List<Placement> out = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d2 = dx * dx + dz * dz;
                if (d2 > r2 + 1.0E-6D) {
                    continue;
                }
                double w = s.falloff.weight(Math.sqrt(d2), radius);
                if (w <= 0.0D || (w < 1.0D && rng.nextDouble() > w)) {
                    continue;
                }
                int wx = center.getX() + dx;
                int wz = center.getZ() + dz;
                double n = noise.fractal2D(wx * scale, wz * scale, 3, 0.5D, 2.0D);
                BlockState chosen = (n >= threshold) ? s.secondaryBlock : s.block;
                for (int y = center.getY() + radius; y >= center.getY() - radius; y--) {
                    cursor.set(wx, y, wz);
                    boolean solid = !level.getBlockState(cursor).isAir();
                    boolean airAbove = level.getBlockState(cursor.above()).isAir();
                    if (solid && airAbove) {
                        for (int layer = 0; layer < depth; layer++) {
                            int yy = y - layer;
                            if (level.getBlockState(cursor.set(wx, yy, wz)).isAir()) {
                                break;
                            }
                            out.add(Placement.of(new BlockPos(wx, yy, wz), chosen));
                        }
                        break;
                    }
                }
            }
        }
        return out;
    }
}
