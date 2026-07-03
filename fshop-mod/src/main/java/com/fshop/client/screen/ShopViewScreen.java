package com.fshop.client.screen;

import com.fshop.client.FShopTheme;
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

/** Buy GUI for a single shop: a grid of offers with prices and stock. */
public final class ShopViewScreen extends Screen {
   private static final int COLS = 7;
   private static final int ROWS = 5;
   private static final int CELL = 26;
   private static final int PANEL_W = COLS * CELL + 24;
   private static final int PANEL_H = ROWS * CELL + 66;

   private final PlayerShop shop;
   private final long balance;
   private int page;
   private int left;
   private int top;

   public ShopViewScreen(PlayerShop shop, long balance) {
      super(Component.literal(shop.getName()));
      this.shop = shop;
      this.balance = balance;
   }

   @Override
   protected void init() {
      this.left = (this.width - PANEL_W) / 2;
      this.top = (this.height - PANEL_H) / 2;
   }

   private int perPage() {
      return COLS * ROWS;
   }

   private int pageCount() {
      return Math.max(1, (shop.getOffers().size() + perPage() - 1) / perPage());
   }



   private int cellX(int i) {
      return left + 12 + (i % COLS) * CELL;
   }

   private int cellY(int i) {
      return top + 30 + (i / COLS) * CELL;
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTheme.panel(g, left, top, PANEL_W, PANEL_H, FShopTheme.PANEL, FShopTheme.BORDER);
      g.fill(left, top, left + PANEL_W, top + 22, FShopTheme.HEADER);
      g.drawString(this.font, shop.getName(), left + 10, top + 7, FShopTheme.GOLD, false);
      String pageStr = (page + 1) + "/" + pageCount();
      g.drawString(this.font, pageStr, left + PANEL_W - 10 - this.font.width(pageStr), top + 7,
            FShopTheme.TEXT_DIM, false);

      List<ShopOffer> offers = shop.getOffers();
      int start = page * perPage();
      int hovered = -1;
      for (int i = 0; i < perPage() && start + i < offers.size(); i++) {
         int gx = cellX(i);
         int gy = cellY(i);
         boolean hov = FShopTheme.inside(mouseX, mouseY, gx, gy, 22, 22);
         g.fill(gx, gy, gx + 22, gy + 22, hov ? FShopTheme.SLOT_HOVER | 0xFF000000 : FShopTheme.SLOT);
         FShopTheme.panel(g, gx, gy, 22, 22, 0x00000000, FShopTheme.BORDER);
         ShopOffer offer = offers.get(start + i);
         g.renderFakeItem(offer.displayStack(1), gx + 3, gy + 3);
         if (hov) {
            hovered = start + i;
         }
      }

      // footer: balance + back button
      int fy = top + PANEL_H - 24;
      g.drawString(this.font, Component.translatable("fshop.gui.balance", CoinEconomy.format(balance)),
            left + 12, fy + 4, FShopTheme.GOLD, false);
      boolean backHov = FShopTheme.inside(mouseX, mouseY, left + PANEL_W - 74, fy, 62, 16);
      FShopTheme.button(g, left + PANEL_W - 74, fy, 62, 16, FShopTheme.SELL, backHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.back"),
            left + PANEL_W - 43, fy + 4, FShopTheme.TEXT);

      super.render(g, mouseX, mouseY, partial);
      if (hovered >= 0) {
         renderOfferTooltip(g, offers.get(hovered), mouseX, mouseY);
      }
   }


   private void renderOfferTooltip(GuiGraphics g, ShopOffer offer, int mouseX, int mouseY) {
      List<Component> lines = new ArrayList<>();
      lines.add(offer.displayStack(1).getHoverName());
      lines.add(Component.translatable("fshop.gui.buy_price", CoinEconomy.format(offer.getUnitPrice()))
            .withStyle(ChatFormatting.GREEN));
      lines.add(Component.translatable("fshop.gui.stock", offer.getStock())
            .withStyle(offer.getStock() > 0 ? ChatFormatting.GRAY : ChatFormatting.RED));
      lines.add(Component.empty());
      lines.add(Component.translatable("fshop.gui.click_to_buy").withStyle(ChatFormatting.GREEN));
      g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      List<ShopOffer> offers = shop.getOffers();
      int start = page * perPage();
      if (button == 0) {
         for (int i = 0; i < perPage() && start + i < offers.size(); i++) {
            if (FShopTheme.inside(mx, my, cellX(i), cellY(i), 22, 22)) {
               int idx = start + i;
               ShopOffer offer = offers.get(idx);
               if (offer.getStock() > 0) {
                  this.minecraft.setScreen(new AmountScreen(shop, idx, balance));
               }
               return true;
            }
         }
         int fy = top + PANEL_H - 24;
         if (FShopTheme.inside(mx, my, left + PANEL_W - 74, fy, 62, 16)) {
            PacketHandler.sendToServer(new RequestBrowsePacket());
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
