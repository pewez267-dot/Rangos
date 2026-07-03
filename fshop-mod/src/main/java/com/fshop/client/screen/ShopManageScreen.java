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
   private static final int FOOTER = 42;
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
      this.top = (this.height - (FShopTextures.GH + FOOTER)) / 2;
   }

   private int perPage() {
      return FShopTextures.contentCells();
   }

   private int pageCount() {
      return Math.max(1, (shop.getOffers().size() + perPage() - 1) / perPage());
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

      int hoveredSlot = ShopWidgets.renderInventory(g, this.font, this.minecraft.player.getInventory(),
            left, top, mouseX, mouseY, true);

      renderFooter(g, mouseX, mouseY);
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

   private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
      int fy = top + FShopTextures.GH;
      int w = FShopTextures.GW;
      g.fill(left, fy, left + w, fy + FOOTER, FShopTheme.HEADER);
      g.fill(left, fy, left + w, fy + 1, FShopTheme.BORDER);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.manage.hint"),
            left + w / 2, fy + 6, FShopTheme.GOLD);

      int by = fy + 22;
      boolean canCollect = shop.getPendingEarnings() > 0;
      boolean colHov = canCollect && FShopTheme.inside(mouseX, mouseY, left + 8, by, 150, 16);
      FShopTheme.button(g, left + 8, by, 150, 16, canCollect ? FShopTheme.BUY : FShopTheme.BORDER, colHov);
      g.drawString(this.font, Component.translatable("fshop.gui.manage.earnings",
            CoinEconomy.formatShort(shop.getPendingEarnings())), left + 12, by + 4, FShopTheme.TEXT, false);

      boolean closeHov = FShopTheme.inside(mouseX, mouseY, left + w - 62, by, 54, 16);
      FShopTheme.button(g, left + w - 62, by, 54, 16, FShopTheme.DANGER, closeHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.close"), left + w - 35, by + 4, FShopTheme.TEXT);

      if (pageCount() > 1) {
         g.drawCenteredString(this.font, (page + 1) + "/" + pageCount(), left + w / 2, by + 4, FShopTheme.TEXT_DIM);
      }
   }

   private void offerTooltip(GuiGraphics g, ShopOffer offer, int mouseX, int mouseY) {
      List<Component> t = new ArrayList<>();
      t.add(offer.displayStack(1).getHoverName());
      t.add(Component.translatable("fshop.gui.buy_price", CoinEconomy.format(offer.getUnitPrice()))
            .withStyle(ChatFormatting.GREEN));
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
                     offers.get(idx).getUnitPrice()));
            }
            return true;
         }
      }
      if (button == 0) {
         int slot = ShopWidgets.slotAt(this.minecraft.player.getInventory(), left, top, mx, my);
         if (slot >= 0) {
            this.minecraft.setScreen(new PriceInputScreen(shop, PriceInputScreen.Mode.ADD, slot, 1));
            return true;
         }
         int by = top + FShopTextures.GH + 22;
         int w = FShopTextures.GW;
         if (shop.getPendingEarnings() > 0 && FShopTheme.inside(mx, my, left + 8, by, 150, 16)) {
            PacketHandler.sendToServer(new CollectPacket(shop.getId()));
            return true;
         }
         if (FShopTheme.inside(mx, my, left + w - 62, by, 54, 16)) {
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
