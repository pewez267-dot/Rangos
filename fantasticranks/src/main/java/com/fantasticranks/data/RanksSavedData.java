package com.fantasticranks.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * World-level persistence for rank packages and the currently active package id. Stored
 * on the overworld's data storage so it is shared across dimensions.
 */
public final class RanksSavedData extends SavedData {

    public static final String DATA_NAME = "fantasticranks_packages";

    private final Map<String, RanksPackage> packages = new LinkedHashMap<>();
    private String activePackageId = "";

    public RanksSavedData() {
    }

    public static RanksSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(RanksSavedData::load, RanksSavedData::new, DATA_NAME);
    }

    public static RanksSavedData get(ServerLevel level) {
        return get(level.getServer());
    }

    public Map<String, RanksPackage> getPackages() {
        return packages;
    }

    @Nullable
    public RanksPackage getPackage(String id) {
        return id == null ? null : packages.get(id);
    }

    public boolean hasPackage(String id) {
        return id != null && packages.containsKey(id);
    }

    public void putPackage(RanksPackage pkg) {
        if (pkg != null && !pkg.getId().isEmpty()) {
            packages.put(pkg.getId(), pkg);
            setDirty();
        }
    }

    public boolean deletePackage(String id) {
        if (id == null) {
            return false;
        }
        boolean removed = packages.remove(id) != null;
        if (id.equals(activePackageId)) {
            activePackageId = "";
        }
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public String getActivePackageId() {
        return activePackageId;
    }

    public void setActivePackageId(String activePackageId) {
        this.activePackageId = activePackageId == null ? "" : activePackageId;
        setDirty();
    }

    @Nullable
    public RanksPackage getActivePackage() {
        if (activePackageId == null || activePackageId.isEmpty()) {
            return null;
        }
        return packages.get(activePackageId);
    }

    // ---- Persistence ----

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString("activePackageId", activePackageId);
        CompoundTag packagesTag = new CompoundTag();
        for (Map.Entry<String, RanksPackage> entry : packages.entrySet()) {
            packagesTag.put(entry.getKey(), entry.getValue().toNbt());
        }
        tag.put("packages", packagesTag);
        return tag;
    }

    public static RanksSavedData load(CompoundTag tag) {
        RanksSavedData data = new RanksSavedData();
        data.activePackageId = tag.getString("activePackageId");
        CompoundTag packagesTag = tag.getCompound("packages");
        for (String key : packagesTag.getAllKeys()) {
            RanksPackage pkg = RanksPackage.fromNbt(packagesTag.getCompound(key));
            data.packages.put(pkg.getId().isEmpty() ? key : pkg.getId(), pkg);
        }
        return data;
    }
}
