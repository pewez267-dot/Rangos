package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.server.ServerCollectionLoader;
import com.theplumteam.server.config.WorldConfig;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_5455;
import net.minecraft.class_9129;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateRemoteCollectionsPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRemoteCollectionsPacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "update_remote_collections");
   private final List<String> enabledRemoteCollections;

   public UpdateRemoteCollectionsPacket(List<String> enabledRemoteCollections) {
      this.enabledRemoteCollections = (List<String>)(enabledRemoteCollections != null ? enabledRemoteCollections : new ArrayList<>());
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_53002(this.enabledRemoteCollections.size());

      for (String id : this.enabledRemoteCollections) {
         buffer.method_10814(id);
      }

      return buffer;
   }

   public static UpdateRemoteCollectionsPacket decode(class_2540 buffer) {
      int count = buffer.readInt();
      List<String> enabled = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         enabled.add(buffer.method_19772());
      }

      return new UpdateRemoteCollectionsPacket(enabled);
   }

   public static void handleServer(class_2540 buf, PacketContext context) {
      UpdateRemoteCollectionsPacket packet = decode(buf);
      context.queue(() -> {
         if (context.getPlayer() instanceof class_3222 player) {
            if (!player.method_5687(2)) {
               LOGGER.warn("Player {} tried to update remote collections without permission", player.method_5477().getString());
               return;
            }

            WorldConfig worldConfig = WorldConfig.get(player.method_5682());
            worldConfig.setEnabledRemoteCollections(packet.enabledRemoteCollections);
            BlockPopsMod.logDebug("Player {} updated enabled remote collections: {}", player.method_5477().getString(), packet.enabledRemoteCollections);
            ServerCollectionLoader.loadCollections(new HashSet<>(packet.enabledRemoteCollections));
            SyncServerConfigPacket.broadcastToAll(player.method_5682());
         }
      });
   }

   public void sendToServer() {
      NetworkManager.sendToServer(ID, this.encode());
   }
}
