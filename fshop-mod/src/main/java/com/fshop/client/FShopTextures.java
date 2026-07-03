package com.fshop.client;

import com.fshop.FShop;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The Spectra ShopGUI+ storefront textures plus the exact slot geometry, which
 * mirrors a standard 6-row chest: shop items sit in the wooden display window
 * (inner 7x4 = slots 10-43) and the player inventory sits on the gray grid.
 * All coordinates were measured directly from the 256x256 textures.
 */
public final class FShopTextures {
   private FShopTextures() {
   }

   private static ResourceLocation gui(String name) {
      return new ResourceLocation(FShop.MOD_ID, "textures/gui/" + name + ".png");
   }

   public static final ResourceLocation MENU = gui("shop_gui_menu");
   public static final ResourceLocation ITEM_DISPLAY = gui("shop_item_display");
   public static final ResourceLocation SELL_MENU = gui("shop_gui_sell_menu");
   public static final ResourceLocation CONFIRMATION = gui("shop_gui_confirmation");
   public static final ResourceLocation STACK = gui("shop_gui_stack");

   public static final int GW = 256;
   public static final int GH = 256;

   public static final int PITCH = 18;
   public static final int COL_X0 = 47;       // left edge of chest column 0
   public static final int CONTENT_ROW0 = 51; // top of container row 0
   public static final int CELL = 18;

   // Shop item area: columns 1-7, rows 1-4 (28 slots), just like the plugin.
   public static final int CONTENT_COLS = 7;
   public static final int CONTENT_ROWS = 4;

   // Player inventory: 9 columns x 4 rows on the gray grid.
   public static final int INV_COLS = 9;
   public static final int INV_ROWS = 4;
   public static final int[] INV_ROW_Y = {173, 191, 209, 231};

   public static int contentCells() {
      return CONTENT_COLS * CONTENT_ROWS;
   }

   public static int contentCellX(int i) {
      return COL_X0 + (1 + i % CONTENT_COLS) * PITCH;
   }

   public static int contentCellY(int i) {
      return CONTENT_ROW0 + (1 + i / CONTENT_COLS) * PITCH;
   }

   public static int invCellX(int col) {
      return COL_X0 + col * PITCH;
   }

   public static int invCellY(int row) {
      return INV_ROW_Y[row];
   }

   /** Maps a gray-grid (row,col) to a real player-inventory slot index. */
   public static int invSlot(int row, int col) {
      return row < 3 ? 9 + row * 9 + col : col;
   }

   public static void blitPanel(GuiGraphics g, ResourceLocation tex, int left, int top) {
      g.blit(tex, left, top, 0, 0, GW, GH, GW, GH);
   }
}
