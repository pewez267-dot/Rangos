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
 * Quantity confirmation, aligned exactly to shop_gui_confirmation.png with the
 * plugin's real button semantics: the left red bar is [Set to 1] [-1] [-10] and
 * the right green bar is [+1] [+10] [Set to 16]; the small green button jumps to
 * a full stack. The item preview sits centred in the recessed frame (slot 22),
 * and the buyer's coin wallet shows on the gray grid instead of their inventory.
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

   private int clamp(int v) {
      return Math.max(1, Math.min(v, Math.max(1, offer.getStock())));
   }

   /** Step magnitude for stepper index: 1, 32, or a full stack. */
   private int step(int i) {
      return switch (i) {
         case 0 -> 1;
         case 1 -> 32;
         default -> fullStack();
      };
   }

   private void applyMinus(int i) {
      amount = clamp(amount - step(i));
   }

   private void applyPlus(int i) {
      amount = clamp(amount + step(i));
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

   private Component minusLabel(int i) {
      return Component.translatable("fshop.gui.amount.remove", step(i));
   }

   private Component plusLabel(int i) {
      return Component.translatable("fshop.gui.amount.add", step(i));
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

      // buyer's coin wallet on the gray grid (only their coins, not inventory);
      // the coin this offer is priced in is ringed
      int coinHov = ShopWidgets.renderCoins(g, this.font, this.minecraft.player,
            left, top, mouseX, mouseY, offer.getCoin());

      // item centred in the recessed frame (slot 22); stack count = chosen amount
      var stack = offer.displayStack(Math.min(amount, fullStack()));
      g.renderFakeItem(stack, left + FShopTextures.ITEM_CX - 8, top + FShopTextures.ITEM_CY - 8);
      g.renderItemDecorations(this.font, stack, left + FShopTextures.ITEM_CX - 8, top + FShopTextures.ITEM_CY - 8);

      super.render(g, mouseX, mouseY, partial);
      renderTooltips(g, mouseX, mouseY, coinHov);
   }

   private void renderTooltips(GuiGraphics g, int mouseX, int mouseY, int coinHov) {
      if (inBox(mouseX, mouseY, FShopTextures.ITEM_FRAME)) {
         itemBreakdownTooltip(g, mouseX, mouseY);
         return;
      }
      for (int i = 0; i < 3; i++) {
         if (inBox(mouseX, mouseY, FShopTextures.MINUS_CELLS[i])) {
            tip(g, mouseX, mouseY, minusLabel(i));
            return;
         }
         if (inBox(mouseX, mouseY, FShopTextures.PLUS_CELLS[i])) {
            tip(g, mouseX, mouseY, plusLabel(i));
            return;
         }
      }
      if (inBox(mouseX, mouseY, FShopTextures.SET_STACK_BOX)) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.amount.set64"));
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
         return;
      }
      if (coinHov >= 0) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.wallet",
               balances[coinHov], Component.translatable(CoinEconomy.coinKey(coinHov))));
      }
   }

   private void tip(GuiGraphics g, int mouseX, int mouseY, Component c) {
      List<Component> t = new ArrayList<>();
      t.add(c);
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   private void itemBreakdownTooltip(GuiGraphics g, int mouseX, int mouseY) {
      Component coin = Component.translatable(CoinEconomy.coinKey(offer.getCoin()));
      List<Component> t = new ArrayList<>();
      t.add(offer.displayStack(1).getHoverName());
      t.add(Component.translatable("fshop.gui.amount.quantity", amount).withStyle(ChatFormatting.GRAY));
      t.add(Component.translatable("fshop.gui.buy_price", offer.getUnitPrice(), coin).withStyle(ChatFormatting.GREEN));
      t.add(Component.translatable("fshop.gui.total_n", total())
            .withStyle(canAfford() ? ChatFormatting.GOLD : ChatFormatting.RED));
      t.add(Component.translatable("fshop.gui.your_balance", balances[offer.getCoin()], coin)
            .withStyle(canAfford() ? ChatFormatting.DARK_GRAY : ChatFormatting.RED));
      if (!canAfford()) {
         t.add(Component.empty());
         t.add(Component.translatable("fshop.msg.cannot_afford").withStyle(ChatFormatting.RED));
      }
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button != 0) {
         return super.mouseClicked(mx, my, button);
      }
      for (int i = 0; i < 3; i++) {
         if (inBox(mx, my, FShopTextures.MINUS_CELLS[i])) {
            applyMinus(i);
            com.fshop.client.Sfx.click();
            return true;
         }
         if (inBox(mx, my, FShopTextures.PLUS_CELLS[i])) {
            applyPlus(i);
            com.fshop.client.Sfx.click();
            return true;
         }
      }
      if (inBox(mx, my, FShopTextures.SET_STACK_BOX)) {
         amount = clamp(64);
         com.fshop.client.Sfx.click();
         return true;
      }
      if (inBox(mx, my, FShopTextures.NO_BOX)) {
         com.fshop.client.Sfx.click();
         PacketHandler.sendToServer(new OpenShopRequestPacket(shop.getId()));
         return true;
      }
      if (canAfford() && inBox(mx, my, FShopTextures.YES_BOX)) {
         com.fshop.client.Sfx.success();
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
