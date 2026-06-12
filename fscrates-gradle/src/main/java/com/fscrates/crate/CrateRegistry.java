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
        final ServerLevel overworld = anyLevel.m_7654_().m_129783_();
        return (CrateRegistry)overworld.m_8895_().m_164861_((Function)CrateRegistry::load, (Supplier)CrateRegistry::new, "fscrates_definitions");
    }
    
    public void put(final CrateConfig crate) {
        this.crates.put(crate.id.toLowerCase(), crate.save());
        this.m_77762_();
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
            this.m_77762_();
        }
        return removed;
    }
    
    public Set<String> ids() {
        return this.crates.keySet();
    }
    
    public static CrateRegistry load(final CompoundTag tag) {
        final CrateRegistry data = new CrateRegistry();
        final CompoundTag stored = tag.m_128469_("crates");
        for (final String key : stored.m_128431_()) {
            data.crates.put(key, stored.m_128469_(key));
        }
        return data;
    }
    
    public CompoundTag m_7176_(final CompoundTag tag) {
        final CompoundTag stored = new CompoundTag();
        for (final Map.Entry<String, CompoundTag> e : this.crates.entrySet()) {
            stored.m_128365_((String)e.getKey(), (Tag)e.getValue());
        }
        tag.m_128365_("crates", (Tag)stored);
        return tag;
    }
}
