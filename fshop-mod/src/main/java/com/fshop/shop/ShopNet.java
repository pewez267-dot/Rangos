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
      for (PlayerShop shop : data.getShops().values()) {
         summaries.add(ShopSummary.of(shop));
      }
      PacketHandler.sendToPlayer(player, new OpenBrowseScreenPacket(summaries));
   }

   public static void openShopView(ServerPlayer player, UUID shopId) {
      FShopSavedData data = FShopSavedData.get(player.serverLevel());
      PlayerShop shop = data.getShop(shopId);
      if (shop != null) {
         PacketHandler.sendToPlayer(player, new OpenShopViewScreenPacket(shop, CoinEconomy.balance(player)));
      }
   }

   public static void openManage(ServerPlayer player, PlayerShop shop) {
      if (shop != null) {
         PacketHandler.sendToPlayer(player, new OpenManageScreenPacket(shop));
      }
   }
}
