package com.fshop.network;

import com.fshop.data.FShopSavedData;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ResultMessages;
import com.fshop.shop.ShopNet;
import com.fshop.shop.ShopService;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Owner removes an offer and gets its remaining stock back. */
public final class RemoveOfferPacket {
   private final UUID shopId;
   private final int offerIndex;

   public RemoveOfferPacket(UUID shopId, int offerIndex) {
      this.shopId = shopId;
      this.offerIndex = offerIndex;
   }

   public static void encode(RemoveOfferPacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.shopId);
      buf.writeVarInt(packet.offerIndex);
   }

   public static RemoveOfferPacket decode(FriendlyByteBuf buf) {
      return new RemoveOfferPacket(buf.readUUID(), buf.readVarInt());
   }

   public static void handle(RemoveOfferPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender == null) {
            return;
         }
         PlayerShop shop = FShopSavedData.get(sender.serverLevel()).getShop(packet.shopId);
         ShopService.Result result = ShopService.removeOffer(sender, shop, packet.offerIndex);
         sender.sendSystemMessage(ResultMessages.of(result));
         ShopNet.openManage(sender, shop);
      });
      context.setPacketHandled(true);
   }
}
