package com.fantasticterraform.intelligent.dungeon.loot;

import net.minecraft.nbt.CompoundTag;

/**
 * Construye el NBT de un cofre con una loot table real del juego/datapack. El cofre
 * vanilla genera su contenido al abrirse usando estos tags ({@code LootTable} /
 * {@code LootTableSeed}).
 */
public final class DungeonLootAssigner {

    private DungeonLootAssigner() {
    }

    public static CompoundTag chestNbt(String lootTable, long seed) {
        CompoundTag tag = new CompoundTag();
        if (lootTable != null && !lootTable.isEmpty()) {
            tag.putString("LootTable", lootTable);
            tag.putLong("LootTableSeed", seed);
        }
        return tag;
    }
}
