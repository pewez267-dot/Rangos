package com.fshop.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Shared colours and drawing helpers giving every FShop screen the same look. */
public final class FShopTheme {
   private FShopTheme() {
   }

   // Palette sampled directly from the storefront textures so every extra
   // panel (bottom nav, price dialog) looks like part of the same shop
   // instead of a foreign dark/blue overlay.
   public static final int INV_LIGHT = 0xFFC6C6C6;   // outer light-gray frame (measured)
   public static final int INV_MID = 0xFF8B8B8B;     // slot/cell fill (measured)
   public static final int INV_DARK = 0xFF373737;    // recessed slot border (measured)
   public static final int WOOD_DARK = 0xFF603420;   // dark plank (measured)
   public static final int WOOD_LIGHT = 0xFF85543A;  // light plank (measured)
   public static final int PLAQUE = 0xFFE0BB92;      // "ARE YOU SURE" sign fill (measured)

   public static final int PANEL = INV_MID;
   public static final int PANEL_LIGHT = 0xFF9A9A9A;
   public static final int BORDER = INV_DARK;
   public static final int HEADER = INV_LIGHT;
   public static final int SLOT = 0xFF767676;
   public static final int SLOT_HOVER = 0x6682CD47;

   public static final int BUY = 0xFF4E8F2B;       // muted green (buy/confirm)
   public static final int SELL = WOOD_LIGHT;      // wood brown (navigation)
   public static final int DANGER = 0xFFA23B32;    // muted red (cancel/remove)

   // Text drawn over the light-gray inventory-style panel (bottom area, dialogs).
   public static final int TEXT = 0xFF2B2B2B;
   public static final int TEXT_DIM = 0xFF5A5A5A;
   public static final int GOLD = 0xFF7A4A17;

   // Text drawn directly over the dark wooden storefront (titles, item slots).
   public static final int WOOD_TEXT = 0xFFF5E6C8;
   public static final int WOOD_TEXT_DIM = 0xFFC9B08A;
   public static final int WOOD_GOLD = 0xFFFFD24A;

   /** Filled rectangle with a 1px border. */
   public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
      g.fill(x, y, x + w, y + h, fill);
      g.fill(x, y, x + w, y + 1, border);
      g.fill(x, y + h - 1, x + w, y + h, border);
      g.fill(x, y, x + 1, y + h, border);
      g.fill(x + w - 1, y, x + w, y + h, border);
   }

   /** A clickable button box; returns true if the point is inside. */
   public static boolean button(GuiGraphics g, int x, int y, int w, int h, int accent, boolean hovered) {
      int fill = hovered ? PANEL_LIGHT : PANEL;
      panel(g, x, y, w, h, fill, accent);
      if (hovered) {
         g.fill(x + 1, y + 1, x + w - 1, y + h - 1, (accent & 0x00FFFFFF) | 0x33000000);
      }
      return true;
   }

   public static boolean inside(double mx, double my, int x, int y, int w, int h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   /**
    * Draws a small page-number chip ("3/12") centred at (cx,cy): a soft rounded
    * dark pill with warm light text, so it reads cleanly over the light-gray
    * inventory grid instead of looking like a stray black number.
    */
   public static void drawPageBadge(GuiGraphics g, Font font, int cx, int cy, String text) {
      int w = font.width(text) + 10;
      int h = 11;
      int x = cx - w / 2;
      int y = cy - h / 2;
      g.fill(x + 1, y, x + w - 1, y + h, 0xB2241C14);
      g.fill(x, y + 1, x + w, y + h - 1, 0xB2241C14);
      g.drawCenteredString(font, text, cx, y + 2, 0xFFEBD9AE);
   }

   /**
    * Draws a count/amount at the bottom-right of a 16px item slot, shrinking the
    * text when it is long so big numbers never spill outside the slot.
    */
   public static void drawCount(GuiGraphics g, Font font, int itemX, int itemY, String s) {
      int w = font.width(s);
      float scale = w <= 15 ? 1.0F : 15.0F / w;
      g.pose().pushPose();
      g.pose().translate(0.0F, 0.0F, 200.0F);
      g.pose().scale(scale, scale, 1.0F);
      int lx = (int) ((itemX + 17) / scale) - w;
      int ly = (int) ((itemY + 17) / scale) - font.lineHeight;
      g.drawString(font, s, lx, ly, 0xFFFFFFFF, true);
      g.pose().popPose();
   }
}
