package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.client.ShopWidgets;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.CollectPacket;
import com.fshop.network.PacketHandler;
import com.fshop.network.RemoveOfferPacket;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ShopOffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Owner GUI: offers in the window; click your inventory (gray grid) to stock. */
public final class ShopManageScreen extends Screen {
   private final PlayerShop shop;
   private int page;
   private int left;
   private int top;

   public ShopManageScreen(PlayerShop shop) {
      super(Component.literal(shop.getName()));
      this.shop = shop;
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

   private int collectX() {
      return left + 46;
   }

   private int closeX() {
      return left + 150;
   }

   private int barY() {
      return top + 149;
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.SELL_MENU, left, top);

      List<ShopOffer> offers = shop.getOffers();
      int start = page * perPage();
      int hovered = -1;
      for (int i = 0; i < perPage() && start + i < offers.size(); i++) {
         int cx = left + FShopTextures.contentCellX(i);
         int cy = top + FShopTextures.contentCellY(i);
         boolean hov = FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL, FShopTextures.CELL);
         if (hov) {
            g.fill(cx, cy, cx + FShopTextures.CELL, cy + FShopTextures.CELL, 0x66FFD24A);
            hovered = start + i;
         }
         ShopOffer offer = offers.get(start + i);
         g.renderFakeItem(offer.displayStack(1), cx + 1, cy + 1);
         g.renderItemDecorations(this.font, offer.displayStack(Math.min(offer.getStock(), 64)), cx + 1, cy + 1);
      }

      // control bar on the wooden ledge (collect / close)
      long earn = shop.totalPendingEarnings();
      boolean colHov = earn > 0 && FShopTheme.inside(mouseX, mouseY, collectX(), barY(), 96, 14);
      FShopTheme.button(g, collectX(), barY(), 96, 14, earn > 0 ? FShopTheme.BUY : FShopTheme.BORDER, colHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.manage.collect"),
            collectX() + 48, barY() + 3, FShopTheme.TEXT);
      boolean closeHov = FShopTheme.inside(mouseX, mouseY, closeX(), barY(), 60, 14);
      FShopTheme.button(g, closeX(), barY(), 60, 14, FShopTheme.DANGER, closeHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.close"), closeX() + 30, barY() + 3, FShopTheme.TEXT);

      // your inventory on the gray grid (click an item to put it on sale)
      int hoveredSlot = ShopWidgets.renderInventory(g, this.font, this.minecraft.player.getInventory(),
            left, top, mouseX, mouseY, true);

      super.render(g, mouseX, mouseY, partial);
      if (hovered >= 0) {
         offerTooltip(g, offers.get(hovered), mouseX, mouseY);
      } else if (hoveredSlot >= 0) {
         List<Component> t = new ArrayList<>();
         t.add(this.minecraft.player.getInventory().getItem(hoveredSlot).getHoverName());
         t.add(Component.translatable("fshop.gui.click_to_stock").withStyle(ChatFormatting.GREEN));
         g.renderComponentTooltip(this.font, t, mouseX, mouseY);
      }
   }

   private void offerTooltip(GuiGraphics g, ShopOffer offer, int mouseX, int mouseY) {
      List<Component> t = new ArrayList<>();
      t.add(offer.displayStack(1).getHoverName());
      t.add(Component.translatable("fshop.gui.buy_price", offer.getUnitPrice(),
            Component.translatable(CoinEconomy.coinKey(offer.getCoin()))).withStyle(ChatFormatting.GREEN));
      t.add(Component.translatable("fshop.gui.stock", offer.getStock()).withStyle(ChatFormatting.GRAY));
      t.add(Component.empty());
      t.add(Component.translatable("fshop.gui.left_edit_price").withStyle(ChatFormatting.AQUA));
      t.add(Component.translatable("fshop.gui.right_remove").withStyle(ChatFormatting.RED));
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      List<ShopOffer> offers = shop.getOffers();
      int start = page * perPage();
      for (int i = 0; i < perPage() && start + i < offers.size(); i++) {
         int cx = left + FShopTextures.contentCellX(i);
         int cy = top + FShopTextures.contentCellY(i);
         if (FShopTheme.inside(mx, my, cx, cy, FShopTextures.CELL, FShopTextures.CELL)) {
            int idx = start + i;
            if (button == 1) {
               PacketHandler.sendToServer(new RemoveOfferPacket(shop.getId(), idx));
            } else if (button == 0) {
               this.minecraft.setScreen(new PriceInputScreen(shop, PriceInputScreen.Mode.EDIT, idx,
                     offers.get(idx).getUnitPrice(), offers.get(idx).getCoin()));
            }
            return true;
         }
      }
      if (button == 0) {
         int slot = ShopWidgets.slotAt(this.minecraft.player.getInventory(), left, top, mx, my);
         if (slot >= 0) {
            this.minecraft.setScreen(new PriceInputScreen(shop, PriceInputScreen.Mode.ADD, slot, 1, CoinEconomy.BRONZE));
            return true;
         }
         if (shop.totalPendingEarnings() > 0 && FShopTheme.inside(mx, my, collectX(), barY(), 96, 14)) {
            PacketHandler.sendToServer(new CollectPacket(shop.getId()));
            return true;
         }
         if (FShopTheme.inside(mx, my, closeX(), barY(), 60, 14)) {
            this.onClose();
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
