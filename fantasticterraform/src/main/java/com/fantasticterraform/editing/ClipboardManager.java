package com.fantasticterraform.editing;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Mirror;
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
            return toPlacements(origin, rotation, false, false, false, 1);
        }

        /**
         * Pegado con transformacion completa: rotacion (eje Y), espejo en X/Y/Z y escala
         * entera (cada bloque se expande a un cubo scale x scale x scale). El espejo y la
         * rotacion se aplican tambien al estado del bloque (lo que vanilla 1.20.1 permite:
         * rotacion en Y y espejo horizontal; el espejo vertical voltea posiciones y la
         * mitad de losas/escaleras como mejor esfuerzo).
         */
        public List<Placement> toPlacements(BlockPos origin, Rotation rotation,
                                            boolean mirrorX, boolean mirrorY, boolean mirrorZ, int scale) {
            int s = Math.max(1, Math.min(8, scale));
            // Limites para reflejar respecto al volumen.
            int maxX = 0;
            int maxY = 0;
            int maxZ = 0;
            for (Entry e : entries) {
                maxX = Math.max(maxX, e.rel.getX());
                maxY = Math.max(maxY, e.rel.getY());
                maxZ = Math.max(maxZ, e.rel.getZ());
            }
            Mirror mirrorH = Mirror.NONE;
            if (mirrorX && mirrorZ) {
                mirrorH = Mirror.NONE; // doble espejo H ~ rotacion 180; lo cubre la posicion
            } else if (mirrorX) {
                mirrorH = Mirror.FRONT_BACK;
            } else if (mirrorZ) {
                mirrorH = Mirror.LEFT_RIGHT;
            }

            List<Placement> out = new ArrayList<>(entries.size() * s * s * s);
            for (Entry e : entries) {
                int rx = mirrorX ? maxX - e.rel.getX() : e.rel.getX();
                int ry = mirrorY ? maxY - e.rel.getY() : e.rel.getY();
                int rz = mirrorZ ? maxZ - e.rel.getZ() : e.rel.getZ();
                BlockPos r = rotate(new BlockPos(rx, ry, rz), rotation);

                BlockState state = e.state;
                if (mirrorH != Mirror.NONE) {
                    state = state.mirror(mirrorH);
                }
                state = state.rotate(rotation);
                if (mirrorY) {
                    state = flipVertical(state);
                }

                if (s == 1) {
                    out.add(new Placement(origin.offset(r), state, e.blockEntityData));
                } else {
                    BlockPos base = new BlockPos(origin.getX() + r.getX() * s,
                            origin.getY() + r.getY() * s, origin.getZ() + r.getZ() * s);
                    for (int dx = 0; dx < s; dx++) {
                        for (int dy = 0; dy < s; dy++) {
                            for (int dz = 0; dz < s; dz++) {
                                // El NBT solo se aplica al bloque base del cubo escalado.
                                CompoundTag nbt = (dx == 0 && dy == 0 && dz == 0) ? e.blockEntityData : null;
                                out.add(new Placement(base.offset(dx, dy, dz), state, nbt));
                            }
                        }
                    }
                }
            }
            return out;
        }

        /** Mejor esfuerzo para voltear verticalmente losas/escaleras al hacer espejo en Y. */
        private static BlockState flipVertical(BlockState state) {
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE)) {
                net.minecraft.world.level.block.state.properties.SlabType t =
                        state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE);
                if (t == net.minecraft.world.level.block.state.properties.SlabType.TOP) {
                    return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE,
                            net.minecraft.world.level.block.state.properties.SlabType.BOTTOM);
                } else if (t == net.minecraft.world.level.block.state.properties.SlabType.BOTTOM) {
                    return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE,
                            net.minecraft.world.level.block.state.properties.SlabType.TOP);
                }
            }
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF)) {
                net.minecraft.world.level.block.state.properties.Half h =
                        state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF);
                return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF,
                        h == net.minecraft.world.level.block.state.properties.Half.TOP
                                ? net.minecraft.world.level.block.state.properties.Half.BOTTOM
                                : net.minecraft.world.level.block.state.properties.Half.TOP);
            }
            return state;
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
