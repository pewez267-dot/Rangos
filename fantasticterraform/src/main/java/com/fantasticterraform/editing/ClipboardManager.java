package com.fantasticterraform.editing;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Buffer en memoria por jugador para copiar/pegar respetando la forma real de la
 * seleccion (no solo su bounding box). El pegado admite rotacion en incrementos de
 * 90 grados aplicada tanto a las posiciones relativas como al estado de cada bloque.
 */
public final class ClipboardManager {

    private static final Map<UUID, Clipboard> CLIPBOARDS = new ConcurrentHashMap<>();

    private ClipboardManager() {
    }

    public static void set(UUID id, Clipboard clipboard) {
        CLIPBOARDS.put(id, clipboard);
    }

    public static Clipboard get(UUID id) {
        return CLIPBOARDS.get(id);
    }

    public static void remove(UUID id) {
        CLIPBOARDS.remove(id);
    }

    /** Una entrada copiada: posicion relativa al origen + estado + NBT de block entity. */
    public static final class Entry {
        public final BlockPos rel;
        public final BlockState state;
        public final CompoundTag blockEntityData;

        public Entry(BlockPos rel, BlockState state, CompoundTag blockEntityData) {
            this.rel = rel;
            this.state = state;
            this.blockEntityData = blockEntityData;
        }
    }

    public static final class Clipboard {
        public final List<Entry> entries;

        public Clipboard(List<Entry> entries) {
            this.entries = entries;
        }

        public int size() {
            return entries.size();
        }

        /**
         * Genera la lista de colocaciones para pegar en {@code origin} con la rotacion
         * indicada. La rotacion se aplica a la posicion relativa (alrededor del eje Y)
         * y al propio estado del bloque.
         */
        public List<Placement> toPlacements(BlockPos origin, Rotation rotation) {
            List<Placement> out = new ArrayList<>(entries.size());
            for (Entry e : entries) {
                BlockPos r = rotate(e.rel, rotation);
                BlockPos world = origin.offset(r);
                BlockState state = e.state.rotate(rotation);
                out.add(new Placement(world, state, e.blockEntityData));
            }
            return out;
        }

        /** Rotacion de una posicion relativa alrededor del eje Y, igual a StructureTemplate. */
        private static BlockPos rotate(BlockPos rel, Rotation rotation) {
            int x = rel.getX();
            int y = rel.getY();
            int z = rel.getZ();
            switch (rotation) {
                case CLOCKWISE_90:
                    return new BlockPos(-z, y, x);
                case CLOCKWISE_180:
                    return new BlockPos(-x, y, -z);
                case COUNTERCLOCKWISE_90:
                    return new BlockPos(z, y, -x);
                case NONE:
                default:
                    return new BlockPos(x, y, z);
            }
        }
    }
}
