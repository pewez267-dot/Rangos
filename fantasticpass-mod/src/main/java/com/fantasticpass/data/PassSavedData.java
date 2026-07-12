package com.fantasticpass.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class PassSavedData extends SavedData {
   public static final String DATA_NAME = "fantasticpass_passes";
   private final Map<String, PassDefinition> passes = new LinkedHashMap<>();
   private String activePassId = "";

   public static PassSavedData get(MinecraftServer server) {
      ServerLevel overworld = server.overworld();
      DimensionDataStorage storage = overworld.getDataStorage();
      return (PassSavedData)storage.computeIfAbsent(PassSavedData::load, PassSavedData::new, "fantasticpass_passes");
   }

   public static PassSavedData get(ServerLevel level) {
      return get(level.getServer());
   }

   public Map<String, PassDefinition> getPasses() {
      return this.passes;
   }

   @Nullable
   public PassDefinition getPass(String id) {
      return id == null ? null : this.passes.get(id);
   }

   public boolean hasPass(String id) {
      return id != null && this.passes.containsKey(id);
   }

   public void putPass(PassDefinition definition) {
      if (definition != null && !definition.getId().isEmpty()) {
         this.passes.put(definition.getId(), definition);
         this.setDirty();
      }
   }

   public boolean deletePass(String id) {
      if (id == null) {
         return false;
      } else {
         boolean removed = this.passes.remove(id) != null;
         if (id.equals(this.activePassId)) {
            this.activePassId = "";
         }

         if (removed) {
            this.setDirty();
         }

         return removed;
      }
   }

   public String getActivePassId() {
      return this.activePassId;
   }

   public void setActivePassId(String activePassId) {
      this.activePassId = activePassId == null ? "" : activePassId;
      this.setDirty();
   }

   @Nullable
   public PassDefinition getActivePass() {
      return this.activePassId != null && !this.activePassId.isEmpty() ? this.passes.get(this.activePassId) : null;
   }

   public CompoundTag save(CompoundTag tag) {
      tag.putString("activePassId", this.activePassId);
      CompoundTag passesTag = new CompoundTag();

      for (Entry<String, PassDefinition> entry : this.passes.entrySet()) {
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
