package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.server.config.ServerConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateHiddenCollectionsPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(UpdateHiddenCollectionsPacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "update_hidden_collections");
   private final List<String> hiddenCollections;

   public UpdateHiddenCollectionsPacket(List<String> hiddenCollections) {
      this.hiddenCollections = (List<String>)(hiddenCollections != null ? hiddenCollections : new ArrayList<>());
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_53002(this.hiddenCollections.size());

      for (String id : this.hiddenCollections) {
         buffer.method_10814(id);
      }

      return buffer;
   }

   public static UpdateHiddenCollectionsPacket decode(class_2540 buffer) {
      int count = buffer.readInt();
      List<String> hiddenCollections = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         hiddenCollections.add(buffer.method_19772());
      }

      return new UpdateHiddenCollectionsPacket(hiddenCollections);
   }

   public static void handleServer(class_2540 buf, PacketContext context) {
      UpdateHiddenCollectionsPacket packet = decode(buf);
      context.queue(() -> {
         if (context.getPlayer() instanceof class_3222 player) {
            if (!player.method_5687(2)) {
               LOGGER.warn("Player {} tried to update hidden collections without permission", player.method_5477().getString());
               return;
            }

            ServerConfig config = ServerConfig.getInstance();
            config.setHiddenCollections(packet.hiddenCollections);
            BlockPopsMod.logDebug("Player {} updated hidden collections: {}", player.method_5477().getString(), packet.hiddenCollections);
            SyncServerConfigPacket.broadcastToAll(player.method_5682());
         }
      });
   }

   public void sendToServer() {
      NetworkManager.sendToServer(ID, this.encode());
   }
}
