package com.fantasticterraform.editing.shapes;

import net.minecraft.core.BlockPos;

/** Esfera de relleno centrada en un punto con radio dado. */
public final class SphereShape implements FillShape {

    private final BlockPos center;
    private final double radiusSq;
    private final BlockPos min;
    private final BlockPos max;

    public SphereShape(BlockPos center, double radius) {
        this.center = center;
        this.radiusSq = radius * radius;
        int r = (int) Math.ceil(radius);
        this.min = center.offset(-r, -r, -r);
        this.max = center.offset(r, r, r);
    }

    @Override
    public boolean contains(BlockPos pos) {
        double dx = pos.getX() - center.getX();
        double dy = pos.getY() - center.getY();
        double dz = pos.getZ() - center.getZ();
        return (dx * dx + dy * dy + dz * dz) <= radiusSq + 1.0E-6D;
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
