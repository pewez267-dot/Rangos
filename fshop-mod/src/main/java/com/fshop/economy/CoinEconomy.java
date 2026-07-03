package com.fshop.economy;

import com.fshop.config.FShopConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Item-based currency using the three FantasticCoins (Athens Coins) items as
 * three INDEPENDENT currencies (bronze, silver, gold). Prices are a whole
 * number of a chosen coin type; there is no conversion between types.
 */
public final class CoinEconomy {
   public static final int BRONZE = 0;
   public static final int SILVER = 1;
   public static final int GOLD = 2;

   private CoinEconomy() {
   }

   private static Item byId(String id) {
      Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
      return i == null ? Items.AIR : i;
   }

   public static Item coinItem(int type) {
      return switch (type) {
         case GOLD -> byId(FShopConfig.GOLD_COIN_ID.get());
         case SILVER -> byId(FShopConfig.SILVER_COIN_ID.get());
         default -> byId(FShopConfig.BRONZE_COIN_ID.get());
      };
   }

   public static ItemStack coinIcon(int type) {
      Item i = coinItem(type);
      return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i);
   }

   public static String coinKey(int type) {
      return switch (type) {
         case GOLD -> "fshop.coin.gold";
         case SILVER -> "fshop.coin.silver";
         default -> "fshop.coin.bronze";
      };
   }

   public static boolean available() {
      return coinItem(BRONZE) != Items.AIR || coinItem(SILVER) != Items.AIR || coinItem(GOLD) != Items.AIR;
   }

   /** How many coins of {@code type} the player carries. */
   public static long balance(Player player, int type) {
      Item coin = coinItem(type);
      if (coin == Items.AIR) {
         return 0L;
      }
      long total = 0L;
      var inv = player.getInventory();
      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack s = inv.getItem(i);
         if (s.getItem() == coin) {
            total += s.getCount();
         }
      }
      return total;
   }

   /** Remove {@code count} coins of {@code type}; false (no change) if short. */
   public static boolean withdraw(Player player, int type, long count) {
      if (count <= 0L) {
         return true;
      }
      if (balance(player, type) < count) {
         return false;
      }
      Item coin = coinItem(type);
      long remaining = count;
      var inv = player.getInventory();
      for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
         ItemStack s = inv.getItem(i);
         if (s.getItem() == coin) {
            int take = (int) Math.min(s.getCount(), remaining);
            s.shrink(take);
            remaining -= take;
         }
      }
      return true;
   }

   /** Give the player {@code count} coins of {@code type}. */
   public static void deposit(Player player, int type, long count) {
      Item coin = coinItem(type);
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
}
