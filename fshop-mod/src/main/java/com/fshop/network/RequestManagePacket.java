package com.fshop.network;

import com.fshop.data.FShopSavedData;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ShopNet;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Client requests to (re)open the management GUI for one of its shops. */
public final class RequestManagePacket {
   private final UUID shopId;

   public RequestManagePacket(UUID shopId) {
      this.shopId = shopId;
   }

   public static void encode(RequestManagePacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.shopId);
   }

   public static RequestManagePacket decode(FriendlyByteBuf buf) {
      return new RequestManagePacket(buf.readUUID());
   }

   public static void handle(RequestManagePacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender == null) {
            return;
         }
         PlayerShop shop = FShopSavedData.get(sender.serverLevel()).getShop(packet.shopId);
         if (shop != null && shop.getOwner().equals(sender.getUUID())) {
            ShopNet.openManage(sender, shop);
         }
      });
      context.setPacketHandled(true);
   }
}
