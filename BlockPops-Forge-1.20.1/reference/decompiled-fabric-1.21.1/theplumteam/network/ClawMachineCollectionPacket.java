package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.blockentity.ClawMachineBlockEntity;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import net.minecraft.class_2338;
import net.minecraft.class_2540;
import net.minecraft.class_2586;
import net.minecraft.class_3222;
import net.minecraft.class_5455;
import net.minecraft.class_9129;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClawMachineCollectionPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(ClawMachineCollectionPacket.class);
   private final class_2338 pos;
   private final String collectionId;

   public ClawMachineCollectionPacket(class_2338 pos, String collectionId) {
      this.pos = pos;
      this.collectionId = collectionId;
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_10807(this.pos);
      buffer.method_10814(this.collectionId);
      return buffer;
   }

   public static ClawMachineCollectionPacket decode(class_2540 buffer) {
      class_2338 pos = buffer.method_10811();
      String collectionId = buffer.method_19772();
      return new ClawMachineCollectionPacket(pos, collectionId);
   }

   public static void handleServer(class_2540 buf, PacketContext context) {
      ClawMachineCollectionPacket packet = decode(buf);
      BlockPopsMod.logDebug("Received ClawMachineCollectionPacket on server - Position: {}, Collection ID: {}", packet.pos, packet.collectionId);
      context.queue(() -> {
         if (context.getPlayer() instanceof class_3222 player) {
            class_2586 blockEntity = player.method_37908().method_8321(packet.pos);
            if (blockEntity instanceof ClawMachineBlockEntity clawMachineBlockEntity) {
               BlockPopsMod.logDebug("Setting collection ID on ClawMachineBlockEntity");
               clawMachineBlockEntity.setCollectionId(packet.collectionId);
               player.method_37908().method_8413(packet.pos, blockEntity.method_11010(), blockEntity.method_11010(), 3);
               BlockPopsMod.logDebug("Collection ID updated successfully");
            } else {
               LOGGER.warn("BlockEntity at {} is not a ClawMachineBlockEntity", packet.pos);
            }
         }
      });
   }

   public void sendToServer() {
      NetworkManager.sendToServer(ModNetworking.CLAW_MACHINE_COLLECTION, this.encode());
   }
}
