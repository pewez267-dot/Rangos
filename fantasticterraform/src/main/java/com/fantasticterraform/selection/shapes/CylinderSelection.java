package com.fantasticterraform.selection.shapes;

import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.selection.SelectionType;
import net.minecraft.core.BlockPos;

/**
 * Cilindro: P1 = centro de la base, P2 = punto en el borde de la base (define el
 * radio en XZ). La altura se ajusta desde el HUD. {@code contains} comprueba la
 * distancia 2D (XZ) al eje central <= radio y que Y este dentro del rango vertical.
 */
public final class CylinderSelection implements SelectionShape {

    private final BlockPos base;
    private final double radius;
    private final double radiusSq;
    private final int height;
    private final BlockPos min;
    private final BlockPos max;

    public CylinderSelection(BlockPos base, BlockPos edge, int height) {
        this.base = base;
        double dx = edge.getX() - base.getX();
        double dz = edge.getZ() - base.getZ();
        this.radius = Math.sqrt(dx * dx + dz * dz);
        this.radiusSq = this.radius * this.radius;
        this.height = Math.max(1, height);
        int r = (int) Math.ceil(this.radius);
        int topY = base.getY() + this.height - 1;
        this.min = new BlockPos(base.getX() - r, Math.min(base.getY(), topY), base.getZ() - r);
        this.max = new BlockPos(base.getX() + r, Math.max(base.getY(), topY), base.getZ() + r);
    }

    public double radius() {
        return radius;
    }

    public int height() {
        return height;
    }

    @Override
    public boolean contains(BlockPos pos) {
        if (pos.getY() < min.getY() || pos.getY() > max.getY()) {
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

    @Override
    public long getVolume() {
        return Math.round(Math.PI * radius * radius * height);
    }

    @Override
    public SelectionType getType() {
        return SelectionType.CYLINDER;
    }
}
