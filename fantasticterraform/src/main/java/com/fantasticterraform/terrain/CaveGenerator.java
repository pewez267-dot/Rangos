package com.fantasticterraform.terrain;

import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.editing.StreamingEditTask;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.terrain.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generacion procedural de cuevas mediante ruido 3D (Simplex propio, determinista).
 * Un bloque solido se vacia cuando el ruido en su posicion supera el umbral. El
 * umbral controla aproximadamente que porcentaje del volumen se talla.
 */
public final class CaveGenerator {

    private CaveGenerator() {
    }

    public static void apply(ServerPlayer player, ServerLevel level, SelectionShape sel,
                             double threshold, double scale, long seed, Mask mask) {
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        SimplexNoise noise = new SimplexNoise(seed);
        BlockState air = Blocks.AIR.defaultBlockState();
        double s = scale <= 0 ? 0.06D : scale;

        StreamingEditTask.StateProvider provider = (lvl, pos) -> {
            if (!sel.contains(pos)) {
                return null;
            }
            if (lvl.getBlockState(pos).isAir()) {
                return null;
            }
            double v = noise.noise3D(pos.getX() * s, pos.getY() * s, pos.getZ() * s);
            return v > threshold ? air : null;
        };

        int total = (int) Math.min(Integer.MAX_VALUE, sel.getVolume());
        BlockChangeQueue.enqueue(new StreamingEditTask(level, player.getUUID(), "Cuevas", total, mask,
                BlockPos.betweenClosed(sel.getMin(), sel.getMax()).iterator(), provider));
    }
}
