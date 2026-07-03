package com.fshop.client;

import com.fshop.FShop;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The Spectra ShopGUI+ storefront textures and the pixel geometry used to align
 * interactive elements on top of them. All storefront textures are 256x256. The
 * clickable content is placed on the clean gray grid at the bottom of the
 * texture (9 columns x 4 rows), leaving the decorative storefront untouched.
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

   /** Canvas size of every storefront texture. */
   public static final int GW = 256;
   public static final int GH = 256;

   // Bottom gray grid -> the interactive slot area (measured from the texture).
   public static final int GRID_COLS = 9;
   public static final int GRID_ROWS = 4;
   public static final int GRID_X0 = 47;   // left edge of the first cell
   public static final int GRID_Y0 = 169;  // top edge of the first row
   public static final int CELL_W = 18;
   public static final int CELL_H = 20;

   public static int cells() {
      return GRID_COLS * GRID_ROWS;
   }

   /** Cell top-left X (texture space) for a grid column. */
   public static int cellX(int col) {
      return GRID_X0 + col * CELL_W;
   }

   public static int cellY(int row) {
      return GRID_Y0 + row * CELL_H;
   }

   /** Item render X: centre a 16px icon inside the cell. */
   public static int itemX(int col) {
      return cellX(col) + 1;
   }

   public static int itemY(int row) {
      return cellY(row) + 2;
   }

   /** Draw a full storefront background centred at (left, top). */
   public static void blitPanel(GuiGraphics g, ResourceLocation tex, int left, int top) {
      g.blit(tex, left, top, 0, 0, GW, GH, GW, GH);
   }
}
