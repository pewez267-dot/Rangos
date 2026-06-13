// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.command;

import net.minecraft.nbt.Tag;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import java.util.HashMap;
import net.minecraft.nbt.CompoundTag;
import java.util.Map;
import net.minecraft.world.level.saveddata.SavedData;

public class FSpawnerPresets extends SavedData
{
    public static final String NAME = "fspawner_presets";
    private final Map<String, CompoundTag> presets;
    
    public FSpawnerPresets() {
        this.presets = new HashMap<String, CompoundTag>();
    }
    
    public static FSpawnerPresets get(final ServerLevel anyLevel) {
        final ServerLevel overworld = anyLevel.getServer().overworld();
        return (FSpawnerPresets)overworld.getDataStorage().computeIfAbsent((Function)FSpawnerPresets::load, (Supplier)FSpawnerPresets::new, "fspawner_presets");
    }
    
    public void put(final String name, final CompoundTag config) {
        this.presets.put(name.toLowerCase(), config.copy());
        this.setDirty();
    }
    
    public CompoundTag get(final String name) {
        final CompoundTag tag = this.presets.get(name.toLowerCase());
        return (tag == null) ? null : tag.copy();
    }
    
    public boolean remove(final String name) {
        final boolean removed = this.presets.remove(name.toLowerCase()) != null;
        if (removed) {
            this.setDirty();
        }
        return removed;
    }
    
    public Set<String> names() {
        return this.presets.keySet();
    }
    
    public static FSpawnerPresets load(final CompoundTag tag) {
        final FSpawnerPresets data = new FSpawnerPresets();
        final CompoundTag stored = tag.getCompound("presets");
        for (final String key : stored.getAllKeys()) {
            data.presets.put(key, stored.getCompound(key));
        }
        return data;
    }
    
    public CompoundTag save(final CompoundTag tag) {
        final CompoundTag stored = new CompoundTag();
        for (final Map.Entry<String, CompoundTag> e : this.presets.entrySet()) {
            stored.put((String)e.getKey(), (Tag)e.getValue());
        }
        tag.put("presets", (Tag)stored);
        return tag;
    }
}
