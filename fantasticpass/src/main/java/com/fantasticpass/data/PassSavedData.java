package com.fantasticpass.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * World-level persistence for pass templates and the currently active pass id.
 * Stored on the overworld's data storage so it is shared across dimensions.
 */
public final class PassSavedData extends SavedData {

    public static final String DATA_NAME = "fantasticpass_passes";

    private final Map<String, PassDefinition> passes = new LinkedHashMap<>();
    private String activePassId = "";

    public PassSavedData() {
    }

    public static PassSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(PassSavedData::new, PassSavedData::load, null),
                DATA_NAME);
    }

    public static PassSavedData get(ServerLevel level) {
        return get(level.getServer());
    }

    public Map<String, PassDefinition> getPasses() {
        return passes;
    }

    @Nullable
    public PassDefinition getPass(String id) {
        return id == null ? null : passes.get(id);
    }

    public boolean hasPass(String id) {
        return id != null && passes.containsKey(id);
    }

    public void putPass(PassDefinition definition) {
        if (definition != null && !definition.getId().isEmpty()) {
            passes.put(definition.getId(), definition);
            setDirty();
        }
    }

    public boolean deletePass(String id) {
        if (id == null) {
            return false;
        }
        boolean removed = passes.remove(id) != null;
        if (id.equals(activePassId)) {
            activePassId = "";
        }
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public String getActivePassId() {
        return activePassId;
    }

    public void setActivePassId(String activePassId) {
        this.activePassId = activePassId == null ? "" : activePassId;
        setDirty();
    }

    @Nullable
    public PassDefinition getActivePass() {
        if (activePassId == null || activePassId.isEmpty()) {
            return null;
        }
        return passes.get(activePassId);
    }

    // ---- Persistence ----

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString("activePassId", activePassId);
        CompoundTag passesTag = new CompoundTag();
        for (Map.Entry<String, PassDefinition> entry : passes.entrySet()) {
            passesTag.put(entry.getKey(), entry.getValue().toNbt());
        }
        tag.put("passes", passesTag);
        return tag;
    }

    public static PassSavedData load(CompoundTag tag) {
        PassSavedData data = new PassSavedData();
        data.activePassId = tag.getString("activePassId");
        CompoundTag passesTag = tag.getCompound("passes");
        for (String key : passesTag.getAllKeys()) {
            PassDefinition pass = PassDefinition.fromNbt(passesTag.getCompound(key));
            data.passes.put(pass.getId().isEmpty() ? key : pass.getId(), pass);
        }
        return data;
    }
}
