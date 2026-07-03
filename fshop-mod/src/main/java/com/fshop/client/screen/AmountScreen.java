package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.BuyPacket;
import com.fshop.network.OpenShopRequestPacket;
import com.fshop.network.PacketHandler;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ShopOffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Quantity confirmation over the ShopGUI+ "ARE YOU SURE?" storefront texture. */
public final class AmountScreen extends Screen {
   // Interactive zones in texture space (measured from shop_gui_confirmation.png).
   private static final int BTN_Y = 84, BTN_H = 24;
   private static final int MINUS_X = 44, MINUS_STEP = 28;
   private static final int PLUS_X = 150, PLUS_STEP = 25;
   private static final int ACT_Y = 118, ACT_H = 28;
   private static final int NO_X = 88, NO_W = 40, YES_X = 146, YES_W = 50;
   private static final int[] STEPS = {1, 16, 64};

   private final PlayerShop shop;
   private final int offerIndex;
   private final long balance;
   private final ShopOffer offer;
   private int amount = 1;
   private int left;
   private int top;

   public AmountScreen(PlayerShop shop, int offerIndex, long balance) {
      super(Component.translatable("fshop.gui.amount.title"));
      this.shop = shop;
      this.offerIndex = offerIndex;
      this.balance = balance;
      this.offer = shop.getOffers().get(offerIndex);
   }

   @Override
   protected void init() {
      this.left = (this.width - FShopTextures.GW) / 2;
      this.top = (this.height - FShopTextures.GH) / 2;
      this.amount = clamp(1);
   }

   private int clamp(int v) {
      return Math.max(1, Math.min(v, Math.max(1, offer.getStock())));
   }

   private long total() {
      return offer.getUnitPrice() * (long) amount;
   }

   private boolean canAfford() {
      return total() <= balance;
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.CONFIRMATION, left, top);

      // hover highlights over the three minus / plus steps
      for (int s = 0; s < 3; s++) {
         hover(g, mouseX, mouseY, MINUS_X + s * MINUS_STEP, BTN_Y, MINUS_STEP, BTN_H, 0x66DF2E38);
         hover(g, mouseX, mouseY, PLUS_X + s * PLUS_STEP, BTN_Y, PLUS_STEP, BTN_H, 0x6682CD47);
      }
      hover(g, mouseX, mouseY, NO_X, ACT_Y, NO_W, ACT_H, 0x66DF2E38);
      if (canAfford()) {
         hover(g, mouseX, mouseY, YES_X, ACT_Y, YES_W, ACT_H, 0x6682CD47);
      }

      // item preview in the central "64" slot, showing the chosen amount
      itemPreview(g);

      // total price just above the gray grid
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.total", CoinEconomy.formatShort(total())),
            left + 128, top + 164, canAfford() ? FShopTheme.GOLD : FShopTheme.DANGER);
      super.render(g, mouseX, mouseY, partial);
   }

   private void itemPreview(GuiGraphics g) {
      int max = offer.getItem().getMaxStackSize();
      var stack = offer.displayStack(Math.min(amount, max));
      g.renderFakeItem(stack, left + 115, top + 145);
      g.renderItemDecorations(this.font, stack, left + 115, top + 145);
   }

   private void hover(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w, int h, int color) {
      if (FShopTheme.inside(mouseX, mouseY, left + x, top + y, w, h)) {
         g.fill(left + x, top + y, left + x + w, top + y + h, color);
      }
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button != 0) {
         return super.mouseClicked(mx, my, button);
      }
      for (int s = 0; s < 3; s++) {
         if (FShopTheme.inside(mx, my, left + MINUS_X + s * MINUS_STEP, top + BTN_Y, MINUS_STEP, BTN_H)) {
            amount = clamp(amount - STEPS[s]);
            return true;
         }
         if (FShopTheme.inside(mx, my, left + PLUS_X + s * PLUS_STEP, top + BTN_Y, PLUS_STEP, BTN_H)) {
            amount = clamp(amount + STEPS[s]);
            return true;
         }
      }
      if (FShopTheme.inside(mx, my, left + NO_X, top + ACT_Y, NO_W, ACT_H)) {
         PacketHandler.sendToServer(new OpenShopRequestPacket(shop.getId()));
         return true;
      }
      if (canAfford() && FShopTheme.inside(mx, my, left + YES_X, top + ACT_Y, YES_W, ACT_H)) {
         PacketHandler.sendToServer(new BuyPacket(shop.getId(), offerIndex, amount));
         return true;
      }
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
