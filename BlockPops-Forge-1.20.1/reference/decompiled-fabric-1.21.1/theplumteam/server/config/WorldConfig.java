package com.theplumteam.server.config;

import com.theplumteam.BlockPopsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_18;
import net.minecraft.class_2487;
import net.minecraft.class_18.class_8645;
import net.minecraft.class_7225.class_7874;
import net.minecraft.server.MinecraftServer;

public class WorldConfig extends class_18 {
   private List<String> enabledRemoteCollections = new ArrayList<>();

   public List<String> getEnabledRemoteCollections() {
      return this.enabledRemoteCollections;
   }

   public void setEnabledRemoteCollections(List<String> enabled) {
      this.enabledRemoteCollections = enabled != null ? new ArrayList<>(enabled) : new ArrayList<>();
      this.method_80();
   }

   public class_2487 method_75(class_2487 tag, class_7874 registries) {
      class_2487 collectionsTag = new class_2487();

      for (String id : this.enabledRemoteCollections) {
         collectionsTag.method_10556(id, true);
      }

      tag.method_10566("EnabledRemoteCollections", collectionsTag);
      return tag;
   }

   public static WorldConfig createFromTag(class_2487 tag, class_7874 registries) {
      WorldConfig config = new WorldConfig();
      class_2487 collectionsTag = tag.method_10562("EnabledRemoteCollections");

      for (String key : collectionsTag.method_10541()) {
         config.enabledRemoteCollections.add(key);
      }

      BlockPopsMod.LOGGER.debug("Loaded per-world config: {} enabled remote collections", config.enabledRemoteCollections.size());
      return config;
   }

   public static WorldConfig get(MinecraftServer server) {
      return (WorldConfig)server.method_30002()
         .method_17983()
         .method_17924(new class_8645(WorldConfig::new, WorldConfig::createFromTag, null), "blockpops_world");
   }
}
