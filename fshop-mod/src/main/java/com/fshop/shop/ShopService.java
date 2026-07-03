package com.fshop.shop;

import com.fshop.config.FShopConfig;
import com.fshop.data.FShopSavedData;
import com.fshop.economy.CoinEconomy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative operations for buying, stocking and managing shops. */
public final class ShopService {
   private ShopService() {
   }

   public enum Result {
      OK,
      NO_SHOP,
      NO_OFFER,
      NOT_OWNER,
      OUT_OF_STOCK,
      CANNOT_AFFORD,
      INVENTORY_FULL,
      INVALID,
      NO_CURRENCY,
      LIMIT_REACHED
   }

   /** Buy {@code amount} items of the given offer from a shop. */
   public static Result buy(ServerPlayer buyer, PlayerShop shop, int offerIndex, int amount) {
      if (shop == null) {
         return Result.NO_SHOP;
      }
      if (offerIndex < 0 || offerIndex >= shop.getOffers().size()) {
         return Result.NO_OFFER;
      }
      if (amount <= 0) {
         return Result.INVALID;
      }
      if (!CoinEconomy.available()) {
         return Result.NO_CURRENCY;
      }
      ShopOffer offer = shop.getOffers().get(offerIndex);
      if (offer.getStock() < amount) {
         return Result.OUT_OF_STOCK;
      }
      long total = offer.getUnitPrice() * (long) amount;
      if (CoinEconomy.balance(buyer) < total) {
         return Result.CANNOT_AFFORD;
      }
      if (!hasRoomFor(buyer, offer, amount)) {
         return Result.INVENTORY_FULL;
      }
      if (!CoinEconomy.withdraw(buyer, total)) {
         return Result.CANNOT_AFFORD;
      }
      giveItems(buyer, offer, amount);
      offer.addStock(-amount);
      shop.addEarnings(total);
      FShopSavedData.get(buyer.serverLevel()).setDirty();
      return Result.OK;
   }

   /**
    * Move the entire stack in {@code slot} into the shop as stock. If an offer
    * for the same item already exists its stock is increased and the price is
    * updated; otherwise a new offer is created.
    */
   public static Result addOrRestock(ServerPlayer owner, PlayerShop shop, int slot, long unitPrice) {
      if (shop == null) {
         return Result.NO_SHOP;
      }
      if (!shop.getOwner().equals(owner.getUUID())) {
         return Result.NOT_OWNER;
      }
      var inv = owner.getInventory();
      if (slot < 0 || slot >= inv.getContainerSize()) {
         return Result.INVALID;
      }
      ItemStack stack = inv.getItem(slot);
      if (stack.isEmpty() || CoinEconomy.coinValue(stack.getItem()) > 0L) {
         return Result.INVALID; // never allow stocking coins
      }
      long price = clampPrice(unitPrice);
      int count = stack.getCount();
      ShopOffer existing = findMatching(shop, stack);
      if (existing != null) {
         existing.addStock(count);
         existing.setUnitPrice(price);
      } else {
         if (shop.getOffers().size() >= FShopConfig.MAX_OFFERS_PER_SHOP.get()) {
            return Result.LIMIT_REACHED;
         }
         shop.getOffers().add(new ShopOffer(stack, price, count));
      }
      inv.setItem(slot, ItemStack.EMPTY);
      FShopSavedData.get(owner.serverLevel()).setDirty();
      return Result.OK;
   }

   /** Update the price of an existing offer. */
   public static Result setPrice(ServerPlayer owner, PlayerShop shop, int offerIndex, long unitPrice) {
      if (shop == null) {
         return Result.NO_SHOP;
      }
      if (!shop.getOwner().equals(owner.getUUID())) {
         return Result.NOT_OWNER;
      }
      if (offerIndex < 0 || offerIndex >= shop.getOffers().size()) {
         return Result.NO_OFFER;
      }
      shop.getOffers().get(offerIndex).setUnitPrice(clampPrice(unitPrice));
      FShopSavedData.get(owner.serverLevel()).setDirty();
      return Result.OK;
   }

   /** Remove an offer and return its remaining stock to the owner. */
   public static Result removeOffer(ServerPlayer owner, PlayerShop shop, int offerIndex) {
      if (shop == null) {
         return Result.NO_SHOP;
      }
      if (!shop.getOwner().equals(owner.getUUID())) {
         return Result.NOT_OWNER;
      }
      if (offerIndex < 0 || offerIndex >= shop.getOffers().size()) {
         return Result.NO_OFFER;
      }
      ShopOffer offer = shop.getOffers().remove(offerIndex);
      giveItems(owner, offer, offer.getStock());
      FShopSavedData.get(owner.serverLevel()).setDirty();
      return Result.OK;
   }

   /** Deposit the shop's pending earnings to the owner's inventory as coins. */
   public static Result collect(ServerPlayer owner, PlayerShop shop) {
      if (shop == null) {
         return Result.NO_SHOP;
      }
      if (!shop.getOwner().equals(owner.getUUID())) {
         return Result.NOT_OWNER;
      }
      if (!CoinEconomy.available()) {
         return Result.NO_CURRENCY;
      }
      long amount = shop.getPendingEarnings();
      if (amount <= 0L) {
         return Result.INVALID;
      }
      CoinEconomy.deposit(owner, amount);
      shop.clearEarnings();
      FShopSavedData.get(owner.serverLevel()).setDirty();
      return Result.OK;
   }

   // Helpers ---------------------------------------------------------------
   private static long clampPrice(long price) {
      return Math.max(0L, Math.min(price, FShopConfig.MAX_UNIT_PRICE.get()));
   }

   private static ShopOffer findMatching(PlayerShop shop, ItemStack stack) {
      for (ShopOffer offer : shop.getOffers()) {
         if (ItemStack.isSameItemSameTags(offer.getItem(), single(stack))) {
            return offer;
         }
      }
      return null;
   }

   private static ItemStack single(ItemStack stack) {
      ItemStack s = stack.copy();
      s.setCount(1);
      return s;
   }

   private static boolean hasRoomFor(ServerPlayer player, ShopOffer offer, int amount) {
      // Simulate insertion on a copy of the inventory.
      var inv = player.getInventory();
      int maxStack = offer.getItem().getMaxStackSize();
      int free = 0;
      for (int i = 0; i < inv.items.size(); i++) {
         ItemStack s = inv.items.get(i);
         if (s.isEmpty()) {
            free += maxStack;
         } else if (ItemStack.isSameItemSameTags(s, offer.getItem())) {
            free += Math.max(0, maxStack - s.getCount());
         }
      }
      return free >= amount;
   }

   private static void giveItems(ServerPlayer player, ShopOffer offer, int amount) {
      int remaining = amount;
      int maxStack = offer.getItem().getMaxStackSize();
      while (remaining > 0) {
         int n = Math.min(maxStack, remaining);
         ItemStack give = offer.displayStack(n);
         if (!player.getInventory().add(give)) {
            player.drop(give, false);
         }
         remaining -= n;
      }
   }
}
