package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.client.discovery.ClientDiscoveryManager;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncDiscoveryDataPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(SyncDiscoveryDataPacket.class);
   public static final ResourceLocation ID = new ResourceLocation("blockpops", "sync_discovery_data");
   private final Set<String> discoveredFigures;
   private final Map<String, String> figureSkins;
   private final Map<String, String> figureQuickSkins;

   public SyncDiscoveryDataPacket(Set<String> discoveredFigures, Map<String, String> figureSkins, Map<String, String> figureQuickSkins) {
      this.discoveredFigures = new HashSet<>(discoveredFigures);
      this.figureSkins = new HashMap<>(figureSkins);
      this.figureQuickSkins = new HashMap<>(figureQuickSkins);
   }

   public FriendlyByteBuf encode() {
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      buffer.writeInt(this.discoveredFigures.size());

      for (String figureId : this.discoveredFigures) {
         buffer.writeUtf(figureId);
      }

      buffer.writeInt(this.figureSkins.size());

      for (Entry<String, String> entry : this.figureSkins.entrySet()) {
         buffer.writeUtf(entry.getKey());
         buffer.writeUtf(entry.getValue());
      }

      buffer.writeInt(this.figureQuickSkins.size());

      for (Entry<String, String> entry : this.figureQuickSkins.entrySet()) {
         buffer.writeUtf(entry.getKey());
         buffer.writeUtf(entry.getValue());
      }

      return buffer;
   }

   public static SyncDiscoveryDataPacket decode(FriendlyByteBuf buffer) {
      int size = buffer.readInt();
      Set<String> discoveredFigures = new HashSet<>();

      for (int i = 0; i < size; i++) {
         discoveredFigures.add(buffer.readUtf());
      }

      int skinsSize = buffer.readInt();
      Map<String, String> figureSkins = new HashMap<>();

      for (int i = 0; i < skinsSize; i++) {
         String figureId = buffer.readUtf();
         String skinUrl = buffer.readUtf();
         figureSkins.put(figureId, skinUrl);
      }

      int qsSize = buffer.readInt();
      Map<String, String> figureQuickSkins = new HashMap<>();

      for (int i = 0; i < qsSize; i++) {
         String figureId = buffer.readUtf();
         String quickSkinId = buffer.readUtf();
         figureQuickSkins.put(figureId, quickSkinId);
      }

      return new SyncDiscoveryDataPacket(discoveredFigures, figureSkins, figureQuickSkins);
   }

   public static void handleClient(FriendlyByteBuf buf, PacketContext context) {
      SyncDiscoveryDataPacket packet = decode(buf);
      context.queue(
         () -> {
            BlockPopsMod.logDebug(
               "Received discovery data sync: {} figures discovered, {} skins, {} quick skins",
               packet.discoveredFigures.size(),
               packet.figureSkins.size(),
               packet.figureQuickSkins.size()
            );
            ClientDiscoveryManager.setData(packet.discoveredFigures, packet.figureSkins, packet.figureQuickSkins);
         }
      );
   }

   public static void sendToPlayer(ServerPlayer player, Set<String> discoveredFigures, Map<String, String> figureSkins) {
      sendToPlayer(player, discoveredFigures, figureSkins, new HashMap<>());
   }

   public static void sendToPlayer(ServerPlayer player, Set<String> discoveredFigures, Map<String, String> figureSkins, Map<String, String> figureQuickSkins) {
      SyncDiscoveryDataPacket packet = new SyncDiscoveryDataPacket(discoveredFigures, figureSkins, figureQuickSkins);
      NetworkManager.sendToPlayer(player, ID, packet.encode());
   }

   public Set<String> getDiscoveredFigures() {
      return this.discoveredFigures;
   }

   public Map<String, String> getFigureSkins() {
      return this.figureSkins;
   }

   public Map<String, String> getFigureQuickSkins() {
      return this.figureQuickSkins;
   }
}
