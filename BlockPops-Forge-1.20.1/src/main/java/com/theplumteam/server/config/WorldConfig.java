package com.theplumteam.server.config;

import com.theplumteam.BlockPopsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-world saved data holding the set of enabled remote collections.
 * NOTE: On 1.20.1 SavedData#save(CompoundTag) and SavedData.Factory take no
 * HolderLookup.Provider (that parameter was added in 1.20.5).
 */
public class WorldConfig extends SavedData {
   private List<String> enabledRemoteCollections = new ArrayList<>();

   public List<String> getEnabledRemoteCollections() {
      return this.enabledRemoteCollections;
   }

   public void setEnabledRemoteCollections(List<String> enabled) {
      this.enabledRemoteCollections = enabled != null ? new ArrayList<>(enabled) : new ArrayList<>();
      this.setDirty();
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      CompoundTag collectionsTag = new CompoundTag();

      for (String id : this.enabledRemoteCollections) {
         collectionsTag.putBoolean(id, true);
      }

      tag.put("EnabledRemoteCollections", collectionsTag);
      return tag;
   }

   public static WorldConfig createFromTag(CompoundTag tag) {
      WorldConfig config = new WorldConfig();
      CompoundTag collectionsTag = tag.getCompound("EnabledRemoteCollections");

      for (String key : collectionsTag.getAllKeys()) {
         config.enabledRemoteCollections.add(key);
      }

      BlockPopsMod.LOGGER.debug("Loaded per-world config: {} enabled remote collections", config.enabledRemoteCollections.size());
      return config;
   }

   public static WorldConfig get(MinecraftServer server) {
      return server.overworld()
         .getDataStorage()
         .computeIfAbsent(new SavedData.Factory<>(WorldConfig::new, WorldConfig::createFromTag, null), "blockpops_world");
   }
}
