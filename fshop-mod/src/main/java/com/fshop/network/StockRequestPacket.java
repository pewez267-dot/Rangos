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

/**
 * Owner clicked an inventory item in the manage GUI to put it on sale. The
 * SERVER decides whether it merges into an existing offer (seamless restock,
 * keeping the original price) or is a brand-new product (asks the client to
 * open the price editor). No client-side item comparison is involved, so
 * identical items always stack regardless of network-layer NBT quirks.
 */
public final class StockRequestPacket {
   private final UUID shopId;
   private final int slot;

   public StockRequestPacket(UUID shopId, int slot) {
      this.shopId = shopId;
      this.slot = slot;
   }

   public static void encode(StockRequestPacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.shopId);
      buf.writeVarInt(packet.slot);
   }

   public static StockRequestPacket decode(FriendlyByteBuf buf) {
      return new StockRequestPacket(buf.readUUID(), buf.readVarInt());
   }

   public static void handle(StockRequestPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender == null) {
            return;
         }
         PlayerShop shop = FShopSavedData.get(sender.serverLevel()).getShop(packet.shopId);
         ShopService.StockOutcome outcome = ShopService.stock(sender, shop, packet.slot);
         switch (outcome) {
            case NEEDS_PRICE -> PacketHandler.sendToPlayer(sender, new OpenPriceScreenPacket(shop, packet.slot));
            case RESTOCKED -> {
               sender.sendSystemMessage(ResultMessages.of(ShopService.Result.OK));
               ShopNet.openManage(sender, shop);
            }
            case LIMIT -> {
               sender.sendSystemMessage(ResultMessages.of(ShopService.Result.LIMIT_REACHED));
               ShopNet.openManage(sender, shop);
            }
            case NOT_OWNER -> sender.sendSystemMessage(ResultMessages.of(ShopService.Result.NOT_OWNER));
            case NO_SHOP -> sender.sendSystemMessage(ResultMessages.of(ShopService.Result.NO_SHOP));
            default -> ShopNet.openManage(sender, shop);
         }
      });
      context.setPacketHandled(true);
   }
}
