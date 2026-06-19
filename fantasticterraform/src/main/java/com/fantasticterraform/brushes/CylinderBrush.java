package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.editing.shapes.CylinderShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/** Coloca un bloque en un cilindro centrado verticalmente en el punto de click. */
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
        int height = Math.max(1, s.height);
        BlockPos base = center.offset(0, -height / 2, 0);
        CylinderShape shape = new CylinderShape(base, s.radius, height);
        List<Placement> out = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(shape.getMin(), shape.getMax())) {
            if (shape.contains(pos)) {
                out.add(Placement.of(pos.immutable(), s.block));
            }
        }
        return out;
    }
}
