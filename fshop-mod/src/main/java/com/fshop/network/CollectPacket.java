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

/** Owner collects the pending coin earnings from one of their shops. */
public final class CollectPacket {
   private final UUID shopId;

   public CollectPacket(UUID shopId) {
      this.shopId = shopId;
   }

   public static void encode(CollectPacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.shopId);
   }

   public static CollectPacket decode(FriendlyByteBuf buf) {
      return new CollectPacket(buf.readUUID());
   }

   public static void handle(CollectPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender == null) {
            return;
         }
         PlayerShop shop = FShopSavedData.get(sender.serverLevel()).getShop(packet.shopId);
         ShopService.Result result = ShopService.collect(sender, shop);
         sender.sendSystemMessage(ResultMessages.of(result));
         ShopNet.openManage(sender, shop);
      });
      context.setPacketHandled(true);
   }
}
