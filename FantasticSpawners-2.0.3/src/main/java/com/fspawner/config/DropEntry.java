// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.config;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class DropEntry
{
    public ItemStack item;
    public int min;
    public int max;
    public float chance;
    
    public DropEntry(final ItemStack item, final int min, final int max, final float chance) {
        this.item = ((item == null) ? ItemStack.EMPTY : item);
        this.min = min;
        this.max = max;
        this.chance = chance;
    }
    
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        final CompoundTag itemTag = new CompoundTag();
        if (!this.item.isEmpty()) {
            this.item.save(itemTag);
        }
        tag.put("item", (Tag)itemTag);
        tag.putInt("min", this.min);
        tag.putInt("max", this.max);
        tag.putFloat("chance", this.chance);
        return tag;
    }
    
    public static DropEntry load(final CompoundTag tag) {
        ItemStack stack = ItemStack.EMPTY;
        if (tag.contains("item")) {
            final CompoundTag itemTag = tag.getCompound("item");
            if (!itemTag.isEmpty()) {
                stack = ItemStack.of(itemTag);
            }
        }
        return new DropEntry(stack, tag.getInt("min"), tag.getInt("max"), tag.contains("chance") ? tag.getFloat("chance") : 1.0f);
    }
    
    public DropEntry copy() {
        return new DropEntry(this.item.copy(), this.min, this.max, this.chance);
    }
}
