package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Flatten (aplanar): lleva la superficie del disco hacia la ALTURA del punto de click,
 * mezclando segun la intensidad y atenuando por el falloff (los bordes se quedan a su
 * altura original para fundirse con el entorno). Perfecto para crear explanadas, caminos
 * y plataformas naturales sin cortes.
 */
public final class FlattenBrush implements Brush {

    @Override
    public String id() {
        return "flatten";
    }

    @Override
    public String displayName() {
        return "Aplanar";
    }

    @Override
    public List<Placement> computePlacements(ServerLevel level, BlockPos center, BrushSettings s) {
        BrushUtil.LocalHeightmap lh = BrushUtil.build(level, center, s.radius);
        int size = lh.size;
        double maxBlend = Math.max(0.0D, Math.min(1.0D, s.intensity));
        int targetY = center.getY();

        int[][] target = new int[size][size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                if (!lh.hasColumn(x, z)) {
                    target[x][z] = lh.height[x][z];
                    continue;
                }
                double dx = x - s.radius;
                double dz = z - s.radius;
                double fall = s.falloff.weight(Math.sqrt(dx * dx + dz * dz), s.radius);
                double blend = maxBlend * fall;
                double cur = lh.height[x][z];
                target[x][z] = (int) Math.round(cur + (targetY - cur) * blend);
            }
        }
        return BrushUtil.toPlacements(level, lh, target);
    }
}
