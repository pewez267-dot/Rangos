package com.fshop.shop;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** Lightweight shop info shown in the browse GUI (one entry per shop). */
public record ShopSummary(UUID id, String name, String ownerName, int offerCount, long minPrice, ItemStack icon) {

   public void toBuf(FriendlyByteBuf buf) {
      buf.writeUUID(id);
      buf.writeUtf(name);
      buf.writeUtf(ownerName);
      buf.writeVarInt(offerCount);
      buf.writeVarLong(minPrice);
      buf.writeItem(icon);
   }

   public static ShopSummary fromBuf(FriendlyByteBuf buf) {
      return new ShopSummary(buf.readUUID(), buf.readUtf(), buf.readUtf(),
            buf.readVarInt(), buf.readVarLong(), buf.readItem());
   }

   public static ShopSummary of(PlayerShop shop) {
      long min = Long.MAX_VALUE;
      ItemStack icon = ItemStack.EMPTY;
      for (ShopOffer offer : shop.getOffers()) {
         if (offer.getUnitPrice() < min) {
            min = offer.getUnitPrice();
         }
         if (icon.isEmpty()) {
            icon = offer.displayStack(1);
         }
      }
      if (min == Long.MAX_VALUE) {
         min = 0L;
      }
      return new ShopSummary(shop.getId(), shop.getName(), shop.getOwnerName(),
            shop.getOffers().size(), min, icon);
   }
}
