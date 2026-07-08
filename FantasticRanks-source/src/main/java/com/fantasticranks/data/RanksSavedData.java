/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.level.storage.DimensionDataStorage
 */
package com.fantasticranks.data;

import com.fantasticranks.data.RanksPackage;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class RanksSavedData
extends SavedData {
    public static final String DATA_NAME = "fantasticranks_packages";
    private final Map<String, RanksPackage> packages = new LinkedHashMap<String, RanksPackage>();
    private String activePackageId = "";
    // Contador global de wipes de progreso. Al hacer /fsranks wipe se incrementa; cada jugador
    // guarda cual fue el ultimo que aplico, para poder limpiar tambien a los que estan offline.
    private long wipeGeneration;

    public long getWipeGeneration() {
        return this.wipeGeneration;
    }

    public long bumpWipeGeneration() {
        ++this.wipeGeneration;
        this.setDirty();
        return this.wipeGeneration;
    }

    public static RanksSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return (RanksSavedData)storage.computeIfAbsent(RanksSavedData::load, RanksSavedData::new, DATA_NAME);
    }

    public static RanksSavedData get(ServerLevel level) {
        return RanksSavedData.get(level.getServer());
    }

    public Map<String, RanksPackage> getPackages() {
        return this.packages;
    }

    @Nullable
    public RanksPackage getPackage(String id) {
        return id == null ? null : this.packages.get(id);
    }

    public boolean hasPackage(String id) {
        return id != null && this.packages.containsKey(id);
    }

    public void putPackage(RanksPackage pkg) {
        if (pkg != null && !pkg.getId().isEmpty()) {
            this.packages.put(pkg.getId(), pkg);
            this.setDirty();
        }
    }

    public boolean deletePackage(String id) {
        boolean removed;
        if (id == null) {
            return false;
        }
        boolean bl = removed = this.packages.remove(id) != null;
        if (id.equals(this.activePackageId)) {
            this.activePackageId = "";
        }
        if (removed) {
            this.setDirty();
        }
        return removed;
    }

    public String getActivePackageId() {
        return this.activePackageId;
    }

    public void setActivePackageId(String activePackageId) {
        this.activePackageId = activePackageId == null ? "" : activePackageId;
        this.setDirty();
    }

    @Nullable
    public RanksPackage getActivePackage() {
        if (this.activePackageId == null || this.activePackageId.isEmpty()) {
            return null;
        }
        return this.packages.get(this.activePackageId);
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putString("activePackageId", this.activePackageId);
        tag.putLong("wipeGeneration", this.wipeGeneration);
        CompoundTag packagesTag = new CompoundTag();
        for (Map.Entry<String, RanksPackage> entry : this.packages.entrySet()) {
            packagesTag.put(entry.getKey(), (Tag)entry.getValue().toNbt());
        }
        tag.put("packages", (Tag)packagesTag);
        return tag;
    }

    public static RanksSavedData load(CompoundTag tag) {
        RanksSavedData data = new RanksSavedData();
        data.activePackageId = tag.getString("activePackageId");
        data.wipeGeneration = tag.getLong("wipeGeneration");
        CompoundTag packagesTag = tag.getCompound("packages");
        for (String key : packagesTag.getAllKeys()) {
            RanksPackage pkg = RanksPackage.fromNbt(packagesTag.getCompound(key));
            data.packages.put(pkg.getId().isEmpty() ? key : pkg.getId(), pkg);
        }
        return data;
    }
}

