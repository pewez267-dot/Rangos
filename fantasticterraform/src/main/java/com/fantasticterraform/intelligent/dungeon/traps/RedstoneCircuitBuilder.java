package com.fantasticterraform.intelligent.dungeon.traps;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Utilidades para construir cableado de redstone real y valido para las trampas: lineas
 * de polvo de redstone y NBT de dispensadores/spawners. El polvo se coloca sobre bloques
 * solidos (piso de la sala) y conecta el trigger con el mecanismo siguiendo las reglas
 * nativas de redstone (el polvo adyacente a un dispensador lo alimenta; una placa de
 * presion alimenta el polvo adyacente).
 */
public final class RedstoneCircuitBuilder {

    private RedstoneCircuitBuilder() {
    }

    /** Linea recta de polvo de redstone entre dos puntos del mismo Y (primero X, luego Z). */
    public static void dustLine(List<Placement> out, BlockPos from, BlockPos to) {
        int y = from.getY();
        int x = from.getX();
        int z = from.getZ();
        int sx = Integer.signum(to.getX() - x);
        while (x != to.getX()) {
            out.add(Placement.of(new BlockPos(x, y, z), Blocks.REDSTONE_WIRE.defaultBlockState()));
            x += sx;
        }
        int sz = Integer.signum(to.getZ() - z);
        while (z != to.getZ()) {
            out.add(Placement.of(new BlockPos(x, y, z), Blocks.REDSTONE_WIRE.defaultBlockState()));
            z += sz;
        }
        out.add(Placement.of(new BlockPos(to.getX(), y, to.getZ()), Blocks.REDSTONE_WIRE.defaultBlockState()));
    }

    /** NBT de inventario para un dispensador con un item en el primer slot. */
    public static CompoundTag dispenserItems(String itemId, int count) {
        CompoundTag tag = new CompoundTag();
        ListTag items = new ListTag();
        CompoundTag slot = new CompoundTag();
        slot.putByte("Slot", (byte) 0);
        slot.putString("id", itemId);
        slot.putByte("Count", (byte) count);
        items.add(slot);
        tag.put("Items", items);
        return tag;
    }

    /** NBT de un spawner que genera el mob indicado. */
    public static CompoundTag spawnerData(String entityId) {
        CompoundTag root = new CompoundTag();
        CompoundTag spawnData = new CompoundTag();
        CompoundTag entity = new CompoundTag();
        entity.putString("id", entityId);
        spawnData.put("entity", entity);
        root.put("SpawnData", spawnData);
        root.putShort("MinSpawnDelay", (short) 200);
        root.putShort("MaxSpawnDelay", (short) 800);
        root.putShort("SpawnCount", (short) 4);
        root.putShort("MaxNearbyEntities", (short) 6);
        root.putShort("RequiredPlayerRange", (short) 16);
        return root;
    }
}
