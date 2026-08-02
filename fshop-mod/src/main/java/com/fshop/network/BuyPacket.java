package com.fshop.network;

import com.fshop.config.FShopConfig;
import com.fshop.data.FShopSavedData;
import com.fshop.shop.Gate;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ResultMessages;
import com.fshop.shop.ShopNet;
import com.fshop.shop.ShopService;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Client confirms a purchase of {@code amount} items of an offer. */
public final class BuyPacket {
   private final UUID shopId;
   private final int offerIndex;
   private final int amount;

   public BuyPacket(UUID shopId, int offerIndex, int amount) {
      this.shopId = shopId;
      this.offerIndex = offerIndex;
      this.amount = amount;
   }

   public static void encode(BuyPacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.shopId);
      buf.writeVarInt(packet.offerIndex);
      buf.writeVarInt(packet.amount);
   }

   public static BuyPacket decode(FriendlyByteBuf buf) {
      return new BuyPacket(buf.readUUID(), buf.readVarInt(), buf.readVarInt());
   }

   public static void handle(BuyPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender == null) {
            return;
         }
         if (FShopConfig.REQUIRE_ZONE_FOR_BUY.get() && !Gate.inMarket(sender)) {
            return;
         }
         PlayerShop shop = FShopSavedData.get(sender.serverLevel()).getShop(packet.shopId);
         ShopService.Result result = ShopService.buy(sender, shop, packet.offerIndex, packet.amount);
         sender.sendSystemMessage(ResultMessages.of(result));
         // Refresh the shop view so stock and balance stay in sync.
         ShopNet.openShopView(sender, packet.shopId);
      });
      context.setPacketHandled(true);
   }
}
