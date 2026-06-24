package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.editing.shapes.SphereShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Esfera de aire del radio configurado: vacia una bola alrededor del punto de click. */
public final class SphereClearBrush implements Brush {

    @Override
    public String id() {
        return "sphere_clear";
    }

    @Override
    public String displayName() {
        return "Vaciado esferico";
    }

    @Override
    public List<Placement> computePlacements(ServerLevel level, BlockPos center, BrushSettings s) {
        SphereShape shape = new SphereShape(center, s.radius);
        BlockState air = Blocks.AIR.defaultBlockState();
        List<Placement> out = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(shape.getMin(), shape.getMax())) {
            if (shape.contains(pos) && !level.getBlockState(pos).isAir()) {
                out.add(Placement.of(pos.immutable(), air));
            }
        }
        return out;
    }
}
