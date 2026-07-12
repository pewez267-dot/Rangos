package com.fantasticpass.gui;

import com.fantasticpass.nametag.NametagBuilder;
import net.minecraft.client.gui.GuiGraphics;

public final class GuiTheme {
   public static final int BACKGROUND = 657935;
   public static final int PANEL = 1315868;
   public static final int PANEL_LIGHT = 1973802;
   public static final int BORDER = 3355455;
   public static final int ACCENT_CYAN = 58879;
   public static final int ACCENT_CYAN_DIM = 670276;
   public static final int ACCENT_GOLD = 16766720;
   public static final int ACCENT_GOLD_DIM = 4865280;
   public static final int TEXT_PRIMARY = 16777215;
   public static final int TEXT_SECONDARY = 11184810;
   public static final int SILVER = 12632264;
   public static final int LOCKED = 3816002;

   private GuiTheme() {
   }

   public static void drawBackground(GuiGraphics graphics, int width, int height) {
      graphics.fill(0, 0, width, height, -16119281);
      graphics.fillGradient(0, 0, width, height, 285870607, 1711280666);
   }

   public static void drawPanel(GuiGraphics graphics, int x, int y, int w, int h) {
      graphics.fill(x, y, x + w, y + h, -15461348);
      graphics.renderOutline(x, y, w, h, -13421761);
   }

   public static void drawAccentPanel(GuiGraphics graphics, int x, int y, int w, int h, int accentRgb) {
      graphics.fill(x, y, x + w, y + h, -14803414);
      graphics.renderOutline(x, y, w, h, 0xFF000000 | accentRgb);
   }

   public static int cyanPulse() {
      double phase = (double)(System.currentTimeMillis() % 1400L) / 1400.0;
      float t = (float)((Math.sin(phase * Math.PI * 2.0) + 1.0) / 2.0);
      return NametagBuilder.lerpColor(670276, 58879, t);
   }

   public static int goldPulse() {
      double phase = (double)(System.currentTimeMillis() % 1600L) / 1600.0;
      float t = (float)((Math.sin(phase * Math.PI * 2.0) + 1.0) / 2.0);
      return NametagBuilder.lerpColor(4865280, 16766720, t);
   }
}
