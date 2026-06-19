package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Esfera de escultura con BORDE SUAVE (falloff), mezcla de dos bloques (scatter) y modo
 * hueco. Con falloff DURO equivale a la esfera solida clasica; con SUAVE/GAUSSIANO los
 * bloques del borde se colocan de forma probabilistica para un acabado organico.
 */
public final class SphereBrush implements Brush {

    @Override
    public String id() {
        return "sphere";
    }

    @Override
    public String displayName() {
        return "Esfera";
    }

    @Override
    public List<Placement> computePlacements(ServerLevel level, BlockPos center, BrushSettings s) {
        int r = s.radius;
        double r2 = (double) r * r;
        double inner2 = (double) (r - 1) * (r - 1);
        RandomSource rng = BrushUtil.rng(center);
        List<Placement> out = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > r2 + 1.0E-6D) {
                        continue;
                    }
                    if (s.hollow && d2 < inner2) {
                        continue; // solo la cascara
                    }
                    double dist = Math.sqrt(d2);
                    double w = s.falloff.weight(dist, r);
                    if (w <= 0.0D || (w < 1.0D && rng.nextDouble() > w)) {
                        continue; // borde difuminado
                    }
                    BlockState state = BrushUtil.pick(s, rng);
                    out.add(Placement.of(new BlockPos(center.getX() + dx, center.getY() + dy, center.getZ() + dz), state));
                }
            }
        }
        return out;
    }
}
