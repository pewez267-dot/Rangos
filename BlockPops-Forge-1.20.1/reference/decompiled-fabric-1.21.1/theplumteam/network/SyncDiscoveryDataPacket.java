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
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_5455;
import net.minecraft.class_9129;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncDiscoveryDataPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(SyncDiscoveryDataPacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "sync_discovery_data");
   private final Set<String> discoveredFigures;
   private final Map<String, String> figureSkins;
   private final Map<String, String> figureQuickSkins;

   public SyncDiscoveryDataPacket(Set<String> discoveredFigures, Map<String, String> figureSkins, Map<String, String> figureQuickSkins) {
      this.discoveredFigures = new HashSet<>(discoveredFigures);
      this.figureSkins = new HashMap<>(figureSkins);
      this.figureQuickSkins = new HashMap<>(figureQuickSkins);
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_53002(this.discoveredFigures.size());

      for (String figureId : this.discoveredFigures) {
         buffer.method_10814(figureId);
      }

      buffer.method_53002(this.figureSkins.size());

      for (Entry<String, String> entry : this.figureSkins.entrySet()) {
         buffer.method_10814(entry.getKey());
         buffer.method_10814(entry.getValue());
      }

      buffer.method_53002(this.figureQuickSkins.size());

      for (Entry<String, String> entry : this.figureQuickSkins.entrySet()) {
         buffer.method_10814(entry.getKey());
         buffer.method_10814(entry.getValue());
      }

      return buffer;
   }

   public static SyncDiscoveryDataPacket decode(class_2540 buffer) {
      int size = buffer.readInt();
      Set<String> discoveredFigures = new HashSet<>();

      for (int i = 0; i < size; i++) {
         discoveredFigures.add(buffer.method_19772());
      }

      int skinsSize = buffer.readInt();
      Map<String, String> figureSkins = new HashMap<>();

      for (int i = 0; i < skinsSize; i++) {
         String figureId = buffer.method_19772();
         String skinUrl = buffer.method_19772();
         figureSkins.put(figureId, skinUrl);
      }

      int qsSize = buffer.readInt();
      Map<String, String> figureQuickSkins = new HashMap<>();

      for (int i = 0; i < qsSize; i++) {
         String figureId = buffer.method_19772();
         String quickSkinId = buffer.method_19772();
         figureQuickSkins.put(figureId, quickSkinId);
      }

      return new SyncDiscoveryDataPacket(discoveredFigures, figureSkins, figureQuickSkins);
   }

   public static void handleClient(class_2540 buf, PacketContext context) {
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

   public static void sendToPlayer(class_3222 player, Set<String> discoveredFigures, Map<String, String> figureSkins) {
      sendToPlayer(player, discoveredFigures, figureSkins, new HashMap<>());
   }

   public static void sendToPlayer(class_3222 player, Set<String> discoveredFigures, Map<String, String> figureSkins, Map<String, String> figureQuickSkins) {
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
