package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
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

/** Buy GUI for a single shop: offers laid out over the gray grid. */
public final class ShopViewScreen extends Screen {
   private static final int FOOTER = 42;
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
      this.left = (this.width - FShopTextures.GW) / 2;
      this.top = (this.height - (FShopTextures.GH + FOOTER)) / 2;
   }

   private int perPage() {
      return FShopTextures.cells();
   }

   private int pageCount() {
      return Math.max(1, (shop.getOffers().size() + perPage() - 1) / perPage());
   }

   private int gx(int i) {
      return left + FShopTextures.cellX(i % FShopTextures.GRID_COLS);
   }

   private int gy(int i) {
      return top + FShopTextures.cellY(i / FShopTextures.GRID_COLS);
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.ITEM_DISPLAY, left, top);

      List<ShopOffer> offers = shop.getOffers();
      int start = page * perPage();
      int hovered = -1;
      for (int i = 0; i < perPage() && start + i < offers.size(); i++) {
         int cx = gx(i);
         int cy = gy(i);
         boolean hov = FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL_W, FShopTextures.CELL_H);
         if (hov) {
            g.fill(cx, cy, cx + FShopTextures.CELL_W, cy + FShopTextures.CELL_H, 0x6682CD47);
            hovered = start + i;
         }
         ShopOffer offer = offers.get(start + i);
         int ix = left + FShopTextures.itemX(i % FShopTextures.GRID_COLS);
         int iy = top + FShopTextures.itemY(i / FShopTextures.GRID_COLS);
         g.renderFakeItem(offer.displayStack(1), ix, iy);
         if (offer.getStock() <= 0) {
            g.fill(ix, iy, ix + 16, iy + 16, 0x99DF2E38);
         }
      }

      renderFooter(g, mouseX, mouseY);
      super.render(g, mouseX, mouseY, partial);
      if (hovered >= 0) {
         offerTooltip(g, offers.get(hovered), mouseX, mouseY);
      }
   }

   private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
      int fy = top + FShopTextures.GH;
      int w = FShopTextures.GW;
      g.fill(left, fy, left + w, fy + FOOTER, FShopTheme.HEADER);
      g.fill(left, fy, left + w, fy + 1, FShopTheme.BORDER);
      g.drawCenteredString(this.font, shop.getName(), left + w / 2, fy + 6, FShopTheme.GOLD);

      int by = fy + 22;
      boolean backHov = FShopTheme.inside(mouseX, mouseY, left + 8, by, 54, 16);
      FShopTheme.button(g, left + 8, by, 54, 16, FShopTheme.SELL, backHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.back"), left + 35, by + 4, FShopTheme.TEXT);

      g.drawCenteredString(this.font,
            Component.translatable("fshop.gui.balance", CoinEconomy.formatShort(balance)),
            left + w / 2, by + 4, FShopTheme.GOLD);

      boolean hp = page > 0 && FShopTheme.inside(mouseX, mouseY, left + w - 78, by, 20, 16);
      boolean hn = page < pageCount() - 1 && FShopTheme.inside(mouseX, mouseY, left + w - 28, by, 20, 16);
      FShopTheme.button(g, left + w - 78, by, 20, 16, page > 0 ? FShopTheme.SELL : FShopTheme.BORDER, hp);
      FShopTheme.button(g, left + w - 28, by, 20, 16, page < pageCount() - 1 ? FShopTheme.SELL : FShopTheme.BORDER, hn);
      g.drawCenteredString(this.font, "<", left + w - 68, by + 4, FShopTheme.TEXT);
      g.drawCenteredString(this.font, ">", left + w - 18, by + 4, FShopTheme.TEXT);
      g.drawCenteredString(this.font, (page + 1) + "/" + pageCount(), left + w - 43, by + 4, FShopTheme.TEXT_DIM);
   }

   private void offerTooltip(GuiGraphics g, ShopOffer offer, int mouseX, int mouseY) {
      List<Component> t = new ArrayList<>();
      t.add(offer.displayStack(1).getHoverName());
      t.add(Component.translatable("fshop.gui.buy_price", CoinEconomy.format(offer.getUnitPrice()))
            .withStyle(ChatFormatting.GREEN));
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
            if (FShopTheme.inside(mx, my, gx(i), gy(i), FShopTextures.CELL_W, FShopTextures.CELL_H)) {
               int idx = start + i;
               if (offers.get(idx).getStock() > 0) {
                  this.minecraft.setScreen(new AmountScreen(shop, idx, balance));
               }
               return true;
            }
         }
         int by = top + FShopTextures.GH + 22;
         int w = FShopTextures.GW;
         if (FShopTheme.inside(mx, my, left + 8, by, 54, 16)) {
            PacketHandler.sendToServer(new RequestBrowsePacket());
            return true;
         }
         if (page > 0 && FShopTheme.inside(mx, my, left + w - 78, by, 20, 16)) {
            page--;
            return true;
         }
         if (page < pageCount() - 1 && FShopTheme.inside(mx, my, left + w - 28, by, 20, 16)) {
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
