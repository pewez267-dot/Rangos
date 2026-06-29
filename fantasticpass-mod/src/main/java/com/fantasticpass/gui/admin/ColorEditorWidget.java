package com.fantasticpass.gui.admin;

import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.data.PassSerializer;
import com.fantasticpass.gui.widgets.ColorWheelWidget;
import com.fantasticpass.gui.widgets.GradientToggleWidget;
import com.fantasticpass.gui.widgets.HexInputWidget;
import com.fantasticpass.gui.widgets.NametagPreviewWidget;
import com.fantasticpass.gui.widgets.RgbSliderWidget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

public final class ColorEditorWidget {
   private static final int SWATCH = 13;
   private static final int SWATCHES_PER_ROW = 8;
   private final NametagStyle style;
   private String rankText;
   private int previewLevel = 100;
   private String previewName = "Player";
   private ColorEditorWidget.Target target = ColorEditorWidget.Target.SOLID;
   private ColorWheelWidget wheel;
   private RgbSliderWidget rgb;
   private HexInputWidget hex;
   private NametagPreviewWidget preview;
   private Button startButton;
   private Button endButton;
   private int paletteX;
   private int paletteY;
   private final List<Integer> palette = new ArrayList<>();

   public ColorEditorWidget(NametagStyle style, String rankText) {
      this.style = style == null ? new NametagStyle() : style.copy();
      this.rankText = rankText == null ? "" : rankText;

      for (ChatFormatting formatting : ChatFormatting.values()) {
         if (formatting.isColor() && formatting.getColor() != null) {
            this.palette.add(formatting.getColor() & 16777215);
         }
      }
   }

   public NametagStyle getStyle() {
      return this.style;
   }

   public void setRankText(String rankText) {
      this.rankText = rankText == null ? "" : rankText;
      this.refresh();
   }

   public void setPreviewContext(String name, int level) {
      this.previewName = name;
      this.previewLevel = level;
      this.refresh();
   }

   public int totalHeight() {
      return 174;
   }

   public void build(ColorEditorWidget.WidgetSink sink, Font font, int x, int y) {
      this.wheel = sink.accept(new ColorWheelWidget(x, y, 120, 56, this::setActiveColor));
      this.rgb = sink.accept(new RgbSliderWidget(x, y + 60, 120, 32, this::setActiveColor));
      int ftY = y + 96;
      sink.accept(new GradientToggleWidget(x, ftY, 27, 16, Component.literal("B"), this.style.isBold(), v -> {
         this.style.setBold(v);
         this.refresh();
      }));
      sink.accept(new GradientToggleWidget(x + 31, ftY, 27, 16, Component.literal("I"), this.style.isItalic(), v -> {
         this.style.setItalic(v);
         this.refresh();
      }));
      sink.accept(new GradientToggleWidget(x + 62, ftY, 27, 16, Component.literal("U"), this.style.isUnderline(), v -> {
         this.style.setUnderline(v);
         this.refresh();
      }));
      sink.accept(new GradientToggleWidget(x + 93, ftY, 27, 16, Component.literal("S"), this.style.isStrikethrough(), v -> {
         this.style.setStrikethrough(v);
         this.refresh();
      }));
      this.paletteX = x;
      this.paletteY = y + 116;
      int rx = x + 130;
      this.hex = sink.accept(new HexInputWidget(font, rx, y, 100, 16, this::setActiveColor));
      sink.accept(
         new GradientToggleWidget(rx, y + 20, 100, 16, Component.translatable("fantasticpass.gui.gradient"), this.style.isGradient(), this::onGradientToggle)
      );
      this.startButton = sink.accept(
         Button.builder(Component.literal("Start"), b -> this.selectTarget(ColorEditorWidget.Target.GRADIENT_START)).bounds(rx, y + 38, 48, 16).build()
      );
      this.endButton = sink.accept(
         Button.builder(Component.literal("End"), b -> this.selectTarget(ColorEditorWidget.Target.GRADIENT_END)).bounds(rx + 52, y + 38, 48, 16).build()
      );
      sink.accept(Button.builder(Component.translatable("fantasticpass.gui.copy"), b -> this.copyCode()).bounds(rx, y + 56, 100, 16).build());
      this.preview = sink.accept(new NametagPreviewWidget(x, y + 146, 240, 28));
      this.target = this.style.isGradient() ? ColorEditorWidget.Target.GRADIENT_START : ColorEditorWidget.Target.SOLID;
      this.syncControls(this.activeColor());
      this.updateTargetVisibility();
      this.refresh();
   }

