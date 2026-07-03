package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
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

/** Owner GUI: offers over the storefront window, inventory over the gray grid. */
public final class ShopManageScreen extends Screen {
   private static final int FOOTER = 26;
   private final PlayerShop shop;
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

   private int offerX(int i) {
      return left + FShopTextures.winX(i % FShopTextures.WIN_COLS);
   }

   private int offerY(int i) {
      return top + FShopTextures.winY(i / FShopTextures.WIN_COLS);
   }

   private int invSlot(int row, int col) {
      return row < 3 ? 9 + row * 9 + col : col;
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.SELL_MENU, left, top);
      g.drawCenteredString(this.font, shop.getName(), left + 128, top + 40, 0xFFFFF0C0);

      // offers over the wooden window
      List<ShopOffer> offers = shop.getOffers();
      int shown = Math.min(offers.size(), FShopTextures.winCells());
      int hoveredOffer = -1;
      for (int i = 0; i < FShopTextures.winCells(); i++) {
         int gx = offerX(i);
         int gy = offerY(i);
         boolean hov = FShopTheme.inside(mouseX, mouseY, gx, gy, 18, 18);
         if (i < shown) {
            if (hov) {
               g.fill(gx, gy, gx + 18, gy + 18, 0x66FFD24A);
               hoveredOffer = i;
            }
            ShopOffer offer = offers.get(i);
            g.renderFakeItem(offer.displayStack(1), gx + 1, gy + 1);
            g.renderItemDecorations(this.font, offer.displayStack(Math.min(offer.getStock(), 64)), gx + 1, gy + 1);
         }
      }

      // player inventory over the gray grid
      var inv = this.minecraft.player.getInventory();
      int hoveredSlot = -1;
      for (int row = 0; row < FShopTextures.INV_ROWS; row++) {
         for (int col = 0; col < FShopTextures.INV_COLS; col++) {
            int gx = left + FShopTextures.invX(col);
            int gy = top + FShopTextures.invY(row);
            boolean hov = FShopTheme.inside(mouseX, mouseY, gx, gy, 18, 18);
            ItemStack st = inv.getItem(invSlot(row, col));
            if (hov && !st.isEmpty()) {
               g.fill(gx, gy, gx + 18, gy + 18, 0x6682CD47);
               hoveredSlot = invSlot(row, col);
            }
            if (!st.isEmpty()) {
               g.renderFakeItem(st, gx + 1, gy + 1);
               g.renderItemDecorations(this.font, st, gx + 1, gy + 1);
            }
         }
      }

      renderFooter(g, mouseX, mouseY);
      super.render(g, mouseX, mouseY, partial);
      if (hoveredOffer >= 0) {
         offerTooltip(g, offers.get(hoveredOffer), mouseX, mouseY);
      } else if (hoveredSlot >= 0) {
         List<Component> t = new ArrayList<>();
         t.add(inv.getItem(hoveredSlot).getHoverName());
         t.add(Component.translatable("fshop.gui.click_to_stock").withStyle(ChatFormatting.GREEN));
         g.renderComponentTooltip(this.font, t, mouseX, mouseY);
      }
   }

   private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
      int fy = top + FShopTextures.GH;
      g.fill(left, fy, left + FShopTextures.GW, fy + FOOTER, FShopTheme.HEADER);
      g.fill(left, fy, left + FShopTextures.GW, fy + 1, FShopTheme.BORDER);
      boolean collectHov = FShopTheme.inside(mouseX, mouseY, left + 8, fy + 5, 150, 16);
      FShopTheme.button(g, left + 8, fy + 5, 150, 16,
            shop.getPendingEarnings() > 0 ? FShopTheme.BUY : FShopTheme.BORDER, collectHov);
      g.drawString(this.font, Component.translatable("fshop.gui.manage.earnings",
            CoinEconomy.format(shop.getPendingEarnings())), left + 12, fy + 9, FShopTheme.GOLD, false);
      boolean closeHov = FShopTheme.inside(mouseX, mouseY, left + FShopTextures.GW - 62, fy + 5, 54, 16);
      FShopTheme.button(g, left + FShopTextures.GW - 62, fy + 5, 54, 16, FShopTheme.DANGER, closeHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.close"),
            left + FShopTextures.GW - 35, fy + 9, FShopTheme.TEXT);
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
      int shown = Math.min(offers.size(), FShopTextures.winCells());
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
      if (button == 0) {
         for (int row = 0; row < FShopTextures.INV_ROWS; row++) {
            for (int col = 0; col < FShopTextures.INV_COLS; col++) {
               int gx = left + FShopTextures.invX(col);
               int gy = top + FShopTextures.invY(row);
               if (FShopTheme.inside(mx, my, gx, gy, 18, 18)) {
                  int slot = invSlot(row, col);
                  if (!this.minecraft.player.getInventory().getItem(slot).isEmpty()) {
                     this.minecraft.setScreen(new PriceInputScreen(shop, PriceInputScreen.Mode.ADD, slot, 1));
                     return true;
                  }
               }
            }
         }
         int fy = top + FShopTextures.GH;
         if (FShopTheme.inside(mx, my, left + 8, fy + 5, 150, 16) && shop.getPendingEarnings() > 0) {
            PacketHandler.sendToServer(new CollectPacket(shop.getId()));
            return true;
         }
         if (FShopTheme.inside(mx, my, left + FShopTextures.GW - 62, fy + 5, 54, 16)) {
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
