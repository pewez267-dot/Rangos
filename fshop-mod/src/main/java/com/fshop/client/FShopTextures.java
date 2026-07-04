package com.fshop.client;

import com.fshop.FShop;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The Spectra ShopGUI+ storefront textures plus the exact slot geometry, all
 * measured pixel-by-pixel from the 256x256 textures. Everything maps to the
 * plugin's real 54-slot (6-row) chest layout: 9 columns pitched every 18px with
 * column 0 centred at x=55.5, i.e. {@code colCenter(c) = 55.5 + 18*c}.
 *
 * <p>The wooden display window shows the shop items in the inner 7x4 block
 * (grid columns 1-7, rows 1-4 = slots 10-43); the light-gray grid underneath is
 * the real inventory area used for stocking (owner) or the coin wallet (buyer).
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
   public static final ResourceLocation BACK_BUTTON = gui("shop_back_button");
   public static final ResourceLocation NEXT_BUTTON = gui("shop_next_button");
   public static final ResourceLocation EMPTY_SLOT = gui("empty_slot");

   public static final int GW = 256;
   public static final int GH = 256;
   public static final int CELL = 18;
   public static final int PITCH = 18;

   // --- Shop item window: inner 7 columns x 4 rows (measured slot recesses) ---
   public static final int CONTENT_COLS = 7;
   public static final int CONTENT_ROWS = 4;
   // First inner slot recess is centred at (73.5, 79); pitch 18. A 16px item
   // centres at (cellLeft+1); an 18px hit cell top-left is one pixel higher/left.
   private static final int CONTENT_CELL_X0 = 65;
   private static final int CONTENT_CELL_Y0 = 70;

   public static int contentCells() {
      return CONTENT_COLS * CONTENT_ROWS;
   }

   public static int contentCellX(int i) {
      return CONTENT_CELL_X0 + (i % CONTENT_COLS) * PITCH;
   }

   public static int contentCellY(int i) {
      return CONTENT_CELL_Y0 + (i / CONTENT_COLS) * PITCH;
   }

   public static int contentItemX(int i) {
      return contentCellX(i) + 1;
   }

   public static int contentItemY(int i) {
      return contentCellY(i) + 1;
   }

   // --- Gray inventory grid: 9 columns x 4 rows (measured) ---
   public static final int INV_COLS = 9;
   public static final int INV_ROWS = 4;
   public static final int COL_X0 = 47;
   public static final int[] INV_ROW_Y = {173, 191, 209, 231};

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

   // --- Coin wallet: three coins centred on the gray grid (row 1, cols 3-5) ---
   public static final int COIN_ROW = 1;
   public static final int COIN_COL0 = 3;

   public static int coinCellX(int c) {
      return invCellX(COIN_COL0 + c);
   }

   public static int coinCellY() {
      return invCellY(COIN_ROW);
   }

   public static void blitPanel(GuiGraphics g, ResourceLocation tex, int left, int top) {
      g.blit(tex, left, top, 0, 0, GW, GH, GW, GH);
   }

   // --- Amount / price confirmation geometry (measured on shop_gui_confirmation.png) ---
   // The 3 minus cells (cols 0,1,2) and 3 plus cells (cols 6,7,8) of the 54-slot
   // grid; row centred at y=95. Boxes are {x0,y0,x1,y1}.
   // Full raised sub-buttons (18px, contiguous) so the whole visible button is
   // clickable -- no dead space between the three minus/plus steps.
   public static final int[][] MINUS_CELLS = {{47, 84, 65, 108}, {65, 84, 83, 108}, {83, 84, 101, 108}};
   public static final int[][] PLUS_CELLS = {{155, 84, 173, 108}, {173, 84, 191, 108}, {191, 84, 209, 108}};
   public static final int[] NO_BOX = {86, 124, 116, 139};
   public static final int[] YES_BOX = {140, 124, 170, 139};
   public static final int[] SET_STACK_BOX = {120, 143, 136, 155};
   /** Item recess at slot 22, measured exactly at x[120,135] y[88,103] (centre 128,96). */
   public static final int[] ITEM_FRAME = {120, 88, 136, 104};
   public static final int ITEM_CX = 128;
   public static final int ITEM_CY = 96;

   // --- Earnings coins on the manage screen: wooden header row (row 0), the
   // three cells to the left of the house icon (inner cols 0,1,2). ---
   public static int earnCellX(int i) {
      return contentCellX(i);
   }

   public static int earnCellY() {
      return contentCellY(0) - PITCH;
   }

   // --- Buy-screen pagination: arrows seated in the bottom corners of the gray
   // grid (row 3, far-left and far-right). Only used by browse/view. ---
   public static final int[] PAGE_PREV_CELL = {47, 231};
   public static final int[] PAGE_NEXT_CELL = {191, 231};

   // --- Sell-amount picker geometry (measured on shop_gui_stack.png) ---
   // A single green-bordered row of 7 slots (cell top y=89) sharing the content
   // grid's column pitch, a CANCEL button, and the tan plaque used to draw the
   // Spanish label over the baked-in "BUY STACKS!" text.
   public static final int STACK_CELL_Y = 89;
   public static final int STACK_SLOTS = 7;
   public static final int[] STACK_CANCEL_BOX = {106, 122, 151, 139};
   public static final int[] STACK_PLAQUE = {89, 60, 165, 77};

   public static int stackCellX(int i) {
      return contentCellX(i);
   }

   // --- 54-slot navigation positions (measured on shop_item_display.png) ---
   // Back = slot 4 (house icon, centred at 128,61), previous = slot 27
   // (col 0, row 3), next = slot 35 (col 8, row 3). Values are 18px cell top-left.
   public static final int[] HOME_CELL = {119, 52};
   public static final int[] PREV_CELL = {47, 106};
   public static final int[] NEXT_CELL = {191, 106};

   public static void blitIcon(GuiGraphics g, ResourceLocation tex, int left, int top, int[] cell) {
      g.blit(tex, left + cell[0] + 1, top + cell[1] + 1, 0.0F, 0.0F, 16, 16, 16, 16);
   }

   public static boolean inCell(double mx, double my, int left, int top, int[] cell) {
      return mx >= left + cell[0] && mx < left + cell[0] + CELL
            && my >= top + cell[1] && my < top + cell[1] + CELL;
   }

   public static void hoverCell(GuiGraphics g, int left, int top, int[] cell, boolean hovered) {
      if (hovered) {
         g.fill(left + cell[0], top + cell[1], left + cell[0] + CELL, top + cell[1] + CELL, 0x55FFFFFF);
      }
   }
}
