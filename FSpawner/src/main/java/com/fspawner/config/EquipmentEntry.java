package com.fspawner.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * An equipment slot configuration: the item to equip, the chance it drops on
 * death (vanilla drop chance) and the chance it actually appears on spawn.
 */
public class EquipmentEntry {

    public EquipmentSlot slot;
    public ItemStack item;
    public float dropChance;   // 0..1 vanilla drop chance
    public float appearChance; // 0..1 probability the item is equipped at all

    public EquipmentEntry(EquipmentSlot slot, ItemStack item, float dropChance, float appearChance) {
        this.slot = slot;
        this.item = item == null ? ItemStack.EMPTY : item;
        this.dropChance = dropChance;
        this.appearChance = appearChance;
    }

    public EquipmentEntry(EquipmentSlot slot) {
        this(slot, ItemStack.EMPTY, 0.085f, 1.0f);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(FSKeys.EQ_SLOT, slot.getName());
        CompoundTag itemTag = new CompoundTag();
        if (!item.isEmpty()) {
            item.save(itemTag);
        }
        tag.put(FSKeys.EQ_ITEM, itemTag);
        tag.putFloat(FSKeys.EQ_DROP_CHANCE, dropChance);
        tag.putFloat(FSKeys.EQ_APPEAR_CHANCE, appearChance);
        return tag;
    }

    public static EquipmentEntry load(CompoundTag tag) {
        EquipmentSlot slot = slotByName(tag.getString(FSKeys.EQ_SLOT));
        ItemStack stack = ItemStack.EMPTY;
        if (tag.contains(FSKeys.EQ_ITEM)) {
            CompoundTag itemTag = tag.getCompound(FSKeys.EQ_ITEM);
            if (!itemTag.isEmpty()) {
                stack = ItemStack.of(itemTag);
            }
        }
        float drop = tag.contains(FSKeys.EQ_DROP_CHANCE) ? tag.getFloat(FSKeys.EQ_DROP_CHANCE) : 0.085f;
        float appear = tag.contains(FSKeys.EQ_APPEAR_CHANCE) ? tag.getFloat(FSKeys.EQ_APPEAR_CHANCE) : 1.0f;
        return new EquipmentEntry(slot, stack, drop, appear);
    }

    public static EquipmentSlot slotByName(String name) {
        for (EquipmentSlot s : EquipmentSlot.values()) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        return EquipmentSlot.MAINHAND;
    }

    public EquipmentEntry copy() {
        return new EquipmentEntry(slot, item.copy(), dropChance, appearChance);
    }
}
