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

/**
 * Quantity confirmation aligned exactly to shop_gui_confirmation.png.
 * The three minus/plus icons step by 1, half a stack and a full stack; the "64"
 * button sets the amount to a full stack.
 */
public final class AmountScreen extends Screen {
   private static final int[] MINUS_CX = {55, 73, 91};
   private static final int[] PLUS_CX = {163, 181, 199};
   private static final int ICON_HALF = 7, ICON_Y = 88, ICON_H = 18;
   private static final int NO_X = 86, YES_X = 140, ACT_Y = 124, ACT_W = 30, ACT_H = 15;
   private static final int SET_X = 121, SET_Y = 142, SET_W = 15, SET_H = 13;
   private static final int ITEM_X = 119, ITEM_Y = 91;

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

   private int fullStack() {
      return Math.max(1, offer.getItem().getMaxStackSize());
   }

   private int step(int idx) {
      return switch (idx) {
         case 1 -> Math.max(1, fullStack() / 2);
         case 2 -> fullStack();
         default -> 1;
      };
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

      // hover highlights fitted to each icon
      for (int i = 0; i < 3; i++) {
         iconHover(g, mouseX, mouseY, MINUS_CX[i]);
         iconHover(g, mouseX, mouseY, PLUS_CX[i]);
      }
      hover(g, mouseX, mouseY, NO_X, ACT_Y, ACT_W, ACT_H);
      if (canAfford()) {
         hover(g, mouseX, mouseY, YES_X, ACT_Y, ACT_W, ACT_H);
      } else {
         g.fill(left + YES_X, top + ACT_Y, left + YES_X + ACT_W, top + ACT_Y + ACT_H, 0x77000000);
      }
      hover(g, mouseX, mouseY, SET_X, SET_Y, SET_W, SET_H);

      // item in the central frame, between the - and + bars
      var stack = offer.displayStack(Math.min(amount, fullStack()));
      g.renderFakeItem(stack, left + ITEM_X, top + ITEM_Y);
      g.renderItemDecorations(this.font, stack, left + ITEM_X, top + ITEM_Y);

      // info in the bottom panel
      ShopWidgets.dimBottom(g, left, top);
      g.drawCenteredString(this.font, "x" + amount + "  " + offer.displayStack(1).getHoverName().getString(),
            left + 128, top + 178, FShopTheme.TEXT);
      int cx = left + 100;
      g.renderFakeItem(CoinEconomy.coinIcon(offer.getCoin()), cx, top + 194);
      g.drawString(this.font, Component.translatable("fshop.gui.total_n", total()),
            cx + 20, top + 198, canAfford() ? FShopTheme.GOLD : FShopTheme.DANGER, false);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.your_balance", balances[offer.getCoin()],
            Component.translatable(CoinEconomy.coinKey(offer.getCoin()))), left + 128, top + 218,
            canAfford() ? FShopTheme.TEXT_DIM : FShopTheme.DANGER);

      super.render(g, mouseX, mouseY, partial);
   }

   private void iconHover(GuiGraphics g, int mouseX, int mouseY, int cx) {
      if (FShopTheme.inside(mouseX, mouseY, left + cx - ICON_HALF, top + ICON_Y, ICON_HALF * 2, ICON_H)) {
         g.fill(left + cx - ICON_HALF, top + ICON_Y, left + cx + ICON_HALF, top + ICON_Y + ICON_H, 0x55FFFFFF);
      }
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
      for (int i = 0; i < 3; i++) {
         if (inIcon(mx, my, MINUS_CX[i])) {
            amount = clamp(amount - step(i));
            return true;
         }
         if (inIcon(mx, my, PLUS_CX[i])) {
            amount = clamp(amount + step(i));
            return true;
         }
      }
      if (FShopTheme.inside(mx, my, left + SET_X, top + SET_Y, SET_W, SET_H)) {
         amount = clamp(fullStack());
         return true;
      }
      if (FShopTheme.inside(mx, my, left + NO_X, top + ACT_Y, ACT_W, ACT_H)) {
         PacketHandler.sendToServer(new OpenShopRequestPacket(shop.getId()));
         return true;
      }
      if (canAfford() && FShopTheme.inside(mx, my, left + YES_X, top + ACT_Y, ACT_W, ACT_H)) {
         PacketHandler.sendToServer(new BuyPacket(shop.getId(), offerIndex, amount));
         return true;
      }
      return super.mouseClicked(mx, my, button);
   }

   private boolean inIcon(double mx, double my, int cx) {
      return FShopTheme.inside(mx, my, left + cx - ICON_HALF, top + ICON_Y, ICON_HALF * 2, ICON_H);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
