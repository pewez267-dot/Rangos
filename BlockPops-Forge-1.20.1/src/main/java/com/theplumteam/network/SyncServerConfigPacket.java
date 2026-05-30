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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncServerConfigPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(SyncServerConfigPacket.class);
   public static final ResourceLocation ID = new ResourceLocation("blockpops", "sync_server_config");
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
      this.hiddenCollections = hiddenCollections != null ? hiddenCollections : new ArrayList<>();
      this.enabledRemoteCollections = enabledRemoteCollections != null ? enabledRemoteCollections : new ArrayList<>();
   }

   public FriendlyByteBuf encode() {
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      buffer.writeInt(this.regularTokenCooldownHours);
      buffer.writeInt(this.maxRegularTokens);
      buffer.writeInt(this.guaranteedTokenResetHour);
      buffer.writeInt(this.hiddenCollections.size());

      for (String id : this.hiddenCollections) {
         buffer.writeUtf(id);
      }

      buffer.writeInt(this.enabledRemoteCollections.size());

      for (String id : this.enabledRemoteCollections) {
         buffer.writeUtf(id);
      }

      return buffer;
   }

   public static SyncServerConfigPacket decode(FriendlyByteBuf buffer) {
      int regularTokenCooldownHours = buffer.readInt();
      int maxRegularTokens = buffer.readInt();
      int guaranteedTokenResetHour = buffer.readInt();
      int hiddenCount = buffer.readInt();
      List<String> hiddenCollections = new ArrayList<>(hiddenCount);

      for (int i = 0; i < hiddenCount; i++) {
         hiddenCollections.add(buffer.readUtf());
      }

      List<String> enabledRemoteCollections = new ArrayList<>();
      if (buffer.isReadable()) {
         int remoteCount = buffer.readInt();

         for (int i = 0; i < remoteCount; i++) {
            enabledRemoteCollections.add(buffer.readUtf());
         }
      }

      if (buffer.isReadable()) {
         BlockPopsMod.LOCAL_ADMIN = buffer.readBoolean();
      }

      return new SyncServerConfigPacket(regularTokenCooldownHours, maxRegularTokens, guaranteedTokenResetHour, hiddenCollections, enabledRemoteCollections);
   }

   public static void handleClient(FriendlyByteBuf buf, PacketContext context) {
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
               packet.regularTokenCooldownHours,
               packet.maxRegularTokens,
               packet.guaranteedTokenResetHour,
               packet.hiddenCollections,
               packet.enabledRemoteCollections
            );
         }
      );
   }

   public static void sendToPlayer(ServerPlayer player) {
      ServerConfig config = ServerConfig.getInstance();
      WorldConfig worldConfig = WorldConfig.get(player.getServer());
      SyncServerConfigPacket packet = new SyncServerConfigPacket(
         config.getRegularTokenCooldownHours(),
         config.getMaxRegularTokens(),
         config.getGuaranteedTokenResetHour(),
         config.getHiddenCollections(),
         worldConfig.getEnabledRemoteCollections()
      );
      FriendlyByteBuf buf = packet.encode();
      buf.writeBoolean(player.hasPermissions(2));
      NetworkManager.sendToPlayer(player, ID, buf);
   }

   public static void broadcastToAll(MinecraftServer server) {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         sendToPlayer(player);
      }
   }
}