   public void renderPalette(GuiGraphics graphics) {
      for (int i = 0; i < this.palette.size(); i++) {
         int col = i % 8;
         int row = i / 8;
         int sx = this.paletteX + col * 13;
         int sy = this.paletteY + row * 13;
         graphics.fill(sx, sy, sx + 13 - 1, sy + 13 - 1, 0xFF000000 | this.palette.get(i));
         graphics.renderOutline(sx, sy, 12, 12, -16777216);
      }
   }

   public boolean handlePaletteClick(double mouseX, double mouseY) {
      for (int i = 0; i < this.palette.size(); i++) {
         int col = i % 8;
         int row = i / 8;
         int sx = this.paletteX + col * 13;
         int sy = this.paletteY + row * 13;
         if (mouseX >= (double)sx && mouseX < (double)(sx + 13 - 1) && mouseY >= (double)sy && mouseY < (double)(sy + 13 - 1)) {
            this.setActiveColor(this.palette.get(i));
            return true;
         }
      }

      return false;
   }

   private int activeColor() {
      return switch (this.target) {
         case SOLID -> this.style.getColor();
         case GRADIENT_START -> this.style.getGradientStart();
         case GRADIENT_END -> this.style.getGradientEnd();
      };
   }

   private void setActiveColor(int color) {
      switch (this.target) {
         case SOLID:
            this.style.setColor(color);
            break;
         case GRADIENT_START:
            this.style.setGradientStart(color);
            break;
         case GRADIENT_END:
            this.style.setGradientEnd(color);
      }

      this.syncControls(color);
      this.refresh();
   }

   private void selectTarget(ColorEditorWidget.Target newTarget) {
      this.target = newTarget;
      this.syncControls(this.activeColor());
   }

   private void onGradientToggle(boolean on) {
      this.style.setGradient(on);
      this.target = on ? ColorEditorWidget.Target.GRADIENT_START : ColorEditorWidget.Target.SOLID;
      this.syncControls(this.activeColor());
      this.updateTargetVisibility();
      this.refresh();
   }

   private void updateTargetVisibility() {
      boolean gradient = this.style.isGradient();
      if (this.startButton != null) {
         this.startButton.visible = gradient;
         this.startButton.active = gradient;
      }

      if (this.endButton != null) {
         this.endButton.visible = gradient;
         this.endButton.active = gradient;
      }
   }

   private void syncControls(int color) {
      if (this.wheel != null) {
         this.wheel.setColor(color);
      }

      if (this.rgb != null) {
         this.rgb.setColor(color);
      }

      if (this.hex != null) {
         this.hex.setColorSilently(color);
      }
   }

   private void refresh() {
      if (this.preview != null) {
         this.preview.setStyle(this.style);
         this.preview.setRankText(this.rankText);
         this.preview.setLevel(this.previewLevel);
         this.preview.setPlayerName(this.previewName);
      }
   }

   private void copyCode() {
      Minecraft.getInstance().keyboardHandler.setClipboard(PassSerializer.toFormatCodeString(this.style, this.rankText));
   }

   public static enum Target {
      SOLID,
      GRADIENT_START,
      GRADIENT_END;
   }

   public interface WidgetSink {
      <T extends GuiEventListener & Renderable & NarratableEntry> T accept(T var1);
   }
}
