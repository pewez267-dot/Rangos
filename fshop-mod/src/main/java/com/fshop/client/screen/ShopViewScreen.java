package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.client.ShopWidgets;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.PacketHandler;
import com.fshop.network.RequestBrowsePacket;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ShopOffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Buy GUI for a single shop: offers in the window, nav + balance below. */
public final class ShopViewScreen extends Screen {
   private final PlayerShop shop;
   private final long[] balances;
   private int page;
   private int left;
   private int top;

   public ShopViewScreen(PlayerShop shop, long[] balances) {
      super(Component.literal(shop.getName()));
      this.shop = shop;
      this.balances = balances;
   }

   @Override
   protected void init() {
      this.left = (this.width - FShopTextures.GW) / 2;
      this.top = (this.height - FShopTextures.GH) / 2;
   }

   private int perPage() {
      return FShopTextures.contentCells();
   }

   private int pageCount() {
      return Math.max(1, (shop.getOffers().size() + perPage() - 1) / perPage());
   }

   private int backX() {
      return left + 48;
   }

   private int prevX() {
      return left + 150;
   }

   private int nextX() {
      return left + 186;
   }

   private int navY() {
      return top + 226;
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.ITEM_DISPLAY, left, top);

      List<ShopOffer> offers = shop.getOffers();
      int start = page * perPage();
      int hovered = -1;
      for (int i = 0; i < perPage() && start + i < offers.size(); i++) {
         int cx = left + FShopTextures.contentCellX(i);
         int cy = top + FShopTextures.contentCellY(i);
         boolean hov = FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL, FShopTextures.CELL);
         if (hov) {
            g.fill(cx, cy, cx + FShopTextures.CELL, cy + FShopTextures.CELL, 0x6682CD47);
            hovered = start + i;
         }
         ShopOffer offer = offers.get(start + i);
         int ix = left + FShopTextures.contentItemX(i);
         int iy = top + FShopTextures.contentItemY(i);
         g.renderFakeItem(offer.displayStack(1), ix, iy);
         if (offer.getStock() <= 0) {
            g.fill(ix, iy, ix + 16, iy + 16, 0x99DF2E38);
         }
      }

      ShopWidgets.dimBottom(g, left, top);
      g.drawCenteredString(this.font, shop.getName(), left + 128, top + 174, FShopTheme.GOLD);

      // player balance for the three coins with icons, centred in the panel
      int[] coins = {CoinEconomy.GOLD, CoinEconomy.SILVER, CoinEconomy.BRONZE};
      int[] gc = {left + 82, left + 128, left + 174};
      for (int i = 0; i < 3; i++) {
         g.renderFakeItem(CoinEconomy.coinIcon(coins[i]), gc[i] - 9, top + 191);
         g.drawString(this.font, "x" + balances[coins[i]], gc[i] + 8, top + 195, FShopTheme.TEXT, false);
      }

      // navigation (wood-brown accent, matches the storefront's own colours)
      boolean backHov = FShopTheme.inside(mouseX, mouseY, backX(), navY(), 54, 18);
      FShopTheme.button(g, backX(), navY(), 54, 18, FShopTheme.SELL, backHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.back"), backX() + 27, navY() + 5, FShopTheme.WOOD_TEXT);
      boolean hp = page > 0 && FShopTheme.inside(mouseX, mouseY, prevX(), navY(), 20, 18);
      boolean hn = page < pageCount() - 1 && FShopTheme.inside(mouseX, mouseY, nextX(), navY(), 20, 18);
      FShopTheme.button(g, prevX(), navY(), 20, 18, page > 0 ? FShopTheme.SELL : FShopTheme.BORDER, hp);
      FShopTheme.button(g, nextX(), navY(), 20, 18, page < pageCount() - 1 ? FShopTheme.SELL : FShopTheme.BORDER, hn);
      g.drawCenteredString(this.font, "<", prevX() + 10, navY() + 5, FShopTheme.WOOD_TEXT);
      g.drawCenteredString(this.font, ">", nextX() + 10, navY() + 5, FShopTheme.WOOD_TEXT);
      g.drawCenteredString(this.font, (page + 1) + "/" + pageCount(), prevX() + 28, navY() + 5, FShopTheme.TEXT_DIM);

      super.render(g, mouseX, mouseY, partial);
      if (hovered >= 0) {
         offerTooltip(g, offers.get(hovered), mouseX, mouseY);
      } else if (backHov) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.nav.back_to_list"));
      } else if (hp) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.nav.prev"));
      } else if (hn) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.nav.next"));
      }
   }

   private void tip(GuiGraphics g, int mouseX, int mouseY, Component c) {
      List<Component> t = new ArrayList<>();
      t.add(c);
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   private void offerTooltip(GuiGraphics g, ShopOffer offer, int mouseX, int mouseY) {
      List<Component> t = new ArrayList<>();
      t.add(offer.displayStack(1).getHoverName());
      t.add(Component.translatable("fshop.gui.buy_price", offer.getUnitPrice(),
            Component.translatable(CoinEconomy.coinKey(offer.getCoin()))).withStyle(ChatFormatting.GREEN));
      t.add(Component.translatable("fshop.gui.stock", offer.getStock())
            .withStyle(offer.getStock() > 0 ? ChatFormatting.GRAY : ChatFormatting.RED));
      t.add(Component.empty());
      t.add(Component.translatable("fshop.gui.click_to_buy").withStyle(ChatFormatting.GREEN));
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      List<ShopOffer> offers = shop.getOffers();
      int start = page * perPage();
      if (button == 0) {
         for (int i = 0; i < perPage() && start + i < offers.size(); i++) {
            int cx = left + FShopTextures.contentCellX(i);
            int cy = top + FShopTextures.contentCellY(i);
            if (FShopTheme.inside(mx, my, cx, cy, FShopTextures.CELL, FShopTextures.CELL)) {
               int idx = start + i;
               if (offers.get(idx).getStock() > 0) {
                  this.minecraft.setScreen(new AmountScreen(shop, idx, balances));
               }
               return true;
            }
         }
         if (FShopTheme.inside(mx, my, backX(), navY(), 54, 18)) {
            PacketHandler.sendToServer(new RequestBrowsePacket());
            return true;
         }
         if (page > 0 && FShopTheme.inside(mx, my, prevX(), navY(), 20, 18)) {
            page--;
            return true;
         }
         if (page < pageCount() - 1 && FShopTheme.inside(mx, my, nextX(), navY(), 20, 18)) {
            page++;
            return true;
         }
      }
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
