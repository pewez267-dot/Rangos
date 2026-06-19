package com.fantasticterraform.intelligent.dungeon.multilevel;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Sala-puente de doble altura que atraviesa de un punto a otro con un piso solido y
 * barandales, dejando un vano amplio (caida controlada) a los lados.
 */
public final class BridgeRoomBuilder {

    private BridgeRoomBuilder() {
    }

    public static void build(List<Placement> out, BlockPos from, BlockPos to, BlockState floor, BlockState rail) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        boolean alongX = Math.abs(dx) >= Math.abs(dz);
        int len = alongX ? Math.abs(dx) : Math.abs(dz);
        if (len == 0) {
            return;
        }
        int sx = Integer.signum(dx);
        int sz = Integer.signum(dz);
        int y = from.getY();
        for (int i = 0; i <= len; i++) {
            int cx = alongX ? from.getX() + sx * i : from.getX();
            int cz = alongX ? from.getZ() : from.getZ() + sz * i;
            // Piso de 3 de ancho.
            for (int w = -1; w <= 1; w++) {
                int px = alongX ? cx : cx + w;
                int pz = alongX ? cz + w : cz;
                out.add(Placement.of(new BlockPos(px, y, pz), floor));
                // Barandal en los bordes.
                if (w == -1 || w == 1) {
                    out.add(Placement.of(new BlockPos(px, y + 1, pz), rail));
                }
                // Altura libre.
                for (int h = 1; h <= 4; h++) {
                    if (w == 0) {
                        out.add(Placement.of(new BlockPos(px, y + h, pz), Blocks.AIR.defaultBlockState()));
                    }
                }
            }
        }
    }
}
