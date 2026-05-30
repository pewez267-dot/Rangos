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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateRemoteCollectionsPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRemoteCollectionsPacket.class);
   public static final ResourceLocation ID = new ResourceLocation("blockpops", "update_remote_collections");
   private final List<String> enabledRemoteCollections;

   public UpdateRemoteCollectionsPacket(List<String> enabledRemoteCollections) {
      this.enabledRemoteCollections = enabledRemoteCollections != null ? enabledRemoteCollections : new ArrayList<>();
   }

   public FriendlyByteBuf encode() {
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      buffer.writeInt(this.enabledRemoteCollections.size());

      for (String id : this.enabledRemoteCollections) {
         buffer.writeUtf(id);
      }

      return buffer;
   }

   public static UpdateRemoteCollectionsPacket decode(FriendlyByteBuf buffer) {
      int count = buffer.readInt();
      List<String> enabled = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         enabled.add(buffer.readUtf());
      }

      return new UpdateRemoteCollectionsPacket(enabled);
   }

   public static void handleServer(FriendlyByteBuf buf, PacketContext context) {
      UpdateRemoteCollectionsPacket packet = decode(buf);
      context.queue(() -> {
         if (context.getPlayer() instanceof ServerPlayer player) {
            if (!player.hasPermissions(2)) {
               LOGGER.warn("Player {} tried to update remote collections without permission", player.getName().getString());
               return;
            }

            WorldConfig worldConfig = WorldConfig.get(player.getServer());
            worldConfig.setEnabledRemoteCollections(packet.enabledRemoteCollections);
            BlockPopsMod.logDebug("Player {} updated enabled remote collections: {}", player.getName().getString(), packet.enabledRemoteCollections);
            ServerCollectionLoader.loadCollections(new HashSet<>(packet.enabledRemoteCollections));
            SyncServerConfigPacket.broadcastToAll(player.getServer());
         }
      });
   }

   public void sendToServer() {
      NetworkManager.sendToServer(ID, this.encode());
   }
}
