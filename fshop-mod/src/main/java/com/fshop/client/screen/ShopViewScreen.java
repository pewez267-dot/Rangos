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

/** Buy GUI for a single shop: offers laid out over the storefront window. */
public final class ShopViewScreen extends Screen {
   private static final int FOOTER = 26;
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
      return FShopTextures.winCells();
   }

   private int pageCount() {
      return Math.max(1, (shop.getOffers().size() + perPage() - 1) / perPage());
   }

   private int cellX(int i) {
      return left + FShopTextures.winX(i % FShopTextures.WIN_COLS);
   }

   private int cellY(int i) {
      return top + FShopTextures.winY(i / FShopTextures.WIN_COLS);
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.ITEM_DISPLAY, left, top);
      g.drawCenteredString(this.font, shop.getName(), left + 128, top + 40, 0xFFFFF0C0);

      List<ShopOffer> offers = shop.getOffers();
      int start = page * perPage();
      int hovered = -1;
      for (int i = 0; i < perPage() && start + i < offers.size(); i++) {
         int gx = cellX(i);
         int gy = cellY(i);
         boolean hov = FShopTheme.inside(mouseX, mouseY, gx, gy, 18, 18);
         if (hov) {
            g.fill(gx, gy, gx + 18, gy + 18, 0x6682CD47);
            hovered = start + i;
         }
         ShopOffer offer = offers.get(start + i);
         g.renderFakeItem(offer.displayStack(1), gx + 1, gy + 1);
         if (offer.getStock() <= 0) {
            g.fill(gx + 1, gy + 1, gx + 17, gy + 17, 0x99DF2E38);
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
      g.fill(left, fy, left + FShopTextures.GW, fy + FOOTER, FShopTheme.HEADER);
      g.fill(left, fy, left + FShopTextures.GW, fy + 1, FShopTheme.BORDER);
      boolean backHov = FShopTheme.inside(mouseX, mouseY, left + 8, fy + 5, 54, 16);
      FShopTheme.button(g, left + 8, fy + 5, 54, 16, FShopTheme.SELL, backHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.back"), left + 35, fy + 9, FShopTheme.TEXT);

      boolean hp = page > 0 && FShopTheme.inside(mouseX, mouseY, left + FShopTextures.GW - 88, fy + 5, 22, 16);
      boolean hn = page < pageCount() - 1 && FShopTheme.inside(mouseX, mouseY, left + FShopTextures.GW - 30, fy + 5, 22, 16);
      FShopTheme.button(g, left + FShopTextures.GW - 88, fy + 5, 22, 16, page > 0 ? FShopTheme.SELL : FShopTheme.BORDER, hp);
      FShopTheme.button(g, left + FShopTextures.GW - 30, fy + 5, 22, 16, page < pageCount() - 1 ? FShopTheme.SELL : FShopTheme.BORDER, hn);
      g.drawCenteredString(this.font, "<", left + FShopTextures.GW - 77, fy + 9, FShopTheme.TEXT);
      g.drawCenteredString(this.font, ">", left + FShopTextures.GW - 19, fy + 9, FShopTheme.TEXT);
      g.drawCenteredString(this.font, (page + 1) + "/" + pageCount(), left + FShopTextures.GW - 53, fy + 9, FShopTheme.TEXT_DIM);

      g.drawCenteredString(this.font, Component.translatable("fshop.gui.balance", CoinEconomy.format(balance)),
            left + 128, fy + 9, FShopTheme.GOLD);
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
            if (FShopTheme.inside(mx, my, cellX(i), cellY(i), 18, 18)) {
               int idx = start + i;
               if (offers.get(idx).getStock() > 0) {
                  this.minecraft.setScreen(new AmountScreen(shop, idx, balance));
               }
               return true;
            }
         }
         int fy = top + FShopTextures.GH;
         if (FShopTheme.inside(mx, my, left + 8, fy + 5, 54, 16)) {
            PacketHandler.sendToServer(new RequestBrowsePacket());
            return true;
         }
         if (page > 0 && FShopTheme.inside(mx, my, left + FShopTextures.GW - 88, fy + 5, 22, 16)) {
            page--;
            return true;
         }
         if (page < pageCount() - 1 && FShopTheme.inside(mx, my, left + FShopTextures.GW - 30, fy + 5, 22, 16)) {
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
