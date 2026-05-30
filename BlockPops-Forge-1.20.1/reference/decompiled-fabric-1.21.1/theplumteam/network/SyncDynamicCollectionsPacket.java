package com.theplumteam.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_5455;
import net.minecraft.class_9129;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncDynamicCollectionsPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(SyncDynamicCollectionsPacket.class);
   private static final Gson GSON = new Gson();
   public static final class_2960 ID = class_2960.method_60655("blockpops", "sync_dynamic_collections");
   private final List<String> collectionsJson;

   public SyncDynamicCollectionsPacket(List<FigureCollection> collections) {
      this.collectionsJson = new ArrayList<>();

      for (FigureCollection collection : collections) {
         JsonObject json = collection.toJson();
         this.collectionsJson.add(GSON.toJson(json));
      }
   }

   private SyncDynamicCollectionsPacket(ArrayList<String> collectionsJson) {
      this.collectionsJson = collectionsJson;
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_53002(this.collectionsJson.size());

      for (String json : this.collectionsJson) {
         buffer.method_10788(json, 1048576);
      }

      return buffer;
   }

   public static SyncDynamicCollectionsPacket decode(class_2540 buffer) {
      int size = buffer.readInt();
      ArrayList<String> collectionsJson = new ArrayList<>();

      for (int i = 0; i < size; i++) {
         collectionsJson.add(buffer.method_10800(1048576));
      }

      return new SyncDynamicCollectionsPacket(collectionsJson);
   }

   public static void handleClient(class_2540 buf, PacketContext context) {
      SyncDynamicCollectionsPacket packet = decode(buf);
      context.queue(() -> {
         BlockPopsMod.logDebug("Received {} dynamic collections from server", packet.collectionsJson.size());

         for (String json : packet.collectionsJson) {
            try {
               JsonObject jsonObject = (JsonObject)GSON.fromJson(json, JsonObject.class);
               FigureCollection collection = FigureCollection.fromJson(jsonObject);
               CollectionRegistry.registerDynamicCollection(collection);
               BlockPopsMod.logDebug("Registered dynamic collection: {} with {} figures", collection.getName(), collection.getFigures().size());
            } catch (Exception var5) {
               LOGGER.error("Failed to deserialize dynamic collection", var5);
            }
         }
      });
   }

   public static void sendToPlayer(class_3222 player, List<FigureCollection> collections) {
      SyncDynamicCollectionsPacket packet = new SyncDynamicCollectionsPacket(collections);
      NetworkManager.sendToPlayer(player, ID, packet.encode());
   }

   public static void sendToAllPlayers(MinecraftServer server, List<FigureCollection> collections) {
      SyncDynamicCollectionsPacket packet = new SyncDynamicCollectionsPacket(collections);

      for (class_3222 player : server.method_3760().method_14571()) {
         NetworkManager.sendToPlayer(player, ID, packet.encode());
      }
   }

   public List<String> getCollectionsJson() {
      return this.collectionsJson;
   }
}
