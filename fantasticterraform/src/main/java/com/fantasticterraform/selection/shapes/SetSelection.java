package com.fantasticterraform.selection.shapes;

import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.selection.SelectionType;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Seleccion explicita respaldada por un conjunto de posiciones (resultado de un
 * flood-fill "smart"). A diferencia de las formas parametricas, su volumen es el
 * numero real de bloques del conjunto y {@code contains} es una pertenencia O(1).
 * Es totalmente compatible con todas las operaciones (que recorren el bounding box y
 * filtran por {@code contains}).
 */
public final class SetSelection implements SelectionShape {

    private final Set<Long> packed;
    private final BlockPos min;
    private final BlockPos max;

    public SetSelection(Set<BlockPos> positions) {
        this.packed = new HashSet<>(Math.max(16, positions.size() * 2));
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos p : positions) {
            packed.add(p.asLong());
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        if (positions.isEmpty()) {
            minX = minY = minZ = 0;
            maxX = maxY = maxZ = 0;
        }
        this.min = new BlockPos(minX, minY, minZ);
        this.max = new BlockPos(maxX, maxY, maxZ);
    }

    /** Devuelve una copia trasladada del conjunto. */
    public SetSelection translate(int dx, int dy, int dz) {
        Set<BlockPos> moved = new HashSet<>(packed.size() * 2);
        for (long l : packed) {
            BlockPos p = BlockPos.of(l);
            moved.add(new BlockPos(p.getX() + dx, p.getY() + dy, p.getZ() + dz));
        }
        return new SetSelection(moved);
    }

    @Override
    public boolean contains(BlockPos pos) {
        return packed.contains(pos.asLong());
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
        return packed.size();
    }

    @Override
    public SelectionType getType() {
        return SelectionType.SMART;
    }
}
