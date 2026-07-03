package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.client.ShopWidgets;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.OpenShopRequestPacket;
import com.fshop.network.PacketHandler;
import com.fshop.shop.ShopSummary;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lists every registered player shop in the storefront window. */
public final class ShopBrowseScreen extends Screen {
   private final List<ShopSummary> shops;
   private int page;
   private int left;
   private int top;

   public ShopBrowseScreen(List<ShopSummary> shops) {
      super(Component.translatable("fshop.gui.browse.title"));
      this.shops = shops;
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
      return Math.max(1, (shops.size() + perPage() - 1) / perPage());
   }

   private int prevX() {
      return left + 56;
   }

   private int nextX() {
      return left + 176;
   }

   private int navY() {
      return top + 224;
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.ITEM_DISPLAY, left, top);

      List<ShopSummary> list = this.shops;
      int start = page * perPage();
      int hovered = -1;
      for (int i = 0; i < perPage() && start + i < list.size(); i++) {
         int cx = left + FShopTextures.contentCellX(i);
         int cy = top + FShopTextures.contentCellY(i);
         boolean hov = FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL, FShopTextures.CELL);
         if (hov) {
            g.fill(cx, cy, cx + FShopTextures.CELL, cy + FShopTextures.CELL, 0x6682CD47);
            hovered = start + i;
         }
         g.renderFakeItem(list.get(start + i).icon(), left + FShopTextures.contentItemX(i), top + FShopTextures.contentItemY(i));
      }

      // navigation panel over the bottom gray area (inventory is hidden)
      ShopWidgets.dimBottom(g, left, top);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.browse.title"),
            left + 128, top + 176, FShopTheme.GOLD);
      if (list.isEmpty()) {
         g.drawCenteredString(this.font, Component.translatable("fshop.gui.browse.empty"),
               left + 128, top + 200, FShopTheme.TEXT_DIM);
      } else {
         boolean hp = page > 0 && FShopTheme.inside(mouseX, mouseY, prevX(), navY(), 24, 18);
         boolean hn = page < pageCount() - 1 && FShopTheme.inside(mouseX, mouseY, nextX(), navY(), 24, 18);
         FShopTheme.button(g, prevX(), navY(), 24, 18, page > 0 ? FShopTheme.SELL : FShopTheme.BORDER, hp);
         FShopTheme.button(g, nextX(), navY(), 24, 18, page < pageCount() - 1 ? FShopTheme.SELL : FShopTheme.BORDER, hn);
         g.drawCenteredString(this.font, "<", prevX() + 12, navY() + 5, FShopTheme.TEXT);
         g.drawCenteredString(this.font, ">", nextX() + 12, navY() + 5, FShopTheme.TEXT);
         g.drawCenteredString(this.font, (page + 1) + " / " + pageCount(), left + 128, navY() + 5, FShopTheme.TEXT);
      }

      super.render(g, mouseX, mouseY, partial);
      if (hovered >= 0) {
         shopTooltip(g, list.get(hovered), mouseX, mouseY);
      }
   }

   private void shopTooltip(GuiGraphics g, ShopSummary s, int mouseX, int mouseY) {
      List<Component> t = new ArrayList<>();
      t.add(Component.literal(s.name()).withStyle(ChatFormatting.GOLD));
      t.add(Component.translatable("fshop.gui.browse.owner", s.ownerName()).withStyle(ChatFormatting.GRAY));
      t.add(Component.translatable("fshop.gui.browse.count", s.offerCount()).withStyle(ChatFormatting.DARK_GRAY));
      t.add(Component.translatable("fshop.gui.browse.from", s.minPrice(),
            Component.translatable(CoinEconomy.coinKey(s.minCoin()))).withStyle(ChatFormatting.GREEN));
      t.add(Component.empty());
      t.add(Component.translatable("fshop.gui.click_to_open").withStyle(ChatFormatting.YELLOW));
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         int start = page * perPage();
         for (int i = 0; i < perPage() && start + i < shops.size(); i++) {
            int cx = left + FShopTextures.contentCellX(i);
            int cy = top + FShopTextures.contentCellY(i);
            if (FShopTheme.inside(mx, my, cx, cy, FShopTextures.CELL, FShopTextures.CELL)) {
               PacketHandler.sendToServer(new OpenShopRequestPacket(shops.get(start + i).id()));
               return true;
            }
         }
         if (page > 0 && FShopTheme.inside(mx, my, prevX(), navY(), 24, 18)) {
            page--;
            return true;
         }
         if (page < pageCount() - 1 && FShopTheme.inside(mx, my, nextX(), navY(), 24, 18)) {
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
