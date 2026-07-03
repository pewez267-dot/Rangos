package com.fshop.network;

import com.fshop.config.FShopConfig;
import com.fshop.shop.Gate;
import com.fshop.shop.ShopNet;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Client requests to open a specific shop's buy GUI. */
public final class OpenShopRequestPacket {
   private final UUID shopId;

   public OpenShopRequestPacket(UUID shopId) {
      this.shopId = shopId;
   }

   public static void encode(OpenShopRequestPacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.shopId);
   }

   public static OpenShopRequestPacket decode(FriendlyByteBuf buf) {
      return new OpenShopRequestPacket(buf.readUUID());
   }

   public static void handle(OpenShopRequestPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender == null) {
            return;
         }
         if (FShopConfig.REQUIRE_ZONE_FOR_BUY.get() && !Gate.inMarket(sender)) {
            return;
         }
         ShopNet.openShopView(sender, packet.shopId);
      });
      context.setPacketHandled(true);
   }
}
