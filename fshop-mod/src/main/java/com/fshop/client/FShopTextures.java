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
   public static final int COL_X0 = 47;
   public static final int CELL = 18;

   // Shop item area (measured cell centres in the wooden window): 7 cols x 4 rows.
   public static final int CONTENT_COLS = 7;
   public static final int CONTENT_ROWS = 4;
   private static final int[] COL_CX = {74, 91, 109, 127, 145, 163, 180};
   private static final int[] ROW_CY = {78, 95, 113, 131};

   // Player inventory: 9 columns x 4 rows on the gray grid.
   public static final int INV_COLS = 9;
   public static final int INV_ROWS = 4;
   public static final int[] INV_ROW_Y = {173, 191, 209, 231};

   public static int contentCells() {
      return CONTENT_COLS * CONTENT_ROWS;
   }

   /** Top-left of the 18px hit cell for content index i. */
   public static int contentCellX(int i) {
      return COL_CX[i % CONTENT_COLS] - 9;
   }

   public static int contentCellY(int i) {
      return ROW_CY[i / CONTENT_COLS] - 9;
   }

   /** Item (16px) render position, centred in the cell. */
   public static int contentItemX(int i) {
      return COL_CX[i % CONTENT_COLS] - 8;
   }

   public static int contentItemY(int i) {
      return ROW_CY[i / CONTENT_COLS] - 8;
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

   // --- Amount-confirmation screen geometry (measured from shop_gui_confirmation.png) ---
   /** The 3 minus-icon cells (x0,y0,x1,y1) inside the red bar, left to right: -1, -half stack, -stack. */
   public static final int[][] MINUS_CELLS = {{48, 88, 63, 103}, {66, 88, 81, 103}, {84, 88, 99, 103}};
   /** The 3 plus-icon cells inside the green bar, left to right: +1, +half stack, +stack. */
   public static final int[][] PLUS_CELLS = {{156, 88, 171, 103}, {174, 88, 189, 103}, {192, 88, 207, 103}};
   public static final int[] NO_BOX = {86, 124, 116, 139};
   public static final int[] YES_BOX = {140, 124, 170, 139};
   public static final int[] SET_STACK_BOX = {121, 142, 136, 155};
   /** Recessed centre frame that displays the item being bought. */
   public static final int[] ITEM_FRAME = {112, 84, 141, 109};
}
