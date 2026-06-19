package com.fantasticterraform.intelligent.dungeon;

import com.fantasticterraform.intelligent.dungeon.multilevel.LevelNode;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Coloca habitaciones dentro del volumen de la seleccion sin solaparse, repartidas en
 * bandas verticales (niveles). Usa intentos aleatorios; si no caben todas tras el
 * maximo de intentos, coloca las que pudo (reduccion automatica, nunca solapa ni se
 * sale de la seleccion).
 */
public final class RoomPacker {

    private RoomPacker() {
    }

    public static List<LevelNode> buildLevels(SelectionShape sel, int levels) {
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        int totalH = max.getY() - min.getY() + 1;
        int n = Math.max(1, levels);
        int band = Math.max(8, totalH / n);
        List<LevelNode> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int yMin = min.getY() + i * band;
            int yMax = (i == n - 1) ? max.getY() : (yMin + band - 1);
            if (yMin >= max.getY()) {
                break;
            }
            out.add(new LevelNode(i, yMin, Math.min(yMax, max.getY())));
        }
        return out;
    }

    public static List<Room> pack(SelectionShape sel, int desiredRooms, int levels, long seed, int maxAttempts,
                                  int minSize, int maxSize) {
        RandomSource rnd = RandomSource.create(seed);
        List<LevelNode> bands = buildLevels(sel, levels);
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        int bboxW = max.getX() - min.getX() + 1;
        int bboxL = max.getZ() - min.getZ() + 1;

        List<Room> placed = new ArrayList<>();
        int attempts = 0;
        int placedCount = 0;
        while (placedCount < desiredRooms && attempts < maxAttempts) {
            attempts++;
            LevelNode band = bands.get(rnd.nextInt(bands.size()));

            int sizeX = minSize + rnd.nextInt(Math.max(1, Math.min(maxSize - minSize + 1, bboxW - 6)));
            int sizeZ = minSize + rnd.nextInt(Math.max(1, Math.min(maxSize - minSize + 1, bboxL - 6)));
            int maxRoomH = Math.max(5, Math.min(14, band.height() - 2));
            int sizeY = 5 + rnd.nextInt(Math.max(1, maxRoomH - 4));

            int x = min.getX() + 1 + rnd.nextInt(Math.max(1, bboxW - sizeX - 2));
            int z = min.getZ() + 1 + rnd.nextInt(Math.max(1, bboxL - sizeZ - 2));
            int y = band.yMin + 1;

            BlockPos roomMin = new BlockPos(x, y, z);
            Room.Shape shape = rnd.nextInt(3) == 0 ? Room.Shape.CIRCLE : Room.Shape.RECT;
            Room room = new Room(roomMin, sizeX, sizeY, sizeZ, RoomType.NORMAL, shape, band.level);

            if (!fitsInSelection(sel, room) || overlaps(placed, room)) {
                continue;
            }
            placed.add(room);
            placedCount++;
        }
        return placed;
    }

    private static boolean fitsInSelection(SelectionShape sel, Room room) {
        // Las 8 esquinas y el centro deben estar dentro de la geometria real de la seleccion.
        int[] xs = {room.min.getX(), room.maxX()};
        int[] ys = {room.min.getY(), room.maxY()};
        int[] zs = {room.min.getZ(), room.maxZ()};
        for (int x : xs) {
            for (int y : ys) {
                for (int z : zs) {
                    if (!sel.contains(new BlockPos(x, y, z))) {
                        return false;
                    }
                }
            }
        }
        return sel.contains(room.center());
    }

    private static boolean overlaps(List<Room> placed, Room room) {
        AABB a = room.boundsWithMargin(2);
        for (Room other : placed) {
            if (a.intersects(other.boundsWithMargin(0))) {
                return true;
            }
        }
        return false;
    }
}
