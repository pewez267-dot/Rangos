package com.fshop.client;

import com.fshop.FShop;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The Spectra ShopGUI+ storefront textures and the pixel geometry used to align
 * interactive elements on top of them. All textures are 256x256; screens render
 * them centred and place slots/buttons at the coordinates defined here.
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
   public static final ResourceLocation ICON_ALERT = gui("icon_red_alert");

   /** Canvas size of every storefront texture. */
   public static final int GW = 256;
   public static final int GH = 256;

   // Wooden "display window" region -> shop content grid (items/offers/shop list)
   public static final int WIN_COLS = 6;
   public static final int WIN_ROWS = 5;
   public static final int WIN_CELL = 18;
   public static final int WIN_X = 95;
   public static final int WIN_Y = 53;

   // Bottom gray grid -> player inventory (used when stocking a shop)
   public static final int INV_COLS = 9;
   public static final int INV_ROWS = 4;
   public static final int INV_X = 44;
   public static final int INV_Y = 167;
   public static final float INV_CELL = 18.78f;

   /** Draw a full storefront background centred at (left, top). */
   public static void blitPanel(GuiGraphics g, ResourceLocation tex, int left, int top) {
      g.blit(tex, left, top, 0, 0, GW, GH, GW, GH);
   }

   // Texture-space cell offsets (add left/top when drawing) --------------
   public static int winX(int col) {
      return WIN_X + col * WIN_CELL;
   }

   public static int winY(int row) {
      return WIN_Y + row * WIN_CELL;
   }

   public static int winCells() {
      return WIN_COLS * WIN_ROWS;
   }

   public static int invX(int col) {
      return INV_X + Math.round(col * INV_CELL);
   }

   public static int invY(int row) {
      return INV_Y + Math.round(row * INV_CELL);
   }

   public static int invCells() {
      return INV_COLS * INV_ROWS;
   }
}
