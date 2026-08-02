package com.fshop.network;

import com.fshop.config.FShopConfig;
import com.fshop.shop.Gate;
import com.fshop.shop.ShopNet;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Client asks the server to (re)open the shop browse GUI. */
public final class RequestBrowsePacket {
   public RequestBrowsePacket() {
   }

   public static void encode(RequestBrowsePacket packet, FriendlyByteBuf buf) {
   }

   public static RequestBrowsePacket decode(FriendlyByteBuf buf) {
      return new RequestBrowsePacket();
   }

   public static void handle(RequestBrowsePacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender == null) {
            return;
         }
         if (FShopConfig.REQUIRE_ZONE_FOR_BUY.get() && !Gate.inMarket(sender)) {
            return;
         }
         ShopNet.openBrowse(sender);
      });
      context.setPacketHandled(true);
   }
}
