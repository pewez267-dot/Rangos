package com.fspawner.command;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Named spawner configuration presets. Stored inside the world save (SavedData)
 * so it requires no external files, configs or databases - just the world data.
 */
public class FSpawnerPresets extends SavedData {

    public static final String NAME = "fspawner_presets";

    private final Map<String, CompoundTag> presets = new HashMap<>();

    public FSpawnerPresets() {}

    public static FSpawnerPresets get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(FSpawnerPresets::load, FSpawnerPresets::new, NAME);
    }

    public void put(String name, CompoundTag config) {
        presets.put(name.toLowerCase(), config.copy());
        setDirty();
    }

    public CompoundTag get(String name) {
        CompoundTag tag = presets.get(name.toLowerCase());
        return tag == null ? null : tag.copy();
    }

    public boolean remove(String name) {
        boolean removed = presets.remove(name.toLowerCase()) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public Set<String> names() {
        return presets.keySet();
    }

    public static FSpawnerPresets load(CompoundTag tag) {
        FSpawnerPresets data = new FSpawnerPresets();
        CompoundTag stored = tag.getCompound("presets");
        for (String key : stored.getAllKeys()) {
            data.presets.put(key, stored.getCompound(key));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag stored = new CompoundTag();
        for (Map.Entry<String, CompoundTag> e : presets.entrySet()) {
            stored.put(e.getKey(), e.getValue());
        }
        tag.put("presets", stored);
        return tag;
    }
}
