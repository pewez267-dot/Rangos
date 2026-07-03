package com.fshop.economy;

import com.fshop.config.FShopConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Item-based currency backed by the FantasticCoins (athens_coins) items. Prices
 * are expressed in the smallest unit (one bronze coin = 1). Silver and gold are
 * worth a configurable multiple of bronze.
 */
public final class CoinEconomy {
   private CoinEconomy() {
   }

   private static Item item(String id) {
      Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
      return i == null ? Items.AIR : i;
   }

   public static Item bronze() {
      return item(FShopConfig.BRONZE_COIN_ID.get());
   }

   public static Item silver() {
      return item(FShopConfig.SILVER_COIN_ID.get());
   }

   public static Item gold() {
      return item(FShopConfig.GOLD_COIN_ID.get());
   }

   public static long silverValue() {
      return FShopConfig.SILVER_VALUE.get();
   }

   public static long goldValue() {
      return FShopConfig.GOLD_VALUE.get();
   }

   /** Value (in bronze units) of a single coin item, or 0 if it isn't a coin. */
   public static long coinValue(Item i) {
      if (i == Items.AIR) {
         return 0L;
      }
      if (i == gold()) {
         return goldValue();
      }
      if (i == silver()) {
         return silverValue();
      }
      if (i == bronze()) {
         return 1L;
      }
      return 0L;
   }

   public static boolean available() {
      return bronze() != Items.AIR;
   }

   /** Total coin value carried by the player, in bronze units. */
   public static long balance(Player player) {
      long total = 0L;
      var inv = player.getInventory();
      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack stack = inv.getItem(i);
         if (!stack.isEmpty()) {
            long v = coinValue(stack.getItem());
            if (v > 0L) {
               total += v * stack.getCount();
            }
         }
      }
      return total;
   }

   private static void removeAllCoins(Player player) {
      var inv = player.getInventory();
      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack stack = inv.getItem(i);
         if (!stack.isEmpty() && coinValue(stack.getItem()) > 0L) {
            inv.setItem(i, ItemStack.EMPTY);
         }
      }
   }

   /**
    * Charge the player {@code amount} bronze units. Returns false (and changes
    * nothing) if the player can't afford it. Change is returned automatically.
    */
   public static boolean withdraw(Player player, long amount) {
      if (amount <= 0L) {
         return true;
      }
      long bal = balance(player);
      if (bal < amount) {
         return false;
      }
      removeAllCoins(player);
      deposit(player, bal - amount);
      return true;
   }

   /** Give the player {@code amount} bronze units as the fewest coins possible. */
   public static void deposit(Player player, long amount) {
      if (amount <= 0L) {
         return;
      }
      long remaining = amount;
      long g = goldValue();
      long s = silverValue();

      long goldCount = (gold() != Items.AIR && g > 0) ? remaining / g : 0L;
      remaining -= goldCount * g;
      long silverCount = (silver() != Items.AIR && s > 0) ? remaining / s : 0L;
      remaining -= silverCount * s;
      long bronzeCount = remaining; // bronze value is 1

      give(player, gold(), goldCount);
      give(player, silver(), silverCount);
      give(player, bronze(), bronzeCount);
   }

   private static void give(Player player, Item coin, long count) {
      if (coin == Items.AIR || count <= 0L) {
         return;
      }
      int max = coin.getMaxStackSize();
      while (count > 0L) {
         int n = (int) Math.min(max, count);
         ItemStack stack = new ItemStack(coin, n);
         if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
         }
         count -= n;
      }
   }

   /** Compact coin breakdown for tight UI spaces, e.g. "1o 20p 5b". */
   public static String formatShort(long amount) {
      if (amount <= 0L) {
         return "0b";
      }
      long g = goldValue();
      long s = silverValue();
      long rem = amount;
      long gc = g > 0 ? rem / g : 0L;
      rem -= gc * g;
      long sc = s > 0 ? rem / s : 0L;
      rem -= sc * s;
      StringBuilder sb = new StringBuilder();
      if (gc > 0) {
         sb.append(gc).append("o ");
      }
      if (sc > 0) {
         sb.append(sc).append("p ");
      }
      sb.append(rem).append("b");
      return sb.toString().trim();
   }

   /** Human-readable coin breakdown, e.g. "1 oro, 20 plata, 5 bronce". */
   public static String format(long amount) {
      if (amount <= 0L) {
         return "0";
      }
      long g = goldValue();
      long s = silverValue();
      long remaining = amount;
      long goldCount = g > 0 ? remaining / g : 0L;
      remaining -= goldCount * g;
      long silverCount = s > 0 ? remaining / s : 0L;
      remaining -= silverCount * s;
      long bronzeCount = remaining;

      StringBuilder sb = new StringBuilder();
      if (goldCount > 0) {
         sb.append(goldCount).append(" oro");
      }
      if (silverCount > 0) {
         if (sb.length() > 0) {
            sb.append(", ");
         }
         sb.append(silverCount).append(" plata");
      }
      if (bronzeCount > 0 || sb.length() == 0) {
         if (sb.length() > 0) {
            sb.append(", ");
         }
         sb.append(bronzeCount).append(" bronce");
      }
      return sb.toString();
   }
}
