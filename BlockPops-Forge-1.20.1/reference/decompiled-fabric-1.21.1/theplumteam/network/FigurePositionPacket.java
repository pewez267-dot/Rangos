package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.blockentity.BoxBlockEntity;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import net.minecraft.class_2338;
import net.minecraft.class_2540;
import net.minecraft.class_3222;
import net.minecraft.class_5455;
import net.minecraft.class_9129;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FigurePositionPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(FigurePositionPacket.class);
   private final class_2338 pos;
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
      class_2338 pos,
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

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_10807(this.pos);
      buffer.method_52940(this.offsetX);
      buffer.method_52940(this.offsetY);
      buffer.method_52940(this.offsetZ);
      buffer.method_52940(this.scale);
      buffer.method_52940(this.hitboxOffsetX);
      buffer.method_52940(this.hitboxOffsetY);
      buffer.method_52940(this.hitboxOffsetZ);
      buffer.method_52940(this.hitboxScaleX);
      buffer.method_52940(this.hitboxScaleY);
      buffer.method_52940(this.hitboxScaleZ);
      buffer.method_52964(this.logoPositionX != null);
      if (this.logoPositionX != null) {
         buffer.method_52940(this.logoPositionX);
      }

      buffer.method_52964(this.logoPositionY != null);
      if (this.logoPositionY != null) {
         buffer.method_52940(this.logoPositionY);
      }

      buffer.method_52964(this.logoPositionZ != null);
      if (this.logoPositionZ != null) {
         buffer.method_52940(this.logoPositionZ);
      }

      buffer.method_52964(this.logoScaleX != null);
      if (this.logoScaleX != null) {
         buffer.method_52940(this.logoScaleX);
      }

      buffer.method_52964(this.logoScaleY != null);
      if (this.logoScaleY != null) {
         buffer.method_52940(this.logoScaleY);
      }

      buffer.method_52964(this.logoScaleZ != null);
      if (this.logoScaleZ != null) {
         buffer.method_52940(this.logoScaleZ);
      }

      return buffer;
   }

   public static FigurePositionPacket decode(class_2540 buffer) {
      class_2338 pos = buffer.method_10811();
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

   public static void handleServer(class_2540 buf, PacketContext context) {
      FigurePositionPacket packet = decode(buf);
      BlockPopsMod.logDebug("Received FigurePositionPacket on server - Position: {}", packet.pos);
      context.queue(() -> {
         if (context.getPlayer() instanceof class_3222 player) {
            if (player.method_37908().method_8321(packet.pos) instanceof BoxBlockEntity boxBlockEntity) {
               BlockPopsMod.logDebug("Setting figure offset, scale, hitbox offset, hitbox scale, and logo config");
               boxBlockEntity.setFigureOffset(packet.offsetX, packet.offsetY, packet.offsetZ);
               boxBlockEntity.setFigureScale(packet.scale);
               boxBlockEntity.setHitboxOffset(packet.hitboxOffsetX, packet.hitboxOffsetY, packet.hitboxOffsetZ);
               boxBlockEntity.setHitboxScale(packet.hitboxScaleX, packet.hitboxScaleY, packet.hitboxScaleZ);
               boxBlockEntity.setLogoPosition(packet.logoPositionX, packet.logoPositionY, packet.logoPositionZ);
               boxBlockEntity.setLogoScale(packet.logoScaleX, packet.logoScaleY, packet.logoScaleZ);
               boxBlockEntity.method_5431();
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
