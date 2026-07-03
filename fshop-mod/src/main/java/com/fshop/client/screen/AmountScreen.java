package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.client.ShopWidgets;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.BuyPacket;
import com.fshop.network.OpenShopRequestPacket;
import com.fshop.network.PacketHandler;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ShopOffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Quantity confirmation, aligned exactly to shop_gui_confirmation.png. */
public final class AmountScreen extends Screen {
   private static final int STEP_BTN_Y = 85, STEP_BTN_H = 26, STEP_W = 20;
   private static final int MINUS_X = 44, PLUS_X = 152;
   private static final int ACT_Y = 124, ACT_H = 15;
   private static final int NO_X = 86, NO_W = 30, YES_X = 140, YES_W = 30;
   private static final int ITEM_X = 119, ITEM_Y = 90;
   private static final int[] STEPS = {1, 16, 64};

   private final PlayerShop shop;
   private final int offerIndex;
   private final long[] balances;
   private final ShopOffer offer;
   private int amount = 1;
   private int left;
   private int top;

   public AmountScreen(PlayerShop shop, int offerIndex, long[] balances) {
      super(Component.translatable("fshop.gui.amount.title"));
      this.shop = shop;
      this.offerIndex = offerIndex;
      this.balances = balances;
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
      return total() <= balances[offer.getCoin()];
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.CONFIRMATION, left, top);

      for (int s = 0; s < 3; s++) {
         hover(g, mouseX, mouseY, MINUS_X + s * STEP_W, STEP_BTN_Y, STEP_W, STEP_BTN_H);
         hover(g, mouseX, mouseY, PLUS_X + s * STEP_W, STEP_BTN_Y, STEP_W, STEP_BTN_H);
      }
      hover(g, mouseX, mouseY, NO_X, ACT_Y, NO_W, ACT_H);
      if (canAfford()) {
         hover(g, mouseX, mouseY, YES_X, ACT_Y, YES_W, ACT_H);
      } else {
         g.fill(left + YES_X, top + ACT_Y, left + YES_X + YES_W, top + ACT_Y + ACT_H, 0x77000000);
      }

      // item preview in the central frame, between the - and + buttons
      var stack = offer.displayStack(Math.min(amount, offer.getItem().getMaxStackSize()));
      g.renderFakeItem(stack, left + ITEM_X, top + ITEM_Y);
      g.renderItemDecorations(this.font, stack, left + ITEM_X, top + ITEM_Y);

      // total + balance in the bottom panel
      ShopWidgets.dimBottom(g, left, top);
      g.drawCenteredString(this.font, "x" + amount + "  " + offer.displayStack(1).getHoverName().getString(),
            left + 128, top + 176, FShopTheme.TEXT);
      int cx = left + 96;
      g.renderFakeItem(CoinEconomy.coinIcon(offer.getCoin()), cx, top + 194);
      g.drawString(this.font, Component.translatable("fshop.gui.total_n", total()),
            cx + 20, top + 198, canAfford() ? FShopTheme.GOLD : FShopTheme.DANGER, false);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.your_balance", balances[offer.getCoin()],
            Component.translatable(CoinEconomy.coinKey(offer.getCoin()))), left + 128, top + 216,
            canAfford() ? FShopTheme.TEXT_DIM : FShopTheme.DANGER);

      super.render(g, mouseX, mouseY, partial);
   }

   private void hover(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w, int h) {
      if (FShopTheme.inside(mouseX, mouseY, left + x, top + y, w, h)) {
         g.fill(left + x + 1, top + y + 1, left + x + w - 1, top + y + h - 1, 0x55FFFFFF);
      }
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button != 0) {
         return super.mouseClicked(mx, my, button);
      }
      for (int s = 0; s < 3; s++) {
         if (FShopTheme.inside(mx, my, left + MINUS_X + s * STEP_W, top + STEP_BTN_Y, STEP_W, STEP_BTN_H)) {
            amount = clamp(amount - STEPS[s]);
            return true;
         }
         if (FShopTheme.inside(mx, my, left + PLUS_X + s * STEP_W, top + STEP_BTN_Y, STEP_W, STEP_BTN_H)) {
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
