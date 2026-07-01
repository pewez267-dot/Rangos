package com.fantasticpass.gui.widgets;

import com.fantasticpass.gui.castle.CastleScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Small "eye" control shown next to the music button in the top-right corner of
 * the pass UI. Toggling it hides the pass artwork/foreground so the background
 * wallpaper is revealed in full, and toggling again restores the pass.
 */
public class PeekButton extends AbstractButton {
   public PeekButton(int x, int y, int size) {
      super(x, y, size, size, Component.empty());
      this.updateTooltip();
   }

   @Override
   public void onPress() {
      CastleScreen.togglePeek();
      this.updateTooltip();
   }

   private void updateTooltip() {
      this.setTooltip(Tooltip.create(Component.translatable(
         CastleScreen.isPeek() ? "fantasticpass.gui.peek_show" : "fantasticpass.gui.peek_hide")));
   }

   @Override
   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      int x = this.getX();
      int y = this.getY();
      int w = this.width;
      int h = this.height;
      boolean hovered = this.isHoveredOrFocused();
      boolean peeking = CastleScreen.isPeek();

      g.fill(x, y, x + w, y + h, hovered ? 0xB0000000 : 0x80000000);
      g.renderOutline(x, y, w, h, peeking ? 0x886FE0A0 : 0x66FFFFFF);

      int cx = x + w / 2;
      int cy = y + h / 2;
      int color = peeking ? 0xFF6FE0A0 : 0xFFFFFFFF;

      // Eye: a small almond "lens" (rows of decreasing width) + a pupil.
      int reach = Math.max(2, w / 2 - 2);
      for (int dy = -2; dy <= 2; dy++) {
         int ww = Math.max(0, reach - Math.abs(dy) * (reach / 3 + 1));
         if (ww > 0) {
            g.fill(cx - ww, cy + dy, cx + ww, cy + dy + 1, color);
         }
      }
      // Pupil (dark) so the white lens reads as an eye.
      g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFF10131A);

      // When NOT peeking the wallpaper is hidden: show an "off" bar cue.
      if (!peeking) {
         g.fill(x + 3, cy - 1, x + w - 3, cy, 0x99FF5555);
      }
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }
}
