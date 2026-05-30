package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.blockentity.ClawMachineBlockEntity;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClawMachineCollectionPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(ClawMachineCollectionPacket.class);
   private final BlockPos pos;
   private final String collectionId;

   public ClawMachineCollectionPacket(BlockPos pos, String collectionId) {
      this.pos = pos;
      this.collectionId = collectionId;
   }

   public FriendlyByteBuf encode() {
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      buffer.writeBlockPos(this.pos);
      buffer.writeUtf(this.collectionId);
      return buffer;
   }

   public static ClawMachineCollectionPacket decode(FriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      String collectionId = buffer.readUtf();
      return new ClawMachineCollectionPacket(pos, collectionId);
   }

   public static void handleServer(FriendlyByteBuf buf, PacketContext context) {
      ClawMachineCollectionPacket packet = decode(buf);
      BlockPopsMod.logDebug("Received ClawMachineCollectionPacket on server - Position: {}, Collection ID: {}", packet.pos, packet.collectionId);
      context.queue(() -> {
         if (context.getPlayer() instanceof ServerPlayer player) {
            BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);
            if (blockEntity instanceof ClawMachineBlockEntity clawMachineBlockEntity) {
               BlockPopsMod.logDebug("Setting collection ID on ClawMachineBlockEntity");
               clawMachineBlockEntity.setCollectionId(packet.collectionId);
               player.level().sendBlockUpdated(packet.pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
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
