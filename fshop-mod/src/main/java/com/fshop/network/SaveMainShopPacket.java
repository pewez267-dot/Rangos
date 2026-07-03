package com.fshop.network;

import com.fshop.data.FShopSavedData;
import com.fshop.shop.PlayerShop;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Admin saves the main server shop ("La Moneda de Oro") built in the creator. */
public final class SaveMainShopPacket {
   private final PlayerShop shop;

   public SaveMainShopPacket(PlayerShop shop) {
      this.shop = shop;
   }

   public static void encode(SaveMainShopPacket packet, FriendlyByteBuf buf) {
      packet.shop.toBuf(buf);
   }

   public static SaveMainShopPacket decode(FriendlyByteBuf buf) {
      return new SaveMainShopPacket(PlayerShop.fromBuf(buf));
   }

   public static void handle(SaveMainShopPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender == null || !sender.hasPermissions(2)) {
            return;
         }
         PlayerShop incoming = packet.shop;
         // Security: only the fixed singleton main-shop id is accepted here.
         if (!incoming.getId().equals(FShopSavedData.MAIN_SHOP_ID)) {
            return;
         }
         incoming.setMain(true);
         if (incoming.getOwner() == null) {
            incoming.setOwner(sender.getUUID());
         }
         FShopSavedData data = FShopSavedData.get(sender.serverLevel());
         data.putShop(incoming);
         sender.sendSystemMessage(Component.literal("[FShop] Tienda principal guardada: \"" + incoming.getName()
               + "\" (" + incoming.getOffers().size() + " items).").withStyle(ChatFormatting.GREEN));
      });
      context.setPacketHandled(true);
   }
}
