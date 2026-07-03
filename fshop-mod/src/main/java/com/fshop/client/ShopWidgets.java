package com.fshop.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Shared rendering helpers for the player-inventory area of the shop screens. */
public final class ShopWidgets {
   private ShopWidgets() {
   }

   /**
    * Draws the player inventory over the gray grid. When {@code interactive} is
    * true, hovered non-empty slots are highlighted and the hovered slot index is
    * returned; otherwise it is purely visual and returns -1.
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

   /** Bounds of the bottom panel that hosts navigation (covers the gray grid). */
   public static final int BOTTOM_X0 = 44;
   public static final int BOTTOM_X1 = 212;
   public static final int BOTTOM_Y0 = 166;
   public static final int BOTTOM_Y1 = 252;

   /**
    * Covers the gray-grid area with the SAME inventory palette as the rest of
    * the storefront (light gray body, dark recessed border) so it reads as
    * part of the shop UI instead of a foreign dark overlay, then hosts the
    * navigation controls on top.
    */
   public static void dimBottom(GuiGraphics g, int left, int top) {
      int x0 = left + BOTTOM_X0;
      int y0 = top + BOTTOM_Y0;
      int w = BOTTOM_X1 - BOTTOM_X0;
      int h = BOTTOM_Y1 - BOTTOM_Y0;
      g.fill(x0, y0, x0 + w, y0 + h, FShopTheme.INV_LIGHT);
      g.fill(x0 + 2, y0 + 2, x0 + w - 2, y0 + h - 2, FShopTheme.INV_MID);
      g.fill(x0, y0, x0 + w, y0 + 1, FShopTheme.INV_DARK);
      g.fill(x0, y0 + h - 1, x0 + w, y0 + h, FShopTheme.INV_DARK);
      g.fill(x0, y0, x0 + 1, y0 + h, FShopTheme.INV_DARK);
      g.fill(x0 + w - 1, y0, x0 + w, y0 + h, FShopTheme.INV_DARK);
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
}
