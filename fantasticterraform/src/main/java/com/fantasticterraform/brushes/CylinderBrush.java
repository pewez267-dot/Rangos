package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Cilindro de colocacion centrado verticalmente en el click, con falloff radial (borde
 * suave), mezcla de bloques y modo hueco (solo la pared del cilindro).
 */
public final class CylinderBrush implements Brush {

    @Override
    public String id() {
        return "cylinder";
    }

    @Override
    public String displayName() {
        return "Cilindro";
    }

    @Override
    public List<Placement> computePlacements(ServerLevel level, BlockPos center, BrushSettings s) {
        int r = s.radius;
        int height = Math.max(1, s.height);
        int y0 = center.getY() - height / 2;
        int y1 = y0 + height - 1;
        double r2 = (double) r * r;
        double inner2 = (double) (r - 1) * (r - 1);
        RandomSource rng = BrushUtil.rng(center);
        List<Placement> out = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d2 = dx * dx + dz * dz;
                if (d2 > r2 + 1.0E-6D) {
                    continue;
                }
                if (s.hollow && d2 < inner2) {
                    continue;
                }
                double w = s.falloff.weight(Math.sqrt(d2), r);
                if (w <= 0.0D || (w < 1.0D && rng.nextDouble() > w)) {
                    continue;
                }
                for (int y = y0; y <= y1; y++) {
                    BlockState state = BrushUtil.pick(s, rng);
                    out.add(Placement.of(new BlockPos(center.getX() + dx, y, center.getZ() + dz), state));
                }
            }
        }
        return out;
    }
}
