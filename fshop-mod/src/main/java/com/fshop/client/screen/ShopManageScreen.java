package com.fshop.client.screen;

import com.fshop.client.FShopTheme;
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
import net.minecraft.world.item.ItemStack;

/** Owner GUI: manage offers (top) and stock items from inventory (bottom). */
public final class ShopManageScreen extends Screen {
   private static final int COLS = 9;
   private static final int OFFER_ROWS = 3;
   private static final int CELL = 20;
   private static final int PANEL_W = COLS * CELL + 16;
   private static final int PANEL_H = 262;

   private final PlayerShop shop;
   private int left;
   private int top;

   public ShopManageScreen(PlayerShop shop) {
      super(Component.literal(shop.getName()));
      this.shop = shop;
   }

   @Override
   protected void init() {
      this.left = (this.width - PANEL_W) / 2;
      this.top = (this.height - PANEL_H) / 2;
   }

   private int offerX(int i) {
      return left + 8 + (i % COLS) * CELL;
   }

   private int offerY(int i) {
      return top + 44 + (i / COLS) * CELL;
   }

   private int invTop() {
      return top + 44 + OFFER_ROWS * CELL + 22;
   }

   private int invX(int col) {
      return left + 8 + col * CELL;
   }

   private int invSlot(int row, int col) {
      return row < 3 ? 9 + row * 9 + col : col;
   }



   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTheme.panel(g, left, top, PANEL_W, PANEL_H, FShopTheme.PANEL, FShopTheme.BORDER);
      g.fill(left, top, left + PANEL_W, top + 22, FShopTheme.HEADER);
      g.drawString(this.font, Component.translatable("fshop.gui.manage.title", shop.getName()),
            left + 10, top + 7, FShopTheme.GOLD, false);

      g.drawString(this.font, Component.translatable("fshop.gui.manage.offers"), left + 8, top + 30,
            FShopTheme.TEXT_DIM, false);

      List<ShopOffer> offers = shop.getOffers();
      int hoveredOffer = -1;
      int shown = Math.min(offers.size(), COLS * OFFER_ROWS);
      for (int i = 0; i < COLS * OFFER_ROWS; i++) {
         int gx = offerX(i);
         int gy = offerY(i);
         boolean hov = FShopTheme.inside(mouseX, mouseY, gx, gy, 18, 18);
         g.fill(gx, gy, gx + 18, gy + 18, hov ? (FShopTheme.SLOT_HOVER | 0xFF000000) : FShopTheme.SLOT);
         if (i < shown) {
            ShopOffer offer = offers.get(i);
            g.renderFakeItem(offer.displayStack(1), gx + 1, gy + 1);
            g.renderItemDecorations(this.font, offer.displayStack(Math.min(offer.getStock(), 64)), gx + 1, gy + 1);
            if (hov) {
               hoveredOffer = i;
            }
         }
      }

      // inventory label + grid
      int invTop = invTop();
      g.drawString(this.font, Component.translatable("fshop.gui.manage.inventory"), left + 8, invTop - 12,
            FShopTheme.TEXT_DIM, false);
      var inv = this.minecraft.player.getInventory();
      int hoveredSlot = -1;
      for (int row = 0; row < 4; row++) {
         for (int col = 0; col < 9; col++) {
            int gx = invX(col);
            int gy = invTop + row * CELL + (row == 3 ? 4 : 0);
            boolean hov = FShopTheme.inside(mouseX, mouseY, gx, gy, 18, 18);
            g.fill(gx, gy, gx + 18, gy + 18, hov ? (FShopTheme.SLOT_HOVER | 0xFF000000) : FShopTheme.SLOT);
            ItemStack st = inv.getItem(invSlot(row, col));
            if (!st.isEmpty()) {
               g.renderFakeItem(st, gx + 1, gy + 1);
               g.renderItemDecorations(this.font, st, gx + 1, gy + 1);
               if (hov) {
                  hoveredSlot = invSlot(row, col);
               }
            }
         }
      }

      renderFooter(g, mouseX, mouseY);
      super.render(g, mouseX, mouseY, partial);
      if (hoveredOffer >= 0) {
         offerTooltip(g, offers.get(hoveredOffer), mouseX, mouseY);
      } else if (hoveredSlot >= 0) {
         ItemStack st = inv.getItem(hoveredSlot);
         List<Component> t = new ArrayList<>();
         t.add(st.getHoverName());
         t.add(Component.translatable("fshop.gui.click_to_stock").withStyle(ChatFormatting.GREEN));
         g.renderComponentTooltip(this.font, t, mouseX, mouseY);
      }
   }


   private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
      int fy = top + PANEL_H - 24;
      String earn = Component.translatable("fshop.gui.manage.earnings",
            CoinEconomy.format(shop.getPendingEarnings())).getString();
      boolean collectHov = FShopTheme.inside(mouseX, mouseY, left + 8, fy, 118, 16);
      FShopTheme.button(g, left + 8, fy, 118, 16,
            shop.getPendingEarnings() > 0 ? FShopTheme.BUY : FShopTheme.BORDER, collectHov);
      g.drawString(this.font, earn, left + 12, fy + 4, FShopTheme.GOLD, false);
      boolean closeHov = FShopTheme.inside(mouseX, mouseY, left + PANEL_W - 62, fy, 54, 16);
      FShopTheme.button(g, left + PANEL_W - 62, fy, 54, 16, FShopTheme.DANGER, closeHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.close"),
            left + PANEL_W - 35, fy + 4, FShopTheme.TEXT);
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
      int shown = Math.min(offers.size(), COLS * OFFER_ROWS);
      for (int i = 0; i < shown; i++) {
         if (FShopTheme.inside(mx, my, offerX(i), offerY(i), 18, 18)) {
            if (button == 1) {
               PacketHandler.sendToServer(new RemoveOfferPacket(shop.getId(), i));
            } else if (button == 0) {
               this.minecraft.setScreen(new PriceInputScreen(shop, PriceInputScreen.Mode.EDIT, i,
                     offers.get(i).getUnitPrice()));
            }
            return true;
         }
      }
      int invTop = invTop();
      for (int row = 0; row < 4; row++) {
         for (int col = 0; col < 9; col++) {
            int gx = invX(col);
            int gy = invTop + row * CELL + (row == 3 ? 4 : 0);
            if (button == 0 && FShopTheme.inside(mx, my, gx, gy, 18, 18)) {
               int slot = invSlot(row, col);
               if (!this.minecraft.player.getInventory().getItem(slot).isEmpty()) {
                  this.minecraft.setScreen(new PriceInputScreen(shop, PriceInputScreen.Mode.ADD, slot, 1));
                  return true;
               }
            }
         }
      }
      int fy = top + PANEL_H - 24;
      if (button == 0 && FShopTheme.inside(mx, my, left + 8, fy, 118, 16) && shop.getPendingEarnings() > 0) {
         PacketHandler.sendToServer(new CollectPacket(shop.getId()));
         return true;
      }
      if (button == 0 && FShopTheme.inside(mx, my, left + PANEL_W - 62, fy, 54, 16)) {
         this.onClose();
         return true;
      }
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
