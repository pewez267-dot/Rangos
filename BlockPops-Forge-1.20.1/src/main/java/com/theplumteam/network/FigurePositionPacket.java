package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.blockentity.BoxBlockEntity;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FigurePositionPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(FigurePositionPacket.class);
   private final BlockPos pos;
   private final double offsetX;
   private final double offsetY;
   private final double offsetZ;
   private final double scale;
   private final double hitboxOffsetX;
   private final double hitboxOffsetY;
   private final double hitboxOffsetZ;
   private final double hitboxScaleX;
   private final double hitboxScaleY;
   private final double hitboxScaleZ;
   private final Double logoPositionX;
   private final Double logoPositionY;
   private final Double logoPositionZ;
   private final Double logoScaleX;
   private final Double logoScaleY;
   private final Double logoScaleZ;

   public FigurePositionPacket(
      BlockPos pos,
      double offsetX,
      double offsetY,
      double offsetZ,
      double scale,
      double hitboxOffsetX,
      double hitboxOffsetY,
      double hitboxOffsetZ,
      double hitboxScaleX,
      double hitboxScaleY,
      double hitboxScaleZ,
      Double logoPositionX,
      Double logoPositionY,
      Double logoPositionZ,
      Double logoScaleX,
      Double logoScaleY,
      Double logoScaleZ
   ) {
      this.pos = pos;
      this.offsetX = offsetX;
      this.offsetY = offsetY;
      this.offsetZ = offsetZ;
      this.scale = scale;
      this.hitboxOffsetX = hitboxOffsetX;
      this.hitboxOffsetY = hitboxOffsetY;
      this.hitboxOffsetZ = hitboxOffsetZ;
      this.hitboxScaleX = hitboxScaleX;
      this.hitboxScaleY = hitboxScaleY;
      this.hitboxScaleZ = hitboxScaleZ;
      this.logoPositionX = logoPositionX;
      this.logoPositionY = logoPositionY;
      this.logoPositionZ = logoPositionZ;
      this.logoScaleX = logoScaleX;
      this.logoScaleY = logoScaleY;
      this.logoScaleZ = logoScaleZ;
   }

   public FriendlyByteBuf encode() {
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      buffer.writeBlockPos(this.pos);
      buffer.writeDouble(this.offsetX);
      buffer.writeDouble(this.offsetY);
      buffer.writeDouble(this.offsetZ);
      buffer.writeDouble(this.scale);
      buffer.writeDouble(this.hitboxOffsetX);
      buffer.writeDouble(this.hitboxOffsetY);
      buffer.writeDouble(this.hitboxOffsetZ);
      buffer.writeDouble(this.hitboxScaleX);
      buffer.writeDouble(this.hitboxScaleY);
      buffer.writeDouble(this.hitboxScaleZ);
      buffer.writeBoolean(this.logoPositionX != null);
      if (this.logoPositionX != null) {
         buffer.writeDouble(this.logoPositionX);
      }

      buffer.writeBoolean(this.logoPositionY != null);
      if (this.logoPositionY != null) {
         buffer.writeDouble(this.logoPositionY);
      }

      buffer.writeBoolean(this.logoPositionZ != null);
      if (this.logoPositionZ != null) {
         buffer.writeDouble(this.logoPositionZ);
      }

      buffer.writeBoolean(this.logoScaleX != null);
      if (this.logoScaleX != null) {
         buffer.writeDouble(this.logoScaleX);
      }

      buffer.writeBoolean(this.logoScaleY != null);
      if (this.logoScaleY != null) {
         buffer.writeDouble(this.logoScaleY);
      }

      buffer.writeBoolean(this.logoScaleZ != null);
      if (this.logoScaleZ != null) {
         buffer.writeDouble(this.logoScaleZ);
      }

      return buffer;
   }

   public static FigurePositionPacket decode(FriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      double offsetX = buffer.readDouble();
      double offsetY = buffer.readDouble();
      double offsetZ = buffer.readDouble();
      double scale = buffer.readDouble();
      double hitboxOffsetX = buffer.readDouble();
      double hitboxOffsetY = buffer.readDouble();
      double hitboxOffsetZ = buffer.readDouble();
      double hitboxScaleX = buffer.readDouble();
      double hitboxScaleY = buffer.readDouble();
      double hitboxScaleZ = buffer.readDouble();
      Double logoPositionX = buffer.readBoolean() ? buffer.readDouble() : null;
      Double logoPositionY = buffer.readBoolean() ? buffer.readDouble() : null;
      Double logoPositionZ = buffer.readBoolean() ? buffer.readDouble() : null;
      Double logoScaleX = buffer.readBoolean() ? buffer.readDouble() : null;
      Double logoScaleY = buffer.readBoolean() ? buffer.readDouble() : null;
      Double logoScaleZ = buffer.readBoolean() ? buffer.readDouble() : null;
      return new FigurePositionPacket(
         pos,
         offsetX,
         offsetY,
         offsetZ,
         scale,
         hitboxOffsetX,
         hitboxOffsetY,
         hitboxOffsetZ,
         hitboxScaleX,
         hitboxScaleY,
         hitboxScaleZ,
         logoPositionX,
         logoPositionY,
         logoPositionZ,
         logoScaleX,
         logoScaleY,
         logoScaleZ
      );
   }

   public static void handleServer(FriendlyByteBuf buf, PacketContext context) {
      FigurePositionPacket packet = decode(buf);
      BlockPopsMod.logDebug("Received FigurePositionPacket on server - Position: {}", packet.pos);
      context.queue(() -> {
         if (context.getPlayer() instanceof ServerPlayer player) {
            if (player.level().getBlockEntity(packet.pos) instanceof BoxBlockEntity boxBlockEntity) {
               BlockPopsMod.logDebug("Setting figure offset, scale, hitbox offset, hitbox scale, and logo config");
               boxBlockEntity.setFigureOffset(packet.offsetX, packet.offsetY, packet.offsetZ);
               boxBlockEntity.setFigureScale(packet.scale);
               boxBlockEntity.setHitboxOffset(packet.hitboxOffsetX, packet.hitboxOffsetY, packet.hitboxOffsetZ);
               boxBlockEntity.setHitboxScale(packet.hitboxScaleX, packet.hitboxScaleY, packet.hitboxScaleZ);
               boxBlockEntity.setLogoPosition(packet.logoPositionX, packet.logoPositionY, packet.logoPositionZ);
               boxBlockEntity.setLogoScale(packet.logoScaleX, packet.logoScaleY, packet.logoScaleZ);
               boxBlockEntity.setChanged();
               BlockPopsMod.logDebug("Figure position updated successfully");
            } else {
               LOGGER.warn("BlockEntity at {} is not a BoxBlockEntity", packet.pos);
            }
         }
      });
   }

   public void sendToServer() {
      NetworkManager.sendToServer(ModNetworking.FIGURE_POSITION, this.encode());
   }
}
