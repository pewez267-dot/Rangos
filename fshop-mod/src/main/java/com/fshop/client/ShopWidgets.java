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
