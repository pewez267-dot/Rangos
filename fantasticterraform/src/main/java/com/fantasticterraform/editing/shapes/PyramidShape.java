package com.fantasticterraform.editing.shapes;

import net.minecraft.core.BlockPos;

/**
 * Piramide de base cuadrada. La semi-anchura disminuye linealmente con la altura:
 * en la base vale {@code size} y en la cima 0. Puede invertirse.
 */
public final class PyramidShape implements FillShape {

    private final BlockPos base;
    private final int size;
    private final int height;
    private final boolean inverted;
    private final BlockPos min;
    private final BlockPos max;

    public PyramidShape(BlockPos base, int size, int height, boolean inverted) {
        this.base = base;
        this.size = Math.max(0, size);
        this.height = Math.max(1, height);
        this.inverted = inverted;
        this.min = new BlockPos(base.getX() - this.size, base.getY(), base.getZ() - this.size);
        this.max = new BlockPos(base.getX() + this.size, base.getY() + this.height - 1, base.getZ() + this.size);
    }

    @Override
    public boolean contains(BlockPos pos) {
        int layer = pos.getY() - base.getY();
        if (layer < 0 || layer >= height) {
            return false;
        }
        // Fraccion vertical: 0 en la base, 1 en la cima.
        double t = (double) layer / (double) height;
        double frac = inverted ? t : (1.0D - t);
        int halfWidth = (int) Math.round(size * frac);
        int dx = Math.abs(pos.getX() - base.getX());
        int dz = Math.abs(pos.getZ() - base.getZ());
        return dx <= halfWidth && dz <= halfWidth;
    }

    @Override
    public BlockPos getMin() {
        return min;
    }

    @Override
    public BlockPos getMax() {
        return max;
    }
}
