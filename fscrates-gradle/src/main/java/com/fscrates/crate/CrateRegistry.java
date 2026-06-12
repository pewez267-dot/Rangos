// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.crate;

import net.minecraft.nbt.Tag;
import java.util.Iterator;
import java.util.Set;
import com.fscrates.config.CrateConfig;
import java.util.function.Supplier;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import java.util.HashMap;
import net.minecraft.nbt.CompoundTag;
import java.util.Map;
import net.minecraft.world.level.saveddata.SavedData;

public class CrateRegistry extends SavedData
{
    public static final String NAME = "fscrates_definitions";
    private final Map<String, CompoundTag> crates;
    
    public CrateRegistry() {
        this.crates = new HashMap<String, CompoundTag>();
    }
    
    public static CrateRegistry get(final ServerLevel anyLevel) {
        final ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CrateRegistry::new, CrateRegistry::load), "fscrates_definitions");
    }
    
    public void put(final CrateConfig crate) {
        this.crates.put(crate.id.toLowerCase(), crate.save());
        this.setDirty();
    }
    
    public CrateConfig get(final String id) {
        final CompoundTag tag = this.crates.get(id.toLowerCase());
        return (tag == null) ? null : CrateConfig.load(tag);
    }
    
    public boolean exists(final String id) {
        return this.crates.containsKey(id.toLowerCase());
    }
    
    public boolean remove(final String id) {
        final boolean removed = this.crates.remove(id.toLowerCase()) != null;
        if (removed) {
            this.setDirty();
        }
        return removed;
    }
    
    public Set<String> ids() {
        return this.crates.keySet();
    }
    
    public static CrateRegistry load(final CompoundTag tag) {
        final CrateRegistry data = new CrateRegistry();
        final CompoundTag stored = tag.getCompound("crates");
        for (final String key : stored.getAllKeys()) {
            data.crates.put(key, stored.getCompound(key));
        }
        return data;
    }
    
    public CompoundTag save(final CompoundTag tag) {
        final CompoundTag stored = new CompoundTag();
        for (final Map.Entry<String, CompoundTag> e : this.crates.entrySet()) {
            stored.put((String)e.getKey(), (Tag)e.getValue());
        }
        tag.put("crates", (Tag)stored);
        return tag;
    }
}
