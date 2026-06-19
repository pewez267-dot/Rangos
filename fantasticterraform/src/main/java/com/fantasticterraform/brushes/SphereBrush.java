package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.editing.shapes.SphereShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/** Coloca un bloque en una esfera del radio configurado, centrada en el punto de click. */
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
        SphereShape shape = new SphereShape(center, s.radius);
        List<Placement> out = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(shape.getMin(), shape.getMax())) {
            if (shape.contains(pos)) {
                out.add(Placement.of(pos.immutable(), s.block));
            }
        }
        return out;
    }
}
