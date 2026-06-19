package com.fantasticterraform.terrain;

import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.editing.StreamingEditTask;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Naturalizacion: reemplaza las capas superiores de una region artificial siguiendo
 * un patron configurable (superficie, varias capas de tierra y el resto piedra).
 * Solo afecta bloques solidos existentes; no fabrica terreno nuevo.
 */
public final class NaturalizeOperation {

    private NaturalizeOperation() {
    }

    public static void apply(ServerPlayer player, ServerLevel level, SelectionShape sel,
                             BlockState surfaceBlock, BlockState dirtBlock, BlockState stoneBlock,
                             int dirtLayers, Mask mask) {
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        TerrainUtil.Heightmap hm = TerrainUtil.buildHeightmap(level, sel);
        int minX = hm.minX;
        int minZ = hm.minZ;
        int layers = Math.max(0, dirtLayers);

        StreamingEditTask.StateProvider provider = (lvl, pos) -> {
            if (!sel.contains(pos)) {
                return null;
            }
            int ix = pos.getX() - minX;
            int iz = pos.getZ() - minZ;
            if (ix < 0 || iz < 0 || ix >= hm.width || iz >= hm.depth || !hm.hasColumn(ix, iz)) {
                return null;
            }
            BlockState cur = lvl.getBlockState(pos);
            if (cur.isAir()) {
                return null; // solo re-texturiza solido existente.
            }
            int depth = hm.height[ix][iz] - pos.getY();
            if (depth < 0) {
                return null; // por encima de la superficie original.
            }
            if (depth == 0) {
                return surfaceBlock;
            }
            if (depth <= layers) {
                return dirtBlock;
            }
            return stoneBlock;
        };

        int total = (int) Math.min(Integer.MAX_VALUE, sel.getVolume());
        BlockChangeQueue.enqueue(new StreamingEditTask(level, player.getUUID(), "Naturalizar", total, mask,
                BlockPos.betweenClosed(sel.getMin(), sel.getMax()).iterator(), provider));
    }
}
