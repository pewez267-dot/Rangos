package com.fscrates.crate;

import com.fscrates.config.CrateConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Server-side registry of named crate definitions, persisted in the world save.
 * Crates are referenced by id from keys and {@code /fscrate} commands, while the
 * full definition is also embedded in the crate ItemStack for portability.
 */
public class CrateRegistry extends SavedData {

    public static final String NAME = "fscrates_definitions";

    private final Map<String, CompoundTag> crates = new HashMap<>();

    public CrateRegistry() {}

    public static CrateRegistry get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(CrateRegistry::load, CrateRegistry::new, NAME);
    }

    public void put(CrateConfig crate) {
        crates.put(crate.id.toLowerCase(), crate.save());
        setDirty();
    }

    public CrateConfig get(String id) {
        CompoundTag tag = crates.get(id.toLowerCase());
        return tag == null ? null : CrateConfig.load(tag);
    }

    public boolean exists(String id) {
        return crates.containsKey(id.toLowerCase());
    }

    public boolean remove(String id) {
        boolean removed = crates.remove(id.toLowerCase()) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public Set<String> ids() {
        return crates.keySet();
    }

    public static CrateRegistry load(CompoundTag tag) {
        CrateRegistry data = new CrateRegistry();
        CompoundTag stored = tag.getCompound("crates");
        for (String key : stored.getAllKeys()) {
            data.crates.put(key, stored.getCompound(key));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag stored = new CompoundTag();
        for (Map.Entry<String, CompoundTag> e : crates.entrySet()) {
            stored.put(e.getKey(), e.getValue());
        }
        tag.put("crates", stored);
        return tag;
    }
}
