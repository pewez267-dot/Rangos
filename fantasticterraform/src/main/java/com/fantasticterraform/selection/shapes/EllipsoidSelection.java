package com.fantasticterraform.selection.shapes;

import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.selection.SelectionType;
import net.minecraft.core.BlockPos;

/**
 * Elipsoide: radios independientes en X, Y, Z derivados del bounding box entre P1
 * (centro) y P2 (esquina del bounding box). {@code contains} usa la distancia
 * normalizada por radio en cada eje (suma de cuadrados normalizados <= 1).
 */
public final class EllipsoidSelection implements SelectionShape {

    private final BlockPos center;
    private final double rx;
    private final double ry;
    private final double rz;
    private final BlockPos min;
    private final BlockPos max;

    public EllipsoidSelection(BlockPos center, BlockPos corner) {
        this.center = center;
        this.rx = Math.max(0.5D, Math.abs(corner.getX() - center.getX()));
        this.ry = Math.max(0.5D, Math.abs(corner.getY() - center.getY()));
        this.rz = Math.max(0.5D, Math.abs(corner.getZ() - center.getZ()));
        int ix = (int) Math.ceil(rx);
        int iy = (int) Math.ceil(ry);
        int iz = (int) Math.ceil(rz);
        this.min = new BlockPos(center.getX() - ix, center.getY() - iy, center.getZ() - iz);
        this.max = new BlockPos(center.getX() + ix, center.getY() + iy, center.getZ() + iz);
    }

    public double radiusX() {
        return rx;
    }

    public double radiusY() {
        return ry;
    }

    public double radiusZ() {
        return rz;
    }

    @Override
    public boolean contains(BlockPos pos) {
        double nx = (pos.getX() - center.getX()) / rx;
        double ny = (pos.getY() - center.getY()) / ry;
        double nz = (pos.getZ() - center.getZ()) / rz;
        return (nx * nx + ny * ny + nz * nz) <= 1.0D + 1.0E-6D;
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
        return Math.round((4.0D / 3.0D) * Math.PI * rx * ry * rz);
    }

    @Override
    public SelectionType getType() {
        return SelectionType.ELLIPSOID;
    }
}
