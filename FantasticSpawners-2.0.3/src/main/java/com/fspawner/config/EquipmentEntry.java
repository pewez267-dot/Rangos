// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.config;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;

public class EquipmentEntry
{
    public EquipmentSlot slot;
    public ItemStack item;
    public float dropChance;
    public float appearChance;
    
    public EquipmentEntry(final EquipmentSlot slot, final ItemStack item, final float dropChance, final float appearChance) {
        this.slot = slot;
        this.item = ((item == null) ? ItemStack.EMPTY : item);
        this.dropChance = dropChance;
        this.appearChance = appearChance;
    }
    
    public EquipmentEntry(final EquipmentSlot slot) {
        this(slot, ItemStack.EMPTY, 0.085f, 1.0f);
    }
    
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("slot", this.slot.getName());
        final CompoundTag itemTag = new CompoundTag();
        if (!this.item.isEmpty()) {
            this.item.save(itemTag);
        }
        tag.put("item", (Tag)itemTag);
        tag.putFloat("dropChance", this.dropChance);
        tag.putFloat("appearChance", this.appearChance);
        return tag;
    }
    
    public static EquipmentEntry load(final CompoundTag tag) {
        final EquipmentSlot slot = slotByName(tag.getString("slot"));
        ItemStack stack = ItemStack.EMPTY;
        if (tag.contains("item")) {
            final CompoundTag itemTag = tag.getCompound("item");
            if (!itemTag.isEmpty()) {
                stack = ItemStack.of(itemTag);
            }
        }
        final float drop = tag.contains("dropChance") ? tag.getFloat("dropChance") : 0.085f;
        final float appear = tag.contains("appearChance") ? tag.getFloat("appearChance") : 1.0f;
        return new EquipmentEntry(slot, stack, drop, appear);
    }
    
    public static EquipmentSlot slotByName(final String name) {
        for (final EquipmentSlot s : EquipmentSlot.values()) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        return EquipmentSlot.MAINHAND;
    }
    
    public EquipmentEntry copy() {
        return new EquipmentEntry(this.slot, this.item.copy(), this.dropChance, this.appearChance);
    }
}
