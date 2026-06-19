package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Brush de superficie (overlay): reemplaza solo la capa superior expuesta al cielo
 * dentro del radio, para pintar cesped/nieve/arena sin afectar lo de debajo.
 */
public final class OverlayBrush implements Brush {

    @Override
    public String id() {
        return "overlay";
    }

    @Override
    public String displayName() {
        return "Superficie (Overlay)";
    }

    @Override
    public List<Placement> computePlacements(ServerLevel level, BlockPos center, BrushSettings s) {
        int radius = s.radius;
        double r2 = (double) radius * radius;
        List<Placement> out = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > r2 + 1.0E-6D) {
                    continue;
                }
                int wx = center.getX() + dx;
                int wz = center.getZ() + dz;
                // Buscar el bloque solido mas alto con aire encima, dentro de la ventana vertical.
                for (int y = center.getY() + radius; y >= center.getY() - radius; y--) {
                    cursor.set(wx, y, wz);
                    boolean solid = !level.getBlockState(cursor).isAir();
                    boolean airAbove = level.getBlockState(cursor.above()).isAir();
                    if (solid && airAbove) {
                        BlockPos top = new BlockPos(wx, y, wz);
                        out.add(Placement.of(top, s.block));
                        break;
                    }
                }
            }
        }
        return out;
    }
}
