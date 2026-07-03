package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.client.Sfx;
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

/**
 * Buy GUI for a single shop: offers in the wooden window (with their real stock
 * shown as the count, even above 64), the buyer's coin wallet on the gray grid,
 * the house icon returns to the shop list, and the bottom-corner arrows page
 * through the offers (also scrollable with the mouse wheel).
 */
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

   private boolean hasPrev() {
      return page > 0;
   }

   private boolean hasNext() {
      return page < pageCount() - 1;
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.ITEM_DISPLAY, left, top);

      int coinHov = ShopWidgets.renderCoins(g, this.font, this.minecraft.player,
            left, top, mouseX, mouseY, -1);

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
         if (offer.getBundle() > 1) {
            FShopTheme.drawCount(g, this.font, ix, iy, Integer.toString(offer.getBundle()));
         }
         if (!offer.isInfinite() && offer.getStock() < offer.getBundle()) {
            g.fill(ix, iy, ix + 16, iy + 16, 0x99DF2E38);
         }
      }

      boolean homeHov = FShopTextures.inCell(mouseX, mouseY, left, top, FShopTextures.HOME_CELL);
      FShopTextures.hoverCell(g, left, top, FShopTextures.HOME_CELL, homeHov);

      boolean hp = hasPrev() && FShopTextures.inCell(mouseX, mouseY, left, top, FShopTextures.PAGE_PREV_CELL);
      boolean hn = hasNext() && FShopTextures.inCell(mouseX, mouseY, left, top, FShopTextures.PAGE_NEXT_CELL);
      if (hasPrev()) {
         FShopTextures.blitIcon(g, FShopTextures.BACK_BUTTON, left, top, FShopTextures.PAGE_PREV_CELL);
         FShopTextures.hoverCell(g, left, top, FShopTextures.PAGE_PREV_CELL, hp);
      }
      if (hasNext()) {
         FShopTextures.blitIcon(g, FShopTextures.NEXT_BUTTON, left, top, FShopTextures.PAGE_NEXT_CELL);
         FShopTextures.hoverCell(g, left, top, FShopTextures.PAGE_NEXT_CELL, hn);
      }

      super.render(g, mouseX, mouseY, partial);
      if (hovered >= 0) {
         offerTooltip(g, offers.get(hovered), mouseX, mouseY);
      } else if (coinHov >= 0) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.wallet",
               balances[coinHov], Component.translatable(CoinEconomy.coinKey(coinHov))));
      } else if (homeHov) {
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
      if (offer.getBundle() > 1) {
         t.add(Component.translatable("fshop.gui.per_pack", offer.getBundle()).withStyle(ChatFormatting.AQUA));
      }
      t.add(Component.translatable(offer.getBundle() > 1 ? "fshop.gui.price_pack" : "fshop.gui.buy_price",
            offer.getUnitPrice(), Component.translatable(CoinEconomy.coinKey(offer.getCoin()))).withStyle(ChatFormatting.GREEN));
      t.add(Component.translatable("fshop.gui.your_balance", balances[offer.getCoin()],
            Component.translatable(CoinEconomy.coinKey(offer.getCoin()))).withStyle(ChatFormatting.GRAY));
      if (offer.isInfinite()) {
         t.add(Component.translatable("fshop.gui.stock_inf").withStyle(ChatFormatting.AQUA));
      } else {
         t.add(Component.translatable("fshop.gui.stock", offer.getStock())
               .withStyle(offer.getStock() > 0 ? ChatFormatting.GRAY : ChatFormatting.RED));
      }
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
               ShopOffer o = offers.get(idx);
               if (o.isInfinite() || o.getStock() >= o.getBundle()) {
                  Sfx.select();
                  this.minecraft.setScreen(new AmountScreen(shop, idx, balances));
               }
               return true;
            }
         }
         if (FShopTextures.inCell(mx, my, left, top, FShopTextures.HOME_CELL)) {
            Sfx.click();
            PacketHandler.sendToServer(new RequestBrowsePacket());
            return true;
         }
         if (hasPrev() && FShopTextures.inCell(mx, my, left, top, FShopTextures.PAGE_PREV_CELL)) {
            page--;
            Sfx.page();
            return true;
         }
         if (hasNext() && FShopTextures.inCell(mx, my, left, top, FShopTextures.PAGE_NEXT_CELL)) {
            page++;
            Sfx.page();
            return true;
         }
      }
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public boolean mouseScrolled(double mx, double my, double delta) {
      if (delta < 0 && hasNext()) {
         page++;
         Sfx.page();
         return true;
      }
      if (delta > 0 && hasPrev()) {
         page--;
         Sfx.page();
         return true;
      }
      return super.mouseScrolled(mx, my, delta);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
