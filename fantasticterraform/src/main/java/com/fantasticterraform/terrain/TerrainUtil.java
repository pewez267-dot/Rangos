package com.fantasticterraform.terrain;

import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.StreamingEditTask;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Utilidades compartidas por las operaciones de terreno: extraccion del heightmap de
 * superficie dentro de una seleccion y aplicacion de un heightmap objetivo
 * (rellenando huecos de aire por debajo y tallando solido por encima), todo a traves
 * de la cola por ticks.
 */
public final class TerrainUtil {

    private TerrainUtil() {
    }

    /** Datos por columna (XZ) de la superficie dentro de la seleccion. */
    public static final class Heightmap {
        public final int minX;
        public final int minZ;
        public final int width;
        public final int depth;
        public final int selMinY;
        public final int selMaxY;
        /** Y de la superficie de cada columna, o {@code selMinY - 1} si la columna esta vacia. */
        public final int[][] height;
        public final BlockState[][] surface;

        Heightmap(int minX, int minZ, int width, int depth, int selMinY, int selMaxY) {
            this.minX = minX;
            this.minZ = minZ;
            this.width = width;
            this.depth = depth;
            this.selMinY = selMinY;
            this.selMaxY = selMaxY;
            this.height = new int[width][depth];
            this.surface = new BlockState[width][depth];
        }

        public boolean hasColumn(int ix, int iz) {
            return height[ix][iz] >= selMinY;
        }
    }

    /** Construye el heightmap de superficie de la seleccion leyendo el mundo una sola vez por columna. */
    public static Heightmap buildHeightmap(ServerLevel level, SelectionShape sel) {
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        int w = max.getX() - min.getX() + 1;
        int d = max.getZ() - min.getZ() + 1;
        Heightmap hm = new Heightmap(min.getX(), min.getZ(), w, d, min.getY(), max.getY());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int ix = 0; ix < w; ix++) {
            for (int iz = 0; iz < d; iz++) {
                int wx = min.getX() + ix;
                int wz = min.getZ() + iz;
                int found = min.getY() - 1;
                BlockState surf = null;
                for (int y = max.getY(); y >= min.getY(); y--) {
                    cursor.set(wx, y, wz);
                    if (sel.contains(cursor) && !level.getBlockState(cursor).isAir()) {
                        found = y;
                        surf = level.getBlockState(cursor);
                        break;
                    }
                }
                hm.height[ix][iz] = found;
                hm.surface[ix][iz] = surf;
            }
        }
        return hm;
    }

    /** Bloque subsuperficial coherente con la superficie (cesped/micelio/podzol -> tierra). */
    public static BlockState subsurfaceFor(BlockState surface) {
        if (surface == null) {
            return Blocks.STONE.defaultBlockState();
        }
        if (surface.is(Blocks.GRASS_BLOCK) || surface.is(Blocks.MYCELIUM) || surface.is(Blocks.PODZOL)) {
            return Blocks.DIRT.defaultBlockState();
        }
        return surface;
    }

    /**
     * Aplica un heightmap objetivo: rellena con material por debajo de la nueva altura
     * y talla (aire) por encima. Solo afecta columnas con superficie original (no
     * fabrica terreno flotante en columnas vacias).
     */
    public static void applyHeightmap(ServerPlayer player, ServerLevel level, SelectionShape sel,
                                      String name, int[][] targetHeight, Heightmap source, Mask mask) {
        BlockState air = Blocks.AIR.defaultBlockState();
        int minX = source.minX;
        int minZ = source.minZ;
        StreamingEditTask.StateProvider provider = (lvl, pos) -> {
            if (!sel.contains(pos)) {
                return null;
            }
            int ix = pos.getX() - minX;
            int iz = pos.getZ() - minZ;
            if (ix < 0 || iz < 0 || ix >= source.width || iz >= source.depth) {
                return null;
            }
            if (!source.hasColumn(ix, iz)) {
                return null; // columna vacia: no inventar terreno.
            }
            int h = targetHeight[ix][iz];
            BlockState surf = source.surface[ix][iz];
            BlockState cur = lvl.getBlockState(pos);
            if (pos.getY() <= h) {
                if (!cur.isAir()) {
                    return null; // ya hay solido, conservar.
                }
                return pos.getY() == h ? surf : subsurfaceFor(surf);
            }
            return cur.isAir() ? null : air;
        };
        int total = (int) Math.min(Integer.MAX_VALUE, sel.getVolume());
        BlockChangeQueue.enqueue(new StreamingEditTask(level, player.getUUID(), name, total, mask,
                BlockPos.betweenClosed(sel.getMin(), sel.getMax()).iterator(), provider));
    }
}
