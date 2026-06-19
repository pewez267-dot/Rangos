package com.fantasticterraform.selection.shapes;

import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.selection.SelectionType;
import net.minecraft.core.BlockPos;

/**
 * Cuboide: bounding box entre dos puntos. {@code contains} es una comparacion de
 * rango simple en X/Y/Z.
 */
public final class CuboidSelection implements SelectionShape {

    private final BlockPos min;
    private final BlockPos max;

    public CuboidSelection(BlockPos a, BlockPos b) {
        this.min = new BlockPos(
                Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ()));
        this.max = new BlockPos(
                Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ()));
    }

    @Override
    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
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
        long dx = (long) (max.getX() - min.getX()) + 1L;
        long dy = (long) (max.getY() - min.getY()) + 1L;
        long dz = (long) (max.getZ() - min.getZ()) + 1L;
        return dx * dy * dz;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.CUBOID;
    }
}
