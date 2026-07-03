package com.fshop.client;

import net.minecraft.client.gui.GuiGraphics;

/** Shared colours and drawing helpers giving every FShop screen the same look. */
public final class FShopTheme {
   private FShopTheme() {
   }

   public static final int BG = 0xF01A1B22;        // near-black translucent panel
   public static final int PANEL = 0xFF23252E;     // panel body
   public static final int PANEL_LIGHT = 0xFF2E313C;
   public static final int BORDER = 0xFF3C4150;
   public static final int HEADER = 0xFF15161C;
   public static final int SLOT = 0xFF14151B;
   public static final int SLOT_HOVER = 0x5582CD47;

   public static final int BUY = 0xFF82CD47;       // green
   public static final int SELL = 0xFF639BFF;      // blue
   public static final int DANGER = 0xFFDF2E38;    // red
   public static final int TEXT = 0xFFE6E6E6;
   public static final int TEXT_DIM = 0xFF9AA0AE;
   public static final int GOLD = 0xFFFFD24A;

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
}
