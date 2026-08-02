package com.fshop.client;

import com.fshop.client.screen.MainShopCreatorScreen;
import com.fshop.client.screen.ShopBrowseScreen;
import com.fshop.client.screen.ShopManageScreen;
import com.fshop.client.screen.ShopViewScreen;
import com.fshop.client.screen.PriceInputScreen;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.OpenBrowseScreenPacket;
import com.fshop.network.OpenCreatorScreenPacket;
import com.fshop.network.OpenManageScreenPacket;
import com.fshop.network.OpenPriceScreenPacket;
import com.fshop.network.OpenShopViewScreenPacket;
import net.minecraft.client.Minecraft;

/**
 * Client-only entry points invoked by the server->client packets. Kept in a
 * separate class so it is only classloaded on the physical client.
 */
public final class ClientPacketHandler {
   private ClientPacketHandler() {
   }

   public static void openBrowse(OpenBrowseScreenPacket packet) {
      Minecraft.getInstance().setScreen(new ShopBrowseScreen(packet.getShops()));
   }

   public static void openShopView(OpenShopViewScreenPacket packet) {
      Minecraft.getInstance().setScreen(new ShopViewScreen(packet.getShop(), packet.getBalances()));
   }

   public static void openManage(OpenManageScreenPacket packet) {
      Minecraft.getInstance().setScreen(new ShopManageScreen(packet.getShop()));
   }

   public static void openPriceScreen(OpenPriceScreenPacket packet) {
      Minecraft.getInstance().setScreen(new PriceInputScreen(packet.getShop(),
            PriceInputScreen.Mode.ADD, packet.getSlot(), 1, CoinEconomy.BRONZE, 1));
   }

   public static void openCreator(OpenCreatorScreenPacket packet) {
      Minecraft.getInstance().setScreen(new MainShopCreatorScreen(packet.getShop()));
   }
}
