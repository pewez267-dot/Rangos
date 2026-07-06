package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.client.Sfx;
import com.fshop.shop.PlayerShop;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * "How many units per sale" picker for a shop offer, built on the REAL Spectra
 * shop_gui_stack.png texture (the plugin's stack-amount screen) instead of a
 * hand-drawn panel: the seller taps one of the seven green-framed slots to sell
 * one at a time, by fractions of a stack, or by the full stack. The chosen
 * amount is the offer's bundle (the price on the next screen is per bundle),
 * matching exactly how the admin creator lets the server shop price by pack.
 *
 * <p>The only overlay is a Spanish caption drawn on the tan plaque so it reads
 * "Vender por" rather than the texture's baked-in English "BUY STACKS!". The
 * screen always leads back to {@link PriceInputScreen} (with the picked amount,
 * or unchanged on CANCEL), so the flow is: choose amount, then set the price.
 */
public final class SellAmountScreen extends Screen {
   private final PlayerShop shop;
   private final PriceInputScreen.Mode mode;
   private final int ref;
   private final long price;
   private final int coin;
   private final int currentBundle;

   private ItemStack itemStack = ItemStack.EMPTY;
   private int[] amounts = {1};
   private int left;
   private int top;

   public SellAmountScreen(PlayerShop shop, PriceInputScreen.Mode mode, int ref,
         long price, int coin, int currentBundle) {
      super(Component.translatable("fshop.gui.amountsel.title"));
      this.shop = shop;
      this.mode = mode;
      this.ref = ref;
      this.price = price;
      this.coin = coin;
      this.currentBundle = Math.max(1, currentBundle);
   }

   @Override
   protected void init() {
      this.left = (this.width - FShopTextures.GW) / 2;
      this.top = (this.height - FShopTextures.GH) / 2;
      if (mode == PriceInputScreen.Mode.ADD) {
         this.itemStack = this.minecraft.player.getInventory().getItem(ref).copy();
      } else if (ref >= 0 && ref < shop.getOffers().size()) {
         this.itemStack = shop.getOffers().get(ref).displayStack(1);
      }
      if (!this.itemStack.isEmpty()) {
         this.itemStack.setCount(1);
      }
      this.amounts = presetAmounts(Math.max(1, this.itemStack.getMaxStackSize()));
   }

   /**
    * Up to {@link FShopTextures#STACK_SLOTS} sensible per-sale amounts for an
    * item whose max stack size is {@code max}: a fixed ladder capped to the
    * item (so a tool that only stacks to 1 shows just "1"), always including the
    * full stack as the last option.
    */
   private static int[] presetAmounts(int max) {
      int[] ladder = {1, 8, 16, 24, 32, 48, 64};
      List<Integer> out = new ArrayList<>();
      for (int a : ladder) {
         int v = Math.min(a, max);
         if (!out.contains(v)) {
            out.add(v);
         }
      }
      if (!out.contains(max)) {
         if (out.size() < FShopTextures.STACK_SLOTS) {
            out.add(max);
         } else {
            out.set(out.size() - 1, max);
         }
      }
      int[] r = new int[Math.min(out.size(), FShopTextures.STACK_SLOTS)];
      for (int i = 0; i < r.length; i++) {
         r[i] = out.get(i);
      }
      return r;
   }

   private void choose(int amount) {
      Sfx.select();
      this.minecraft.setScreen(new PriceInputScreen(shop, mode, ref, price, coin, amount));
   }

   private boolean inBox(double mx, double my, int[] b) {
      return FShopTheme.inside(mx, my, left + b[0], top + b[1], b[2] - b[0], b[3] - b[1]);
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.STACK, left, top);
      FShopTheme.footerHint(g, this.font, this.width, this.height,
            Component.translatable("fshop.gui.amountsel.hint"));

      // Spanish caption over the baked-in "BUY STACKS!" plaque.
      int[] pl = FShopTextures.STACK_PLAQUE;
      g.fill(left + pl[0], top + pl[1], left + pl[2], top + pl[3], FShopTheme.PLAQUE);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.amountsel.plaque"),
            left + (pl[0] + pl[2]) / 2, top + pl[1] + 2, FShopTheme.GOLD);

      int hovered = -1;
      for (int i = 0; i < amounts.length; i++) {
         int cx = left + FShopTextures.stackCellX(i);
         int cy = top + FShopTextures.STACK_CELL_Y;
         if (amounts[i] == currentBundle) {
            g.fill(cx, cy, cx + FShopTextures.CELL, cy + FShopTextures.CELL, 0x66FFD24A);
         }
         boolean hov = FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL, FShopTextures.CELL);
         if (hov) {
            g.fill(cx, cy, cx + FShopTextures.CELL, cy + FShopTextures.CELL, 0x6682CD47);
            hovered = i;
         }
         if (!itemStack.isEmpty()) {
            g.renderFakeItem(itemStack, cx + 1, cy + 1);
            FShopTheme.drawCount(g, this.font, cx + 1, cy + 1, Integer.toString(amounts[i]));
         }
      }

      boolean cancelHov = inBox(mouseX, mouseY, FShopTextures.STACK_CANCEL_BOX);
      if (cancelHov) {
         int[] b = FShopTextures.STACK_CANCEL_BOX;
         g.fill(left + b[0], top + b[1], left + b[2], top + b[3], 0x55FFFFFF);
      }

      super.render(g, mouseX, mouseY, partial);

      if (hovered >= 0) {
         List<Component> t = new ArrayList<>();
         t.add(itemStack.getHoverName());
         t.add(Component.translatable("fshop.gui.amountsel.pick", amounts[hovered]));
         g.renderComponentTooltip(this.font, t, mouseX, mouseY);
      } else if (cancelHov) {
         List<Component> t = new ArrayList<>();
         t.add(Component.translatable("fshop.gui.amountsel.cancel"));
         g.renderComponentTooltip(this.font, t, mouseX, mouseY);
      }
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         for (int i = 0; i < amounts.length; i++) {
            int cx = left + FShopTextures.stackCellX(i);
            int cy = top + FShopTextures.STACK_CELL_Y;
            if (FShopTheme.inside(mx, my, cx, cy, FShopTextures.CELL, FShopTextures.CELL)) {
               choose(amounts[i]);
               return true;
            }
         }
         if (inBox(mx, my, FShopTextures.STACK_CANCEL_BOX)) {
            Sfx.click();
            this.minecraft.setScreen(new PriceInputScreen(shop, mode, ref, price, coin, currentBundle));
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
