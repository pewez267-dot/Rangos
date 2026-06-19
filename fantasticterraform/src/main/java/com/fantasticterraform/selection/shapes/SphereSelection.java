package com.fantasticterraform.selection.shapes;

import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.selection.SelectionType;
import net.minecraft.core.BlockPos;

/**
 * Esfera: P1 es el centro, P2 un punto en la superficie. El radio es la distancia
 * euclidiana entre ambos. {@code contains} comprueba distancia al cuadrado <= radio
 * al cuadrado, de modo que jamas rellena el cubo completo.
 */
public final class SphereSelection implements SelectionShape {

    private final BlockPos center;
    private final double radius;
    private final double radiusSq;
    private final BlockPos min;
    private final BlockPos max;

    public SphereSelection(BlockPos center, BlockPos surface) {
        this.center = center;
        double dx = surface.getX() - center.getX();
        double dy = surface.getY() - center.getY();
        double dz = surface.getZ() - center.getZ();
        this.radius = Math.sqrt(dx * dx + dy * dy + dz * dz);
        this.radiusSq = this.radius * this.radius;
        int r = (int) Math.ceil(this.radius);
        this.min = new BlockPos(center.getX() - r, center.getY() - r, center.getZ() - r);
        this.max = new BlockPos(center.getX() + r, center.getY() + r, center.getZ() + r);
    }

    public double radius() {
        return radius;
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

    @Override
    public long getVolume() {
        return Math.round((4.0D / 3.0D) * Math.PI * radius * radius * radius);
    }

    @Override
    public SelectionType getType() {
        return SelectionType.SPHERE;
    }
}
