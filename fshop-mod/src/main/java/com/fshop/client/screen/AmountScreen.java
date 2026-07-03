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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Quantity confirmation, aligned exactly to shop_gui_confirmation.png. Each of
 * the three minus/plus icons has its own tight hitbox (matching the drawn
 * glyph) and a distinct step: 1, half a stack, a full stack. The "64" button
 * jumps straight to a full stack, and the item preview sits centred in the
 * recessed frame between the two bars.
 */
public final class AmountScreen extends Screen {
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

   private boolean inBox(double mx, double my, int[] box) {
      return FShopTheme.inside(mx, my, left + box[0], top + box[1], box[2] - box[0], box[3] - box[1]);
   }

   private void hoverBox(GuiGraphics g, int mouseX, int mouseY, int[] box) {
      if (inBox(mouseX, mouseY, box)) {
         g.fill(left + box[0], top + box[1], left + box[2], top + box[3], 0x55FFFFFF);
      }
   }

   private String stepLabel(int idx, boolean plus) {
      String sign = plus ? "+" : "-";
      return switch (idx) {
         case 1 -> sign + (fullStack() / 2);
         case 2 -> sign + fullStack();
         default -> sign + "1";
      };
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.CONFIRMATION, left, top);

      for (int[] box : FShopTextures.MINUS_CELLS) {
         hoverBox(g, mouseX, mouseY, box);
      }
      for (int[] box : FShopTextures.PLUS_CELLS) {
         hoverBox(g, mouseX, mouseY, box);
      }
      hoverBox(g, mouseX, mouseY, FShopTextures.NO_BOX);
      if (canAfford()) {
         hoverBox(g, mouseX, mouseY, FShopTextures.YES_BOX);
      } else {
         int[] y = FShopTextures.YES_BOX;
         g.fill(left + y[0], top + y[1], left + y[2], top + y[3], 0x77000000);
      }
      hoverBox(g, mouseX, mouseY, FShopTextures.SET_STACK_BOX);

      // item centred inside the recessed frame between the two bars
      int[] fr = FShopTextures.ITEM_FRAME;
      int fw = fr[2] - fr[0];
      int fh = fr[3] - fr[1];
      int ix = left + fr[0] + (fw - 16) / 2;
      int iy = top + fr[1] + (fh - 16) / 2;
      var stack = offer.displayStack(Math.min(amount, fullStack()));
      g.renderFakeItem(stack, ix, iy);
      g.renderItemDecorations(this.font, stack, ix, iy);

      // info panel (same light-gray palette as the rest of the shop)
      ShopWidgets.dimBottom(g, left, top);
      g.drawCenteredString(this.font, "x" + amount + "  " + offer.displayStack(1).getHoverName().getString(),
            left + 128, top + 178, FShopTheme.TEXT);
      int cx = left + 100;
      g.renderFakeItem(CoinEconomy.coinIcon(offer.getCoin()), cx, top + 194);
      g.drawString(this.font, Component.translatable("fshop.gui.total_n", total()),
            cx + 20, top + 198, canAfford() ? FShopTheme.GOLD : FShopTheme.DANGER, false);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.your_balance", balances[offer.getCoin()],
            Component.translatable(CoinEconomy.coinKey(offer.getCoin()))), left + 128, top + 216,
            canAfford() ? FShopTheme.TEXT_DIM : FShopTheme.DANGER);

      super.render(g, mouseX, mouseY, partial);
      renderTooltips(g, mouseX, mouseY);
   }

   private void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
      for (int i = 0; i < 3; i++) {
         if (inBox(mouseX, mouseY, FShopTextures.MINUS_CELLS[i])) {
            tip(g, mouseX, mouseY, Component.translatable("fshop.gui.amount.step", stepLabel(i, false)));
            return;
         }
         if (inBox(mouseX, mouseY, FShopTextures.PLUS_CELLS[i])) {
            tip(g, mouseX, mouseY, Component.translatable("fshop.gui.amount.step", stepLabel(i, true)));
            return;
         }
      }
      if (inBox(mouseX, mouseY, FShopTextures.SET_STACK_BOX)) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.amount.set_stack", fullStack()));
         return;
      }
      if (inBox(mouseX, mouseY, FShopTextures.NO_BOX)) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.amount.tip_no"));
         return;
      }
      if (inBox(mouseX, mouseY, FShopTextures.YES_BOX)) {
         tip(g, mouseX, mouseY, canAfford()
               ? Component.translatable("fshop.gui.amount.tip_yes")
               : Component.translatable("fshop.msg.cannot_afford"));
      }
   }

   private void tip(GuiGraphics g, int mouseX, int mouseY, Component c) {
      List<Component> t = new ArrayList<>();
      t.add(c);
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button != 0) {
         return super.mouseClicked(mx, my, button);
      }
      for (int i = 0; i < 3; i++) {
         if (inBox(mx, my, FShopTextures.MINUS_CELLS[i])) {
            amount = clamp(amount - step(i));
            return true;
         }
         if (inBox(mx, my, FShopTextures.PLUS_CELLS[i])) {
            amount = clamp(amount + step(i));
            return true;
         }
      }
      if (inBox(mx, my, FShopTextures.SET_STACK_BOX)) {
         amount = clamp(fullStack());
         return true;
      }
      if (inBox(mx, my, FShopTextures.NO_BOX)) {
         PacketHandler.sendToServer(new OpenShopRequestPacket(shop.getId()));
         return true;
      }
      if (canAfford() && inBox(mx, my, FShopTextures.YES_BOX)) {
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
