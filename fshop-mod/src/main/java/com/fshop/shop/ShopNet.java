package com.fshop.shop;

import com.fshop.data.FShopSavedData;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.OpenBrowseScreenPacket;
import com.fshop.network.OpenManageScreenPacket;
import com.fshop.network.OpenShopViewScreenPacket;
import com.fshop.network.PacketHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Server-side helpers that push shop GUIs to a player. */
public final class ShopNet {
   private ShopNet() {
   }

   public static void openBrowse(ServerPlayer player) {
      FShopSavedData data = FShopSavedData.get(player.serverLevel());
      List<ShopSummary> summaries = new ArrayList<>();
      // Main server shop always occupies the first slot.
      PlayerShop main = data.getMainShop();
      if (main != null) {
         summaries.add(ShopSummary.of(main));
      }
      for (PlayerShop shop : data.getShops().values()) {
         if (!shop.isMain()) {
            summaries.add(ShopSummary.of(shop));
         }
      }
      PacketHandler.sendToPlayer(player, new OpenBrowseScreenPacket(summaries));
   }

   public static void openCreator(ServerPlayer player, PlayerShop mainShop) {
      if (mainShop != null) {
         PacketHandler.sendToPlayer(player, new com.fshop.network.OpenCreatorScreenPacket(mainShop));
      }
   }

   public static void openShopView(ServerPlayer player, UUID shopId) {
      FShopSavedData data = FShopSavedData.get(player.serverLevel());
      PlayerShop shop = data.getShop(shopId);
      if (shop != null) {
         if (!shop.isMain()) {
            ShopOffer.mergeDuplicates(shop.getOffers());
            data.setDirty();
         }
         long[] balances = {
               CoinEconomy.balance(player, 0),
               CoinEconomy.balance(player, 1),
               CoinEconomy.balance(player, 2)
         };
         PacketHandler.sendToPlayer(player, new OpenShopViewScreenPacket(shop, balances));
      }
   }

   public static void openManage(ServerPlayer player, PlayerShop shop) {
      if (shop != null) {
         ShopOffer.mergeDuplicates(shop.getOffers());
         FShopSavedData.get(player.serverLevel()).setDirty();
         PacketHandler.sendToPlayer(player, new OpenManageScreenPacket(shop));
      }
   }
}
