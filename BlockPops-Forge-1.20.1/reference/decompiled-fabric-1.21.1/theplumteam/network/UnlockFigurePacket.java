package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.client.discovery.ClientDiscoveryManager;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_5455;
import net.minecraft.class_9129;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlockFigurePacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(UnlockFigurePacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "unlock_figure");
   private final String figureId;
   private final String figureName;
   @Nullable
   private final String skinSnapshot;
   @Nullable
   private final String quickSkinId;

   public UnlockFigurePacket(String figureId, String figureName) {
      this(figureId, figureName, null, null);
   }

   public UnlockFigurePacket(String figureId, String figureName, @Nullable String skinSnapshot) {
      this(figureId, figureName, skinSnapshot, null);
   }

   public UnlockFigurePacket(String figureId, String figureName, @Nullable String skinSnapshot, @Nullable String quickSkinId) {
      this.figureId = figureId;
      this.figureName = figureName;
      this.skinSnapshot = skinSnapshot;
      this.quickSkinId = quickSkinId;
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_10814(this.figureId);
      buffer.method_10814(this.figureName);
      buffer.method_52964(this.skinSnapshot != null);
      if (this.skinSnapshot != null) {
         buffer.method_10814(this.skinSnapshot);
      }

      buffer.method_52964(this.quickSkinId != null);
      if (this.quickSkinId != null) {
         buffer.method_10814(this.quickSkinId);
      }

      return buffer;
   }

   public static UnlockFigurePacket decode(class_2540 buffer) {
      String figureId = buffer.method_19772();
      String figureName = buffer.method_19772();
      String skinSnapshot = null;
      if (buffer.readBoolean()) {
         skinSnapshot = buffer.method_19772();
      }

      String quickSkinId = null;
      if (buffer.readBoolean()) {
         quickSkinId = buffer.method_19772();
      }

      return new UnlockFigurePacket(figureId, figureName, skinSnapshot, quickSkinId);
   }

   public static void handleClient(class_2540 buf, PacketContext context) {
      UnlockFigurePacket packet = decode(buf);
      context.queue(() -> {
         BlockPopsMod.logDebug("Unlocked new figure: {} ({})", packet.figureName, packet.figureId);
         ClientDiscoveryManager.unlock(packet.figureId);
         if (packet.skinSnapshot != null) {
            ClientDiscoveryManager.saveFigureSkin(packet.figureId, packet.skinSnapshot);
            BlockPopsMod.logDebug("Saved skin snapshot for unlocked figure: {}", packet.figureId);
         }

         if (packet.quickSkinId != null) {
            ClientDiscoveryManager.saveFigureQuickSkin(packet.figureId, packet.quickSkinId);
            BlockPopsMod.logDebug("Saved Quick Skin ID for unlocked figure: {}", packet.figureId);
         }
      });
   }

   public static void sendToPlayer(class_3222 player, String figureId, String figureName, @Nullable String skinSnapshot) {
      sendToPlayer(player, figureId, figureName, skinSnapshot, null);
   }

   public static void sendToPlayer(class_3222 player, String figureId, String figureName, @Nullable String skinSnapshot, @Nullable String quickSkinId) {
      UnlockFigurePacket packet = new UnlockFigurePacket(figureId, figureName, skinSnapshot, quickSkinId);
      NetworkManager.sendToPlayer(player, ID, packet.encode());
   }

   public String getFigureId() {
      return this.figureId;
   }

   public String getFigureName() {
      return this.figureName;
   }

   @Nullable
   public String getSkinSnapshot() {
      return this.skinSnapshot;
   }

   @Nullable
   public String getQuickSkinId() {
      return this.quickSkinId;
   }
}
