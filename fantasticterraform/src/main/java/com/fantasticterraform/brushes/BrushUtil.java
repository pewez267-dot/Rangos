package com.fantasticterraform.brushes;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.terrain.TerrainUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Heightmap local para los brushes de escultura (suavizado/erosion), limitado a un
 * disco XZ de radio dado y a una ventana vertical alrededor del punto de click.
 */
public final class BrushUtil {

    private BrushUtil() {
    }

    public static final class LocalHeightmap {
        public final int cx;
        public final int cz;
        public final int radius;
        public final int size;
        public final int minY;
        public final int maxY;
        public final int[][] height;
        public final BlockState[][] surface;
        public final boolean[][] inDisc;

        LocalHeightmap(int cx, int cz, int radius, int minY, int maxY) {
            this.cx = cx;
            this.cz = cz;
            this.radius = radius;
            this.size = 2 * radius + 1;
            this.minY = minY;
            this.maxY = maxY;
            this.height = new int[size][size];
            this.surface = new BlockState[size][size];
            this.inDisc = new boolean[size][size];
        }

        public boolean hasColumn(int ix, int iz) {
            return inDisc[ix][iz] && height[ix][iz] >= minY;
        }
    }

    public static LocalHeightmap build(ServerLevel level, BlockPos center, int radius) {
        int minY = center.getY() - radius;
        int maxY = center.getY() + radius;
        LocalHeightmap lh = new LocalHeightmap(center.getX(), center.getZ(), radius, minY, maxY);
        double r2 = (double) radius * radius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int ix = 0; ix < lh.size; ix++) {
            for (int iz = 0; iz < lh.size; iz++) {
                int dx = ix - radius;
                int dz = iz - radius;
                boolean disc = (dx * dx + dz * dz) <= r2 + 1.0E-6D;
                lh.inDisc[ix][iz] = disc;
                lh.height[ix][iz] = minY - 1;
                if (!disc) {
                    continue;
                }
                int wx = center.getX() + dx;
                int wz = center.getZ() + dz;
                for (int y = maxY; y >= minY; y--) {
                    cursor.set(wx, y, wz);
                    if (!level.getBlockState(cursor).isAir()) {
                        lh.height[ix][iz] = y;
                        lh.surface[ix][iz] = level.getBlockState(cursor).getBlock().defaultBlockState();
                        break;
                    }
                }
            }
        }
        return lh;
    }

    /** Convierte un heightmap objetivo en colocaciones dentro del disco y la ventana vertical. */
    public static List<Placement> toPlacements(ServerLevel level, LocalHeightmap lh, int[][] target) {
        List<Placement> out = new ArrayList<>();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int ix = 0; ix < lh.size; ix++) {
            for (int iz = 0; iz < lh.size; iz++) {
                if (!lh.hasColumn(ix, iz)) {
                    continue;
                }
                int wx = lh.cx + (ix - lh.radius);
                int wz = lh.cz + (iz - lh.radius);
                int h = target[ix][iz];
                BlockState surf = lh.surface[ix][iz];
                BlockState sub = TerrainUtil.subsurfaceFor(surf);
                for (int y = lh.minY; y <= lh.maxY; y++) {
                    BlockPos pos = new BlockPos(wx, y, wz);
                    boolean solid = !level.getBlockState(pos).isAir();
                    if (y <= h) {
                        if (!solid) {
                            out.add(Placement.of(pos, y == h ? surf : sub));
                        }
                    } else if (solid) {
                        out.add(Placement.of(pos, air));
                    }
                }
            }
        }
        return out;
    }
}
