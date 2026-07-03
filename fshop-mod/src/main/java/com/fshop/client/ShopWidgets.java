package com.fshop.client;

import com.fshop.economy.CoinEconomy;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shared rendering helpers for the inventory / coin area of the shop screens. */
public final class ShopWidgets {
   private ShopWidgets() {
   }

   /**
    * Draws the player inventory over the gray grid (used only on the create/edit
    * screen for stocking). When {@code interactive}, hovered non-empty slots are
    * highlighted and the hovered slot index is returned; otherwise returns -1.
    */
   public static int renderInventory(GuiGraphics g, Font font, Inventory inv,
         int left, int top, int mouseX, int mouseY, boolean interactive) {
      int hovered = -1;
      for (int row = 0; row < FShopTextures.INV_ROWS; row++) {
         for (int col = 0; col < FShopTextures.INV_COLS; col++) {
            int cx = left + FShopTextures.invCellX(col);
            int cy = top + FShopTextures.invCellY(row);
            int slot = FShopTextures.invSlot(row, col);
            ItemStack st = inv.getItem(slot);
            boolean hov = interactive && !st.isEmpty()
                  && FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL, FShopTextures.CELL);
            if (hov) {
               g.fill(cx, cy, cx + FShopTextures.CELL, cy + FShopTextures.CELL, 0x6682CD47);
               hovered = slot;
            }
            if (!st.isEmpty()) {
               g.renderFakeItem(st, cx + 1, cy + 1);
               g.renderItemDecorations(font, st, cx + 1, cy + 1);
            }
         }
      }
      return hovered;
   }

   /** Returns the inventory slot at (mx,my), or -1. */
   public static int slotAt(Inventory inv, int left, int top, double mx, double my) {
      for (int row = 0; row < FShopTextures.INV_ROWS; row++) {
         for (int col = 0; col < FShopTextures.INV_COLS; col++) {
            int cx = left + FShopTextures.invCellX(col);
            int cy = top + FShopTextures.invCellY(row);
            if (FShopTheme.inside(mx, my, cx, cy, FShopTextures.CELL, FShopTextures.CELL)) {
               int slot = FShopTextures.invSlot(row, col);
               return inv.getItem(slot).isEmpty() ? -1 : slot;
            }
         }
      }
      return -1;
   }

   /**
    * Draws the player's three coin balances as real coin stacks seated in the
    * gray grid (this is what the buyer sees in place of their inventory). Returns
    * the hovered coin type (0-2) or -1. If {@code highlight} is 0-2 that coin is
    * ringed as selected.
    */
   public static int renderCoins(GuiGraphics g, Font font, Player player,
         int left, int top, int mouseX, int mouseY, int highlight) {
      int hovered = -1;
      for (int c = 0; c < 3; c++) {
         int cx = left + FShopTextures.coinCellX(c);
         int cy = top + FShopTextures.coinCellY();
         // seat the coin in an empty-slot frame so it reads as a real slot
         g.blit(FShopTextures.EMPTY_SLOT, cx + 1, cy + 1, 0.0F, 0.0F, 16, 16, 16, 16);
         boolean hov = FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL, FShopTextures.CELL);
         if (c == highlight) {
            g.fill(cx, cy, cx + FShopTextures.CELL, cy + FShopTextures.CELL, 0x66FFD24A);
         }
         if (hov) {
            g.fill(cx, cy, cx + FShopTextures.CELL, cy + FShopTextures.CELL, 0x6682CD47);
            hovered = c;
         }
         ItemStack coin = CoinEconomy.coinIcon(c);
         if (!coin.isEmpty()) {
            long bal = CoinEconomy.balance(player, c);
            g.renderFakeItem(coin, cx + 1, cy + 1);
            g.renderItemDecorations(font, coin, cx + 1, cy + 1, Long.toString(bal));
         }
      }
      return hovered;
   }

   /** Returns the coin type (0-2) whose wallet slot is at (mx,my), or -1. */
   public static int coinAt(int left, int top, double mx, double my) {
      for (int c = 0; c < 3; c++) {
         int cx = left + FShopTextures.coinCellX(c);
         int cy = top + FShopTextures.coinCellY();
         if (FShopTheme.inside(mx, my, cx, cy, FShopTextures.CELL, FShopTextures.CELL)) {
            return c;
         }
      }
      return -1;
   }
}
