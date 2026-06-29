package com.fantasticpass.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class ThemedButton extends AbstractButton {
   private final ThemedButton.OnClick onClick;
   private int accentRgb;

   public ThemedButton(int x, int y, int w, int h, Component message, int accentRgb, ThemedButton.OnClick onClick) {
      super(x, y, w, h, message);
      this.accentRgb = accentRgb;
      this.onClick = onClick;
   }

   public void setAccent(int rgb) {
      this.accentRgb = rgb;
   }

   public void onPress() {
      if (this.onClick != null) {
         this.onClick.onClick(this);
      }
   }

   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      int x = this.getX();
      int y = this.getY();
      int w = this.getWidth();
      int h = this.getHeight();
      boolean hovered = this.isHoveredOrFocused() && this.active;
      int fill = this.active ? (hovered ? 1973802 : 1315868) : 657935;
      g.fill(x, y, x + w, y + h, 0xFF000000 | fill);
      if (hovered) {
         g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 570425344 | this.accentRgb & 16777215);
         g.fill(x + 1, y + 1, x + w - 1, y + 2, 1426063360 | this.accentRgb & 16777215);
      }

      int borderAlpha = this.active ? (hovered ? 255 : 204) : 102;
      int border = borderAlpha << 24 | (this.active ? this.accentRgb & 16777215 : 3355455);
      g.renderOutline(x, y, w, h, border);
      int textColor = this.active ? 16777215 : 3816002;
      int tx = x + w / 2;
      int ty = y + (h - 8) / 2;
      g.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), tx, ty, 0xFF000000 | textColor);
   }

   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }

   public interface OnClick {
      void onClick(ThemedButton var1);
   }
}
