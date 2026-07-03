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

/** Owner moves the stack in {@code slot} into their shop at {@code unitPrice}. */
public final class AddOfferPacket {
   private final UUID shopId;
   private final int slot;
   private final long unitPrice;
   private final int coin;

   public AddOfferPacket(UUID shopId, int slot, long unitPrice, int coin) {
      this.shopId = shopId;
      this.slot = slot;
      this.unitPrice = unitPrice;
      this.coin = coin;
   }

   public static void encode(AddOfferPacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.shopId);
      buf.writeVarInt(packet.slot);
      buf.writeVarLong(packet.unitPrice);
      buf.writeVarInt(packet.coin);
   }

   public static AddOfferPacket decode(FriendlyByteBuf buf) {
      return new AddOfferPacket(buf.readUUID(), buf.readVarInt(), buf.readVarLong(), buf.readVarInt());
   }

   public static void handle(AddOfferPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender == null) {
            return;
         }
         PlayerShop shop = FShopSavedData.get(sender.serverLevel()).getShop(packet.shopId);
         ShopService.Result result = ShopService.addOrRestock(sender, shop, packet.slot, packet.unitPrice, packet.coin);
         sender.sendSystemMessage(ResultMessages.of(result));
         ShopNet.openManage(sender, shop);
      });
      context.setPacketHandled(true);
   }
}
