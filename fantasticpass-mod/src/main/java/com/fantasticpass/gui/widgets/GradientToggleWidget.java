package com.fantasticpass.gui.widgets;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class GradientToggleWidget extends AbstractWidget {
   private boolean state;
   private final Consumer<Boolean> onToggle;

   public GradientToggleWidget(int x, int y, int width, int height, Component label, boolean initialState, Consumer<Boolean> onToggle) {
      super(x, y, width, height, label);
      this.state = initialState;
      this.onToggle = onToggle;
   }

   public boolean getState() {
      return this.state;
   }

   public void setStateSilently(boolean state) {
      this.state = state;
   }

   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      boolean hovered = mouseX >= this.getX() && mouseX < this.getX() + this.width && mouseY >= this.getY() && mouseY < this.getY() + this.height;
      int background = this.state ? 670276 : 1315868;
      graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF000000 | background);
      int border = this.state ? 16766720 : (hovered ? '\ue5ff' : 3355455);
      graphics.renderOutline(this.getX(), this.getY(), this.width, this.height, 0xFF000000 | border);
      int textColor = this.state ? -1 : -5592406;
      int textWidth = Minecraft.getInstance().font.width(this.getMessage());
      graphics.drawString(
         Minecraft.getInstance().font, this.getMessage(), this.getX() + (this.width - textWidth) / 2, this.getY() + (this.height - 8) / 2, textColor, false
      );
   }

   public void onClick(double mouseX, double mouseY) {
      this.state = !this.state;
      if (this.onToggle != null) {
         this.onToggle.accept(this.state);
      }
   }

   protected void updateWidgetNarration(NarrationElementOutput output) {
      output.add(NarratedElementType.TITLE, this.getMessage());
   }
}
