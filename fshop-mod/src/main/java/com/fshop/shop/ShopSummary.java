package com.fshop.shop;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** Lightweight shop info shown in the browse GUI (one entry per shop). */
public record ShopSummary(UUID id, UUID ownerId, String name, String ownerName, int offerCount,
      long minPrice, int minCoin, ItemStack icon, boolean main) {

   public void toBuf(FriendlyByteBuf buf) {
      buf.writeUUID(id);
      buf.writeUUID(ownerId);
      buf.writeUtf(name);
      buf.writeUtf(ownerName);
      buf.writeVarInt(offerCount);
      buf.writeVarLong(minPrice);
      buf.writeVarInt(minCoin);
      buf.writeItem(icon);
      buf.writeBoolean(main);
   }

   public static ShopSummary fromBuf(FriendlyByteBuf buf) {
      return new ShopSummary(buf.readUUID(), buf.readUUID(), buf.readUtf(), buf.readUtf(),
            buf.readVarInt(), buf.readVarLong(), buf.readVarInt(), buf.readItem(), buf.readBoolean());
   }

   public static ShopSummary of(PlayerShop shop) {
      long min = Long.MAX_VALUE;
      int minCoin = 0;
      ItemStack icon = ItemStack.EMPTY;
      for (ShopOffer offer : shop.getOffers()) {
         if (offer.getUnitPrice() < min) {
            min = offer.getUnitPrice();
            minCoin = offer.getCoin();
         }
         if (icon.isEmpty()) {
            icon = offer.displayStack(1);
         }
      }
      if (min == Long.MAX_VALUE) {
         min = 0L;
      }
      // The main shop shows its own configured icon; regular shops fall back to
      // their first item (the browse screen renders the owner head for those).
      if (shop.isMain() && !shop.getIcon().isEmpty()) {
         icon = shop.getIcon();
      }
      return new ShopSummary(shop.getId(), shop.getOwner(), shop.getName(), shop.getOwnerName(),
            shop.getOffers().size(), min, minCoin, icon, shop.isMain());
   }
}
