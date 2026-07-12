package com.fantasticpass.gui.widgets;

import com.fantasticpass.client.PassPlaylistManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Small, unobtrusive speaker control shown in the top-right corner of the pass
 * UI. Clicking it cycles the playlist volume (100% &rarr; 66% &rarr; 33% &rarr;
 * muted &rarr; ...). It renders a music-note glyph over a tiny 3-cell volume
 * meter, turning red when muted.
 */
public class MusicButton extends AbstractButton {
   public MusicButton(int x, int y, int size) {
      super(x, y, size, size, Component.empty());
      this.updateTooltip();
   }

   @Override
   public void onPress() {
      PassPlaylistManager.cycleVolume();
      this.updateTooltip();
   }

   private void updateTooltip() {
      Component tip = PassPlaylistManager.isMuted()
         ? Component.translatable("fantasticpass.gui.music_muted")
         : Component.translatable("fantasticpass.gui.music_volume", PassPlaylistManager.volumePercent());
      this.setTooltip(Tooltip.create(tip));
   }

   @Override
   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      int x = this.getX();
      int y = this.getY();
      int w = this.width;
      int h = this.height;
      boolean hovered = this.isHoveredOrFocused();
      boolean muted = PassPlaylistManager.isMuted();

      // Subtle panel so it stays readable over the artwork without being loud.
      g.fill(x, y, x + w, y + h, hovered ? 0xB0000000 : 0x80000000);
      g.renderOutline(x, y, w, h, muted ? 0x88FF5555 : 0x66FFFFFF);

      // Music note glyph, centred in the upper portion.
      var font = Minecraft.getInstance().font;
      int noteColor = muted ? 0xFFFF6060 : 0xFFFFFFFF;
      g.drawCenteredString(font, "\u266b", x + w / 2, y + 2, noteColor);

      // Tiny 3-cell volume meter along the bottom edge.
      int bars = PassPlaylistManager.volumeBars();
      int cells = 3;
      int gap = 1;
      int meterW = w - 6;
      int cellW = Math.max(2, (meterW - gap * (cells - 1)) / cells);
      int mx = x + 3;
      int my = y + h - 5;
      for (int i = 0; i < cells; i++) {
         int cx = mx + i * (cellW + gap);
         boolean on = i < bars;
         g.fill(cx, my, cx + cellW, my + 3, on ? 0xFF6FE0A0 : 0x55FFFFFF);
      }

      // A clear diagonal "off" cue when muted.
      if (muted) {
         g.fill(x + 3, y + h / 2 - 1, x + w - 3, y + h / 2, 0xFFFF5555);
      }
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }
}
