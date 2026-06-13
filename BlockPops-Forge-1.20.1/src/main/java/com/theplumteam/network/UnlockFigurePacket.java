package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.client.discovery.ClientDiscoveryManager;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlockFigurePacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(UnlockFigurePacket.class);
   public static final ResourceLocation ID = new ResourceLocation("blockpops", "unlock_figure");
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

   public FriendlyByteBuf encode() {
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      buffer.writeUtf(this.figureId);
      buffer.writeUtf(this.figureName);
      buffer.writeBoolean(this.skinSnapshot != null);
      if (this.skinSnapshot != null) {
         buffer.writeUtf(this.skinSnapshot);
      }

      buffer.writeBoolean(this.quickSkinId != null);
      if (this.quickSkinId != null) {
         buffer.writeUtf(this.quickSkinId);
      }

      return buffer;
   }

   public static UnlockFigurePacket decode(FriendlyByteBuf buffer) {
      String figureId = buffer.readUtf();
      String figureName = buffer.readUtf();
      String skinSnapshot = null;
      if (buffer.readBoolean()) {
         skinSnapshot = buffer.readUtf();
      }

      String quickSkinId = null;
      if (buffer.readBoolean()) {
         quickSkinId = buffer.readUtf();
      }

      return new UnlockFigurePacket(figureId, figureName, skinSnapshot, quickSkinId);
   }

   public static void handleClient(FriendlyByteBuf buf, PacketContext context) {
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

   public static void sendToPlayer(ServerPlayer player, String figureId, String figureName, @Nullable String skinSnapshot) {
      sendToPlayer(player, figureId, figureName, skinSnapshot, null);
   }

   public static void sendToPlayer(ServerPlayer player, String figureId, String figureName, @Nullable String skinSnapshot, @Nullable String quickSkinId) {
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
