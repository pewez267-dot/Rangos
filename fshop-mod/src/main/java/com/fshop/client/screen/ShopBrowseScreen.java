package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.client.PlayerHeadRenderer;
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

/**
 * Lists every registered player shop, faithful to the ShopGUI+ layout: shop
 * heads sit in the wooden 7x4 window, the player's own inventory shows on the
 * real gray grid below (like the plugin), the house icon (slot 4) closes the
 * menu and the real arrow icons (slots 27/35) page through shops.
 */
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

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.ITEM_DISPLAY, left, top);

      // player inventory on the real gray grid (purely visual, like the plugin)
      ShopWidgets.renderInventory(g, this.font, this.minecraft.player.getInventory(),
            left, top, mouseX, mouseY, false);

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
         ShopSummary s = list.get(start + i);
         PlayerHeadRenderer.draw(g, s.ownerId(), s.ownerName(),
               left + FShopTextures.contentItemX(i), top + FShopTextures.contentItemY(i), 16);
      }

      if (list.isEmpty()) {
         g.drawCenteredString(this.font, Component.translatable("fshop.gui.browse.empty"),
               left + 128, top + 100, FShopTheme.WOOD_TEXT_DIM);
      }

      // navigation using the real texture elements: house icon = close, arrows = paging
      boolean homeHov = FShopTextures.inCell(mouseX, mouseY, left, top, FShopTextures.HOME_CELL);
      FShopTextures.hoverCell(g, left, top, FShopTextures.HOME_CELL, homeHov);
      boolean multi = pageCount() > 1;
      boolean hp = false;
      boolean hn = false;
      if (multi) {
         hp = page > 0 && FShopTextures.inCell(mouseX, mouseY, left, top, FShopTextures.PREV_CELL);
         hn = page < pageCount() - 1 && FShopTextures.inCell(mouseX, mouseY, left, top, FShopTextures.NEXT_CELL);
         if (page > 0) {
            FShopTextures.blitIcon(g, FShopTextures.BACK_BUTTON, left, top, FShopTextures.PREV_CELL);
            FShopTextures.hoverCell(g, left, top, FShopTextures.PREV_CELL, hp);
         }
         if (page < pageCount() - 1) {
            FShopTextures.blitIcon(g, FShopTextures.NEXT_BUTTON, left, top, FShopTextures.NEXT_CELL);
            FShopTextures.hoverCell(g, left, top, FShopTextures.NEXT_CELL, hn);
         }
      }

      super.render(g, mouseX, mouseY, partial);
      if (hovered >= 0) {
         shopTooltip(g, list.get(hovered), mouseX, mouseY);
      } else if (homeHov) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.close"));
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
         if (FShopTextures.inCell(mx, my, left, top, FShopTextures.HOME_CELL)) {
            this.onClose();
            return true;
         }
         if (page > 0 && FShopTextures.inCell(mx, my, left, top, FShopTextures.PREV_CELL)) {
            page--;
            return true;
         }
         if (page < pageCount() - 1 && FShopTextures.inCell(mx, my, left, top, FShopTextures.NEXT_CELL)) {
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
