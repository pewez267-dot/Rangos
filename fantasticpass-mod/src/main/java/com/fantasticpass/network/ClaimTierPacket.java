package com.fantasticpass.network;

import com.fantasticpass.progression.RewardDispatcher;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public final class ClaimTierPacket {
   private final int tier;

   public ClaimTierPacket(int tier) {
      this.tier = tier;
   }

   public static void encode(ClaimTierPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.tier);
   }

   public static ClaimTierPacket decode(FriendlyByteBuf buf) {
      return new ClaimTierPacket(buf.readVarInt());
   }

   public static void handle(ClaimTierPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender != null) {
            RewardDispatcher.ClaimResult result = RewardDispatcher.claim(sender, packet.tier);
            sender.sendSystemMessage(RewardDispatcher.messageFor(result, packet.tier));
            com.fantasticpass.data.PlayerPassData data = com.fantasticpass.capability.PassCapability.getData(sender);
            if (data != null) {
               PacketHandler.sendToPlayer(sender, new ClaimResultPacket(result, packet.tier, data));
            }
         }
      });
      context.setPacketHandled(true);
   }
}
