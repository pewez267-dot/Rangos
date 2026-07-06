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
      OK, NO_SHOP, NO_OFFER, NOT_OWNER, OUT_OF_STOCK, CANNOT_AFFORD,
      INVENTORY_FULL, INVALID, NO_CURRENCY, LIMIT_REACHED
   }

   /** Outcome of a server-authoritative stock request coming from the manage GUI. */
   public enum StockOutcome {
      RESTOCKED, NEEDS_PRICE, LIMIT, INVALID, NOT_OWNER, NO_SHOP
   }

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
      // amount = number of bundles; items delivered = bundle * amount
      int items = offer.getBundle() * amount;
      if (!offer.isInfinite() && offer.getStock() < items) {
         return Result.OUT_OF_STOCK;
      }
      long total = offer.getUnitPrice() * (long) amount;
      if (CoinEconomy.balance(buyer, offer.getCoin()) < total) {
         return Result.CANNOT_AFFORD;
      }
      if (!hasRoomFor(buyer, offer, items)) {
         return Result.INVENTORY_FULL;
      }
      if (!CoinEconomy.withdraw(buyer, offer.getCoin(), total)) {
         return Result.CANNOT_AFFORD;
      }
      giveItems(buyer, offer, items);
      if (!offer.isInfinite()) {
         offer.addStock(-items);
      }
      // Every shop (including the main server shop) accumulates its earnings so
      // an admin can collect the gold-coin shop's income.
      shop.addEarnings(offer.getCoin(), total);
      FShopSavedData.get(buyer.serverLevel()).setDirty();
      return Result.OK;
   }

   /** Move the whole stack in {@code slot} into the shop at the given price/coin/bundle. */
   public static Result addOrRestock(ServerPlayer owner, PlayerShop shop, int slot, long unitPrice, int coin, int bundle) {
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
      if (stack.isEmpty() || isCoin(stack)) {
         return Result.INVALID;
      }
      long price = clampPrice(unitPrice);
      int count = stack.getCount();
      ShopOffer existing = findMatching(shop, stack);
      if (existing != null) {
         // The item already has an offer: just add the new stock and KEEP the
         // price/coin/bundle the owner set originally. Restocking must never
         // silently change (or re-prompt for) the price already in place.
         existing.addStock(count);
      } else {
         if (shop.getOffers().size() >= FShopConfig.MAX_OFFERS_PER_SHOP.get()) {
            return Result.LIMIT_REACHED;
         }
         ShopOffer offer = new ShopOffer(stack, price, coin, count);
         offer.setBundle(bundle);
         shop.getOffers().add(offer);
      }
      inv.setItem(slot, ItemStack.EMPTY);
      // Collapse any pre-existing duplicate offers of the same product into one.
      ShopOffer.mergeDuplicates(shop.getOffers());
      FShopSavedData.get(owner.serverLevel()).setDirty();
      return Result.OK;
   }

   /**
    * Server-authoritative stocking entry point used by the manage GUI. The
    * SERVER (never the client) decides whether the clicked item merges into an
    * existing offer -- using its own, non-network-altered ItemStacks -- so two
    * identical items ALWAYS stack seamlessly, and the price editor only ever
    * opens for a genuinely new product. This removes the old client-side
    * matching, which could wrongly split identical items into separate offers
    * because of NBT the networking layer rewrites in transit.
    */
   public static StockOutcome stock(ServerPlayer owner, PlayerShop shop, int slot) {
      if (shop == null) {
         return StockOutcome.NO_SHOP;
      }
      if (!shop.getOwner().equals(owner.getUUID())) {
         return StockOutcome.NOT_OWNER;
      }
      var inv = owner.getInventory();
      if (slot < 0 || slot >= inv.getContainerSize()) {
         return StockOutcome.INVALID;
      }
      ItemStack stack = inv.getItem(slot);
      if (stack.isEmpty() || isCoin(stack)) {
         return StockOutcome.INVALID;
      }
      ShopOffer existing = findMatching(shop, stack);
      if (existing != null) {
         // Identical product already on sale: add the stock and KEEP the price,
         // coin and bundle the owner set originally -- never re-prompt.
         existing.addStock(stack.getCount());
         inv.setItem(slot, ItemStack.EMPTY);
         ShopOffer.mergeDuplicates(shop.getOffers());
         FShopSavedData.get(owner.serverLevel()).setDirty();
         return StockOutcome.RESTOCKED;
      }
      if (shop.getOffers().size() >= FShopConfig.MAX_OFFERS_PER_SHOP.get()) {
         return StockOutcome.LIMIT;
      }
      return StockOutcome.NEEDS_PRICE;
   }

   public static Result setPrice(ServerPlayer owner, PlayerShop shop, int offerIndex, long unitPrice, int coin, int bundle) {
      if (shop == null) {
         return Result.NO_SHOP;
      }
      if (!shop.getOwner().equals(owner.getUUID())) {
         return Result.NOT_OWNER;
      }
      if (offerIndex < 0 || offerIndex >= shop.getOffers().size()) {
         return Result.NO_OFFER;
      }
      ShopOffer offer = shop.getOffers().get(offerIndex);
      offer.setUnitPrice(clampPrice(unitPrice));
      offer.setCoin(coin);
      offer.setBundle(bundle);
      FShopSavedData.get(owner.serverLevel()).setDirty();
      return Result.OK;
   }

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
      if (shop.totalPendingEarnings() <= 0L) {
         return Result.INVALID;
      }
      for (int coin = 0; coin < 3; coin++) {
         CoinEconomy.deposit(owner, coin, shop.getPendingEarnings(coin));
      }
      shop.clearEarnings();
      FShopSavedData.get(owner.serverLevel()).setDirty();
      return Result.OK;
   }

   private static long clampPrice(long price) {
      return Math.max(1L, Math.min(price, FShopConfig.MAX_UNIT_PRICE.get()));
   }

   private static boolean isCoin(ItemStack s) {
      return s.getItem() == CoinEconomy.coinItem(0)
            || s.getItem() == CoinEconomy.coinItem(1)
            || s.getItem() == CoinEconomy.coinItem(2);
   }

   private static ShopOffer findMatching(PlayerShop shop, ItemStack stack) {
      for (ShopOffer offer : shop.getOffers()) {
         if (ShopOffer.matchesForMerge(offer.getItem(), stack)) {
            return offer;
         }
      }
      return null;
   }

   private static boolean hasRoomFor(ServerPlayer player, ShopOffer offer, int amount) {
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
