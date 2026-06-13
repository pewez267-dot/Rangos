package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.client.config.ClientServerConfig;
import com.theplumteam.client.remote.RemoteAssetManager;
import com.theplumteam.server.config.ServerConfig;
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
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncServerConfigPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(SyncServerConfigPacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "sync_server_config");
   private final int regularTokenCooldownHours;
   private final int maxRegularTokens;
   private final int guaranteedTokenResetHour;
   private final List<String> hiddenCollections;
   private final List<String> enabledRemoteCollections;

   public SyncServerConfigPacket(
      int regularTokenCooldownHours, int maxRegularTokens, int guaranteedTokenResetHour, List<String> hiddenCollections, List<String> enabledRemoteCollections
   ) {
      this.regularTokenCooldownHours = regularTokenCooldownHours;
      this.maxRegularTokens = maxRegularTokens;
      this.guaranteedTokenResetHour = guaranteedTokenResetHour;
      this.hiddenCollections = (List<String>)(hiddenCollections != null ? hiddenCollections : new ArrayList<>());
      this.enabledRemoteCollections = (List<String>)(enabledRemoteCollections != null ? enabledRemoteCollections : new ArrayList<>());
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_53002(this.regularTokenCooldownHours);
      buffer.method_53002(this.maxRegularTokens);
      buffer.method_53002(this.guaranteedTokenResetHour);
      buffer.method_53002(this.hiddenCollections.size());

      for (String id : this.hiddenCollections) {
         buffer.method_10814(id);
      }

      buffer.method_53002(this.enabledRemoteCollections.size());

      for (String id : this.enabledRemoteCollections) {
         buffer.method_10814(id);
      }

      return buffer;
   }

   public static SyncServerConfigPacket decode(class_2540 buffer) {
      int regularTokenCooldownHours = buffer.readInt();
      int maxRegularTokens = buffer.readInt();
      int guaranteedTokenResetHour = buffer.readInt();
      int hiddenCount = buffer.readInt();
      List<String> hiddenCollections = new ArrayList<>(hiddenCount);

      for (int i = 0; i < hiddenCount; i++) {
         hiddenCollections.add(buffer.method_19772());
      }

      List<String> enabledRemoteCollections = new ArrayList<>();
      if (buffer.isReadable()) {
         int remoteCount = buffer.readInt();

         for (int i = 0; i < remoteCount; i++) {
            enabledRemoteCollections.add(buffer.method_19772());
         }
      }

      if (buffer.isReadable()) {
         BlockPopsMod.LOCAL_ADMIN = buffer.readBoolean();
      }

      return new SyncServerConfigPacket(regularTokenCooldownHours, maxRegularTokens, guaranteedTokenResetHour, hiddenCollections, enabledRemoteCollections);
   }

   public static void handleClient(class_2540 buf, PacketContext context) {
      SyncServerConfigPacket packet = decode(buf);
      context.queue(
         () -> {
            ClientServerConfig.update(packet.regularTokenCooldownHours, packet.maxRegularTokens, packet.guaranteedTokenResetHour);
            ClientServerConfig.updateHiddenCollections(packet.hiddenCollections);
            ClientServerConfig.updateEnabledRemoteCollections(packet.enabledRemoteCollections);
            if (!packet.enabledRemoteCollections.isEmpty()) {
               RemoteAssetManager.init();
               RemoteAssetManager.syncEnabledCollections(new HashSet<>(packet.enabledRemoteCollections));
            }

            LOGGER.debug(
               "Received server config sync: cooldown={}h, maxTokens={}, resetHour={}, hidden={}, remote={}",
               new Object[]{
                  packet.regularTokenCooldownHours,
                  packet.maxRegularTokens,
                  packet.guaranteedTokenResetHour,
                  packet.hiddenCollections,
                  packet.enabledRemoteCollections
               }
            );
         }
      );
   }

   public static void sendToPlayer(class_3222 player) {
      ServerConfig config = ServerConfig.getInstance();
      WorldConfig worldConfig = WorldConfig.get(player.method_5682());
      SyncServerConfigPacket packet = new SyncServerConfigPacket(
         config.getRegularTokenCooldownHours(),
         config.getMaxRegularTokens(),
         config.getGuaranteedTokenResetHour(),
         config.getHiddenCollections(),
         worldConfig.getEnabledRemoteCollections()
      );
      class_2960 var10001 = ID;
      class_9129 var10002 = packet.encode();
      var10002.method_52964(player.method_5687(2));
      NetworkManager.sendToPlayer(player, var10001, var10002);
   }

   public static void broadcastToAll(MinecraftServer server) {
      for (class_3222 player : server.method_3760().method_14571()) {
         sendToPlayer(player);
      }
   }
}
