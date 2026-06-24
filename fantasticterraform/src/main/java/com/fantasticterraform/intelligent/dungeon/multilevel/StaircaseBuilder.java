package com.fantasticterraform.intelligent.dungeon.multilevel;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Escalera tallada en escalon entre dos puntos con distinta altura, a lo largo de un
 * eje horizontal. Cada paso sube un bloque y deja altura libre para caminar.
 */
public final class StaircaseBuilder {

    private StaircaseBuilder() {
    }

    public static void build(List<Placement> out, BlockPos from, BlockPos to, BlockState step) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        boolean alongX = Math.abs(dx) >= Math.abs(dz);
        int horizontal = alongX ? Math.abs(dx) : Math.abs(dz);
        if (horizontal == 0) {
            return;
        }
        int sx = Integer.signum(dx);
        int sz = Integer.signum(dz);
        int dy = to.getY() - from.getY();
        for (int i = 0; i <= horizontal; i++) {
            int x = alongX ? from.getX() + sx * i : from.getX();
            int z = alongX ? from.getZ() : from.getZ() + sz * i;
            int y = from.getY() + (int) Math.round((double) dy * i / horizontal);
            out.add(Placement.of(new BlockPos(x, y, z), step));
            // Altura libre para caminar.
            for (int h = 1; h <= 3; h++) {
                out.add(Placement.of(new BlockPos(x, y + h, z), Blocks.AIR.defaultBlockState()));
            }
        }
    }
}
