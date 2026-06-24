package com.fspawner.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * An advanced loot entry: an item with a min/max count range and a drop chance.
 * The full ItemStack NBT is stored so enchantments / custom NBT survive.
 */
public class DropEntry {

    public ItemStack item;
    public int min;
    public int max;
    public float chance; // 0..1

    public DropEntry(ItemStack item, int min, int max, float chance) {
        this.item = item == null ? ItemStack.EMPTY : item;
        this.min = min;
        this.max = max;
        this.chance = chance;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag itemTag = new CompoundTag();
        if (!item.isEmpty()) {
            item.save(itemTag);
        }
        tag.put(FSKeys.DROP_ITEM, itemTag);
        tag.putInt(FSKeys.DROP_MIN, min);
        tag.putInt(FSKeys.DROP_MAX, max);
        tag.putFloat(FSKeys.DROP_CHANCE, chance);
        return tag;
    }

    public static DropEntry load(CompoundTag tag) {
        ItemStack stack = ItemStack.EMPTY;
        if (tag.contains(FSKeys.DROP_ITEM)) {
            CompoundTag itemTag = tag.getCompound(FSKeys.DROP_ITEM);
            if (!itemTag.isEmpty()) {
                stack = ItemStack.of(itemTag);
            }
        }
        return new DropEntry(
                stack,
                tag.getInt(FSKeys.DROP_MIN),
                tag.getInt(FSKeys.DROP_MAX),
                tag.contains(FSKeys.DROP_CHANCE) ? tag.getFloat(FSKeys.DROP_CHANCE) : 1.0f);
    }

    public DropEntry copy() {
        return new DropEntry(item.copy(), min, max, chance);
    }
}
