package com.fshop.client.screen;

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

/** Quantity selection GUI for buying, in the ShopGUI+ visual style. */
public final class AmountScreen extends Screen {
   private static final int PANEL_W = 220;
   private static final int PANEL_H = 170;

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
      this.left = (this.width - PANEL_W) / 2;
      this.top = (this.height - PANEL_H) / 2;
      this.amount = clamp(1);
   }

   private int clamp(int v) {
      int max = Math.max(1, offer.getStock());
      return Math.max(1, Math.min(v, max));
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
      FShopTheme.panel(g, left, top, PANEL_W, PANEL_H, FShopTheme.PANEL, FShopTheme.BORDER);
      g.fill(left, top, left + PANEL_W, top + 22, FShopTheme.HEADER);
      g.drawString(this.font, Component.translatable("fshop.gui.amount.title"), left + 10, top + 7,
            FShopTheme.GOLD, false);

      // item preview + name + amount
      g.renderFakeItem(offer.displayStack(Math.min(amount, offer.getItem().getMaxStackSize())),
            left + PANEL_W / 2 - 8, top + 26);
      g.drawCenteredString(this.font, offer.displayStack(1).getHoverName(), left + PANEL_W / 2, top + 46,
            FShopTheme.TEXT);
      g.drawCenteredString(this.font, "x" + amount, left + PANEL_W / 2, top + 58, FShopTheme.BUY);

      int ay = top + 70;
      btn(g, left + 14, ay, 34, 16, "-10", FShopTheme.DANGER, mouseX, mouseY);
      btn(g, left + 52, ay, 34, 16, "-1", FShopTheme.DANGER, mouseX, mouseY);
      btn(g, left + 118, ay, 34, 16, "+1", FShopTheme.BUY, mouseX, mouseY);
      btn(g, left + 156, ay, 34, 16, "+10", FShopTheme.BUY, mouseX, mouseY);

      int sy = top + 90;
      btn(g, left + 12, sy, 46, 16, "1", FShopTheme.SELL, mouseX, mouseY);
      btn(g, left + 62, sy, 46, 16, "16", FShopTheme.SELL, mouseX, mouseY);
      btn(g, left + 112, sy, 46, 16, "64", FShopTheme.SELL, mouseX, mouseY);
      btn(g, left + 162, sy, 46, 16, "MAX", FShopTheme.SELL, mouseX, mouseY);

      String totalStr = Component.translatable("fshop.gui.total", CoinEconomy.format(total())).getString();
      g.drawCenteredString(this.font, totalStr, left + PANEL_W / 2, top + 112,
            canAfford() ? FShopTheme.GOLD : FShopTheme.DANGER);

      int cy = top + 128;
      boolean confirmHov = FShopTheme.inside(mouseX, mouseY, left + 12, cy, 95, 20);
      FShopTheme.button(g, left + 12, cy, 95, 20, canAfford() ? FShopTheme.BUY : FShopTheme.BORDER, confirmHov && canAfford());
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.confirm"), left + 59, cy + 6,
            canAfford() ? FShopTheme.TEXT : FShopTheme.TEXT_DIM);
      boolean cancelHov = FShopTheme.inside(mouseX, mouseY, left + PANEL_W - 107, cy, 95, 20);
      FShopTheme.button(g, left + PANEL_W - 107, cy, 95, 20, FShopTheme.DANGER, cancelHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.cancel"), left + PANEL_W - 59, cy + 6,
            FShopTheme.TEXT);

      super.render(g, mouseX, mouseY, partial);
   }

   private void btn(GuiGraphics g, int x, int y, int w, int h, String label, int accent, int mouseX, int mouseY) {
      boolean hov = FShopTheme.inside(mouseX, mouseY, x, y, w, h);
      FShopTheme.button(g, x, y, w, h, accent, hov);
      g.drawCenteredString(this.font, label, x + w / 2, y + (h - 8) / 2, FShopTheme.TEXT);
   }


   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button != 0) {
         return super.mouseClicked(mx, my, button);
      }
      int ay = top + 70;
      if (FShopTheme.inside(mx, my, left + 14, ay, 34, 16)) {
         amount = clamp(amount - 10);
         return true;
      }
      if (FShopTheme.inside(mx, my, left + 52, ay, 34, 16)) {
         amount = clamp(amount - 1);
         return true;
      }
      if (FShopTheme.inside(mx, my, left + 118, ay, 34, 16)) {
         amount = clamp(amount + 1);
         return true;
      }
      if (FShopTheme.inside(mx, my, left + 156, ay, 34, 16)) {
         amount = clamp(amount + 10);
         return true;
      }
      int sy = top + 90;
      if (FShopTheme.inside(mx, my, left + 12, sy, 46, 16)) {
         amount = clamp(1);
         return true;
      }
      if (FShopTheme.inside(mx, my, left + 62, sy, 46, 16)) {
         amount = clamp(16);
         return true;
      }
      if (FShopTheme.inside(mx, my, left + 112, sy, 46, 16)) {
         amount = clamp(64);
         return true;
      }
      if (FShopTheme.inside(mx, my, left + 162, sy, 46, 16)) {
         amount = clamp(offer.getStock());
         return true;
      }
      int cy = top + 128;
      if (canAfford() && FShopTheme.inside(mx, my, left + 12, cy, 95, 20)) {
         PacketHandler.sendToServer(new BuyPacket(shop.getId(), offerIndex, amount));
         return true;
      }
      if (FShopTheme.inside(mx, my, left + PANEL_W - 107, cy, 95, 20)) {
         PacketHandler.sendToServer(new OpenShopRequestPacket(shop.getId()));
         return true;
      }
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
