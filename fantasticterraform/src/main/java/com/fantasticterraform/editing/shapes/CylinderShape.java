package com.fantasticterraform.editing.shapes;

import net.minecraft.core.BlockPos;

/** Cilindro de relleno: centro de la base, radio en XZ y altura hacia arriba. */
public final class CylinderShape implements FillShape {

    private final BlockPos base;
    private final double radiusSq;
    private final int top;
    private final BlockPos min;
    private final BlockPos max;

    public CylinderShape(BlockPos base, double radius, int height) {
        this.base = base;
        this.radiusSq = radius * radius;
        int h = Math.max(1, height);
        this.top = base.getY() + h - 1;
        int r = (int) Math.ceil(radius);
        this.min = new BlockPos(base.getX() - r, base.getY(), base.getZ() - r);
        this.max = new BlockPos(base.getX() + r, top, base.getZ() + r);
    }

    @Override
    public boolean contains(BlockPos pos) {
        if (pos.getY() < base.getY() || pos.getY() > top) {
            return false;
        }
        double dx = pos.getX() - base.getX();
        double dz = pos.getZ() - base.getZ();
        return (dx * dx + dz * dz) <= radiusSq + 1.0E-6D;
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
