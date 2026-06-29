package com.fantasticpass.network;

import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.data.PlayerPassData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public final class SetDisplayRankPacket {
   private final String rankId;

   public SetDisplayRankPacket(String rankId) {
      this.rankId = rankId == null ? "" : rankId;
   }

   public static void encode(SetDisplayRankPacket packet, FriendlyByteBuf buf) {
      buf.writeUtf(packet.rankId);
   }

   public static SetDisplayRankPacket decode(FriendlyByteBuf buf) {
      return new SetDisplayRankPacket(buf.readUtf());
   }

   public static void handle(SetDisplayRankPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender != null) {
            PlayerPassData data = PassCapability.getData(sender);
            if (data != null) {
               if (packet.rankId.isEmpty()) {
                  data.setDisplayedRankId(null);
                  NametagSync.syncPlayer(sender);
               } else if (!data.hasEarnedRank(packet.rankId)) {
                  sender.sendSystemMessage(Component.translatable("fantasticpass.msg.rank_not_owned", new Object[]{packet.rankId}));
               } else {
                  data.setDisplayedRankId(packet.rankId);
                  sender.sendSystemMessage(Component.translatable("fantasticpass.msg.rank_set", new Object[]{packet.rankId}));
                  NametagSync.syncPlayer(sender);
               }
            }
         }
      });
      context.setPacketHandled(true);
   }
}
