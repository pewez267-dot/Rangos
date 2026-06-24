package com.fantasticterraform.selection.shapes;

import com.fantasticterraform.selection.ConvexHull3D;
import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.selection.SelectionType;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Freehand / Convex Hull: forma irregular definida por el envolvente convexo 3D de
 * N puntos arbitrarios. {@code contains} delega en {@link ConvexHull3D}, que
 * comprueba la pertenencia exacta al poliedro (interseccion de semiespacios), nunca
 * una aproximacion cuboide.
 */
public final class ConvexHullSelection implements SelectionShape {

    private final ConvexHull3D hull;
    private final BlockPos min;
    private final BlockPos max;
    private final boolean valid;
    private long cachedVolume = -1L;

    public ConvexHullSelection(List<BlockPos> points) {
        double[][] arr = new double[points.size()][3];
        int lowX = Integer.MAX_VALUE;
        int lowY = Integer.MAX_VALUE;
        int lowZ = Integer.MAX_VALUE;
        int highX = Integer.MIN_VALUE;
        int highY = Integer.MIN_VALUE;
        int highZ = Integer.MIN_VALUE;
        for (int i = 0; i < points.size(); i++) {
            BlockPos p = points.get(i);
            arr[i][0] = p.getX() + 0.5D;
            arr[i][1] = p.getY() + 0.5D;
            arr[i][2] = p.getZ() + 0.5D;
            lowX = Math.min(lowX, p.getX());
            lowY = Math.min(lowY, p.getY());
            lowZ = Math.min(lowZ, p.getZ());
            highX = Math.max(highX, p.getX());
            highY = Math.max(highY, p.getY());
            highZ = Math.max(highZ, p.getZ());
        }
        this.hull = new ConvexHull3D(arr);
        this.valid = hull.isValid();
        this.min = new BlockPos(lowX, lowY, lowZ);
        this.max = new BlockPos(highX, highY, highZ);
    }

    public boolean isValid() {
        return valid;
    }

    public ConvexHull3D hull() {
        return hull;
    }

    @Override
    public boolean contains(BlockPos pos) {
        return hull.contains(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    @Override
    public BlockPos getMin() {
        return min;
    }

    @Override
    public BlockPos getMax() {
        return max;
    }

    @Override
    public long getVolume() {
        if (cachedVolume >= 0) {
            return cachedVolume;
        }
        long bbox = (long) (max.getX() - min.getX() + 1)
                * (long) (max.getY() - min.getY() + 1)
                * (long) (max.getZ() - min.getZ() + 1);
        // Para bounding boxes manejables contamos el volumen real (cacheado).
        // Para cajas enormes devolvemos el bbox como cota superior (la operacion sera
        // rechazada igualmente por el limite de volumen).
        if (bbox > 4_000_000L) {
            cachedVolume = bbox;
            return cachedVolume;
        }
        long count = 0;
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (hull.contains(x + 0.5D, y + 0.5D, z + 0.5D)) {
                        count++;
                    }
                }
            }
        }
        cachedVolume = count;
        return cachedVolume;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.CONVEX_HULL;
    }
}
