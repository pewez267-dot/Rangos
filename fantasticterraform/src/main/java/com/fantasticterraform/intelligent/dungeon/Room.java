package com.fantasticterraform.intelligent.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/**
 * Una sala de la dungeon, posicionada en coordenadas de mundo. {@code min} es la
 * esquina inferior; {@code sizeX/sizeY/sizeZ} el tamano interior+muros. {@code level}
 * es el nivel vertical al que pertenece (para multi-nivel).
 */
public final class Room {

    public enum Shape {
        RECT,
        CIRCLE
    }

    public final BlockPos min;
    public final int sizeX;
    public final int sizeY;
    public final int sizeZ;
    public RoomType type;
    public final Shape shape;
    public final int level;

    public Room(BlockPos min, int sizeX, int sizeY, int sizeZ, RoomType type, Shape shape, int level) {
        this.min = min;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.type = type;
        this.shape = shape;
        this.level = level;
    }

    public int maxX() {
        return min.getX() + sizeX - 1;
    }

    public int maxY() {
        return min.getY() + sizeY - 1;
    }

    public int maxZ() {
        return min.getZ() + sizeZ - 1;
    }

    public BlockPos center() {
        return new BlockPos(min.getX() + sizeX / 2, min.getY() + sizeY / 2, min.getZ() + sizeZ / 2);
    }

    /** Caja con margen para comprobar solapamientos durante el packing. */
    public AABB boundsWithMargin(int margin) {
        return new AABB(
                min.getX() - margin, min.getY() - margin, min.getZ() - margin,
                maxX() + 1 + margin, maxY() + 1 + margin, maxZ() + 1 + margin);
    }
}
