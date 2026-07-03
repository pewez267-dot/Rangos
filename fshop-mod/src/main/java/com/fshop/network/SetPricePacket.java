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

/** Owner updates the unit price of one of their offers. */
public final class SetPricePacket {
   private final UUID shopId;
   private final int offerIndex;
   private final long unitPrice;
   private final int coin;

   public SetPricePacket(UUID shopId, int offerIndex, long unitPrice, int coin) {
      this.shopId = shopId;
      this.offerIndex = offerIndex;
      this.unitPrice = unitPrice;
      this.coin = coin;
   }

   public static void encode(SetPricePacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.shopId);
      buf.writeVarInt(packet.offerIndex);
      buf.writeVarLong(packet.unitPrice);
      buf.writeVarInt(packet.coin);
   }

   public static SetPricePacket decode(FriendlyByteBuf buf) {
      return new SetPricePacket(buf.readUUID(), buf.readVarInt(), buf.readVarLong(), buf.readVarInt());
   }

   public static void handle(SetPricePacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender == null) {
            return;
         }
         PlayerShop shop = FShopSavedData.get(sender.serverLevel()).getShop(packet.shopId);
         ShopService.Result result = ShopService.setPrice(sender, shop, packet.offerIndex, packet.unitPrice, packet.coin);
         sender.sendSystemMessage(ResultMessages.of(result));
         ShopNet.openManage(sender, shop);
      });
      context.setPacketHandled(true);
   }
}
