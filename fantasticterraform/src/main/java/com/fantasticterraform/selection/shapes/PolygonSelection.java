package com.fantasticterraform.selection.shapes;

import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.selection.SelectionType;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Poligono: prisma vertical. La base es el poligono 2D formado por los vertices
 * proyectados en el plano XZ; la altura va del Y minimo al Y maximo de los vertices.
 * {@code contains} resuelve point-in-polygon mediante ray casting 2D y comprueba el
 * rango de altura.
 */
public final class PolygonSelection implements SelectionShape {

    private final double[] vx;
    private final double[] vz;
    private final int minY;
    private final int maxY;
    private final BlockPos min;
    private final BlockPos max;
    private final double area2d;

    public PolygonSelection(List<BlockPos> vertices) {
        int n = vertices.size();
        this.vx = new double[n];
        this.vz = new double[n];
        int lowY = Integer.MAX_VALUE;
        int highY = Integer.MIN_VALUE;
        int lowX = Integer.MAX_VALUE;
        int highX = Integer.MIN_VALUE;
        int lowZ = Integer.MAX_VALUE;
        int highZ = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            BlockPos v = vertices.get(i);
            // Centro del bloque para que el test point-in-polygon sea consistente.
            vx[i] = v.getX() + 0.5D;
            vz[i] = v.getZ() + 0.5D;
            lowY = Math.min(lowY, v.getY());
            highY = Math.max(highY, v.getY());
            lowX = Math.min(lowX, v.getX());
            highX = Math.max(highX, v.getX());
            lowZ = Math.min(lowZ, v.getZ());
            highZ = Math.max(highZ, v.getZ());
        }
        this.minY = lowY;
        this.maxY = highY;
        this.min = new BlockPos(lowX, lowY, lowZ);
        this.max = new BlockPos(highX, highY, highZ);
        this.area2d = Math.abs(shoelace(vx, vz));
    }

    private static double shoelace(double[] xs, double[] zs) {
        double sum = 0.0D;
        int n = xs.length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            sum += xs[i] * zs[j] - xs[j] * zs[i];
        }
        return sum / 2.0D;
    }

    @Override
    public boolean contains(BlockPos pos) {
        if (pos.getY() < minY || pos.getY() > maxY) {
            return false;
        }
        return pointInPolygon(pos.getX() + 0.5D, pos.getZ() + 0.5D);
    }

    /** Algoritmo de ray casting (crossing number) sobre el plano XZ. */
    private boolean pointInPolygon(double px, double pz) {
        boolean inside = false;
        int n = vx.length;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            boolean crosses = (vz[i] > pz) != (vz[j] > pz);
            if (crosses) {
                double slope = (px - vx[i]) * (vz[j] - vz[i]) - (vx[j] - vx[i]) * (pz - vz[i]);
                if (vz[j] < vz[i]) {
                    slope = -slope;
                }
                if (slope > 0.0D) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    public List<double[]> vertices2d() {
        List<double[]> out = new ArrayList<>(vx.length);
        for (int i = 0; i < vx.length; i++) {
            out.add(new double[] {vx[i], vz[i]});
        }
        return out;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
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
        long h = (long) (maxY - minY) + 1L;
        return Math.round(area2d) * h;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.POLYGON;
    }
}
