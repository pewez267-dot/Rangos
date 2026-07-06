package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.client.PlayerHeadRenderer;
import com.fshop.client.Sfx;
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
 * Lists every registered player shop: shop heads sit in the wooden 7x4 window,
 * the buyer's coin wallet shows on the gray grid, the house icon closes the
 * menu, and the two arrows in the bottom corners page through the shops (also
 * scrollable with the mouse wheel).
 */
public final class ShopBrowseScreen extends Screen {
   private final List<ShopSummary> shops;
   private int page;
   private int left;
   private int top;
   private int openTick;

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
         int cx0 = left + FShopTextures.contentCellX(i);
         int cy0 = top + FShopTextures.contentCellY(i);
         if (s.main() && !s.icon().isEmpty()) {
            g.renderFakeItem(s.icon(), cx0 + 1, cy0 + 1);
         } else {
            // 12px head, centred in the 18px cell with a clear margin (never spills)
            PlayerHeadRenderer.draw(g, s.ownerId(), s.ownerName(), cx0 + 3, cy0 + 3, 12);
         }
      }

      if (list.isEmpty()) {
         g.drawCenteredString(this.font, Component.translatable("fshop.gui.browse.empty"),
               left + 128, top + 100, FShopTheme.WOOD_TEXT_DIM);
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
      if (pageCount() > 1) {
         FShopTheme.drawPageBadge(g, this.font, left + 128, top + 240, (page + 1) + "/" + pageCount());
      }

      super.render(g, mouseX, mouseY, partial);
      if (hovered >= 0) {
         shopTooltip(g, list.get(hovered), mouseX, mouseY);
      } else if (coinHov >= 0) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.wallet",
               CoinEconomy.balance(this.minecraft.player, coinHov),
               Component.translatable(CoinEconomy.coinKey(coinHov))));
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
               Sfx.select();
               PacketHandler.sendToServer(new OpenShopRequestPacket(shops.get(start + i).id()));
               return true;
            }
         }
         if (FShopTextures.inCell(mx, my, left, top, FShopTextures.HOME_CELL)) {
            Sfx.click();
            this.onClose();
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
   public void tick() {
      super.tick();
      // a single soft note the first time the market opens (a 4-note melody
      // felt too noisy)
      if (this.openTick == 0) {
         Sfx.spark(1.0F);
      }
      this.openTick++;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
