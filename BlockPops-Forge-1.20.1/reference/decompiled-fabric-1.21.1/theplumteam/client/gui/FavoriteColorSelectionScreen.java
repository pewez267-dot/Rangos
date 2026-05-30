package com.theplumteam.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.block.PopBlockColor;
import com.theplumteam.client.config.ClientConfig;
import com.theplumteam.client.gui.util.GuiScaleManager;
import com.theplumteam.client.gui.widget.ColorSelectionButton;
import com.theplumteam.network.SetFavoriteColorPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import net.minecraft.class_293.class_5596;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FavoriteColorSelectionScreen extends class_437 {
   private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteColorSelectionScreen.class);
   @Nullable
   private PopBlockColor selectedColor = null;
   private class_4185 doneButton;
   private class_4185 toggleFigureButton;
   private boolean showFigureInBox = true;
   private final List<ColorSelectionButton> colorButtons = new ArrayList<>();
   private int panelX;
   private int panelY;
   private int panelWidth;
   private int panelHeight;
   private static final int MIN_PANEL_WIDTH = 400;
   private static final int MAX_PANEL_WIDTH = 600;
   private static final int MIN_PANEL_HEIGHT = 450;
   private float rotationX = 342.3F;
   private float rotationY = 335.9F;
   private float rotationZ = 0.0F;
   private float scale = 1.2F;
   private float offsetX = 198.0F;
   private float offsetY = -134.0F;
   private float offsetZ = 0.0F;
   private float containerScale = 1.2F;
   private boolean guiScaleForced = false;
   private boolean isClosing = false;

   public FavoriteColorSelectionScreen() {
      super(class_2561.method_43470("Choose Your Favorite Color"));
      BlockPopsMod.logDebug("FavoriteColorSelectionScreen created");
   }

   protected void method_25426() {
      if (this.isClosing) {
         super.method_25426();
      } else if (!this.guiScaleForced && GuiScaleManager.setMenuGuiScale(GuiScaleManager.getOptimalMenuScale(), true)) {
         this.guiScaleForced = true;
      } else {
         super.method_25426();
         this.method_37067();
         this.colorButtons.clear();
         this.calculatePanelDimensions();
         int scaledPadding = 20;
         int scaledSpacing = 10;
         int scaledComponentHeight = 24;
         int titleY = this.panelY + scaledPadding;
         int buttonSize = (int)(60.0F * this.containerScale);
         int gridCols = 4;
         int gridRows = 4;
         int gridWidth = gridCols * buttonSize + (gridCols - 1) * scaledSpacing;
         int gridHeight = gridRows * buttonSize + (gridRows - 1) * scaledSpacing;
         int gridStartX = this.panelX + (this.panelWidth - gridWidth) / 2;
         int descriptionHeight = 9 * 3;
         int gridStartY = titleY + descriptionHeight + scaledPadding;
         PopBlockColor[] colors = PopBlockColor.values();

         for (int i = 0; i < colors.length; i++) {
            PopBlockColor color = colors[i];
            int row = i / gridCols;
            int col = i % gridCols;
            int x = gridStartX + col * (buttonSize + scaledSpacing);
            int y = gridStartY + row * (buttonSize + scaledSpacing);
            ColorSelectionButton colorButton = new ColorSelectionButton(x, y, buttonSize, color, this);
            this.method_37063(colorButton);
            this.colorButtons.add(colorButton);
         }

         int buttonWidth = 200;
         int toggleButtonWidth = 120;
         int buttonSpacing = 10;
         int totalButtonWidth = buttonWidth + toggleButtonWidth + buttonSpacing;
         int buttonsStartX = this.panelX + (this.panelWidth - totalButtonWidth) / 2;
         int doneButtonY = this.panelY + this.panelHeight - scaledPadding - scaledComponentHeight;
         this.toggleFigureButton = class_4185.method_46430(class_2561.method_43470(this.showFigureInBox ? "Figure: ON" : "Figure: OFF"), button -> {
            this.showFigureInBox = !this.showFigureInBox;
            button.method_25355(class_2561.method_43470(this.showFigureInBox ? "Figure: ON" : "Figure: OFF"));
            this.updateFigureVisibility();
         }).method_46434(buttonsStartX, doneButtonY, toggleButtonWidth, scaledComponentHeight).method_46431();
         this.method_37063(this.toggleFigureButton);
         int doneButtonX = buttonsStartX + toggleButtonWidth + buttonSpacing;
         this.doneButton = class_4185.method_46430(class_2561.method_43470("Done"), button -> {
            if (this.selectedColor != null) {
               BlockPopsMod.logDebug("Player confirmed favorite color choice: {}", this.selectedColor.method_15434());
               SetFavoriteColorPacket packet = new SetFavoriteColorPacket(this.selectedColor.method_15434());
               packet.sendToServer();
               this.method_25419();
            }
         }).method_46434(doneButtonX, doneButtonY, buttonWidth, scaledComponentHeight).method_46431();
         this.doneButton.field_22763 = false;
         this.method_37063(this.doneButton);
         this.updateButtonTransforms();
      }
   }

   private void updateButtonTransforms() {
      for (ColorSelectionButton button : this.colorButtons) {
         button.setTransforms(this.rotationX, this.rotationY, this.rotationZ, this.scale, this.offsetX, this.offsetY, this.offsetZ);
      }
   }

   private void updateFigureVisibility() {
      for (ColorSelectionButton button : this.colorButtons) {
         button.setShowFigure(this.showFigureInBox);
      }
   }

   private void calculatePanelDimensions() {
      int desiredWidth = (int)((float)this.field_22789 * 0.5F);
      int desiredHeight = (int)((float)this.field_22790 * 0.7F);
      this.panelWidth = class_3532.method_15340(desiredWidth, 400, Math.min(600, this.field_22789 - 60));
      this.panelHeight = class_3532.method_15340(desiredHeight, 450, this.field_22790 - 60);
      this.panelX = (this.field_22789 - this.panelWidth) / 2;
      this.panelY = (this.field_22790 - this.panelHeight) / 2;
   }

   public void setSelectedColor(PopBlockColor color) {
      this.selectedColor = color;
      this.doneButton.field_22763 = true;

      for (ColorSelectionButton button : this.colorButtons) {
         button.setSelected(button.getColor() == color);
      }

      LOGGER.debug("Selected color: {}", color.method_15434());
   }

   public void method_25419() {
      this.isClosing = true;
      this.restoreGuiScaleIfNeeded();
      super.method_25419();
   }

   public void method_25432() {
      this.isClosing = true;
      this.restoreGuiScaleIfNeeded();
      super.method_25432();
   }

   private void restoreGuiScaleIfNeeded() {
      if (this.guiScaleForced) {
         this.guiScaleForced = false;
         GuiScaleManager.restoreOriginalGuiScale();
         BlockPopsMod.logDebug("Restored original GUI scale");
      }
   }

   public void method_25394(@NotNull class_332 graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackgroundEffects(graphics, partialTick);
      this.renderPanel(graphics);
      super.method_25394(graphics, mouseX, mouseY, partialTick);
      this.renderTitleAndDescription(graphics);
   }

   private void renderBackgroundEffects(class_332 graphics, float partialTick) {
      int bgColor = -16777216;
      graphics.method_25294(0, 0, this.field_22789, this.field_22790, bgColor);
      this.renderStarPattern(graphics, partialTick);
   }

   private void renderStarPattern(class_332 graphics, float partialTick) {
      double pixelsPerSecond = 5.0;
      int tileSize = StarPatternCache.getTileSize();
      int tickCount = this.field_22787 != null ? this.field_22787.field_1705.method_1738() : 0;
      double smoothTime = (double)((float)tickCount + partialTick) / 20.0;
      double offsetX = smoothTime * pixelsPerSecond % (double)tileSize;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      ClientConfig config = ClientConfig.getInstance();
      RenderSystem.setShaderColor(config.starColorR, config.starColorG, config.starColorB, config.starOpacity);
      class_2960 cacheTexture = StarPatternCache.getTextureLocation();
      int cacheWidth = StarPatternCache.getTextureWidth();
      int cacheHeight = StarPatternCache.getTextureHeight();
      float u0 = (float)offsetX / (float)cacheWidth;
      float v0 = 0.0F;
      float u1 = u0 + (float)this.field_22789 / (float)cacheWidth;
      float v1 = (float)this.field_22790 / (float)cacheHeight;
      class_4587 pose = graphics.method_51448();
      pose.method_22903();
      RenderSystem.setShaderTexture(0, cacheTexture);
      RenderSystem.setShader(class_757::method_34542);
      class_287 bufferBuilder = class_289.method_1348().method_60827(class_5596.field_27382, class_290.field_1585);
      bufferBuilder.method_22918(pose.method_23760().method_23761(), 0.0F, (float)this.field_22790, 0.0F).method_22913(u0, v1);
      bufferBuilder.method_22918(pose.method_23760().method_23761(), (float)this.field_22789, (float)this.field_22790, 0.0F).method_22913(u1, v1);
      bufferBuilder.method_22918(pose.method_23760().method_23761(), (float)this.field_22789, 0.0F, 0.0F).method_22913(u1, v0);
      bufferBuilder.method_22918(pose.method_23760().method_23761(), 0.0F, 0.0F, 0.0F).method_22913(u0, v0);
      class_286.method_43433(bufferBuilder.method_60800());
      pose.method_22909();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderPanel(class_332 graphics) {
      ClientConfig config = ClientConfig.getInstance();
      int alpha = (int)(config.panelOpacity * 255.0F);
      int panelBgColor = alpha << 24 | 0;
      graphics.method_25294(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, panelBgColor);
      graphics.method_25294(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 1, 1627389951);
      graphics.method_25294(this.panelX, this.panelY + this.panelHeight - 1, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 1627389951);
      graphics.method_25294(this.panelX, this.panelY + 1, this.panelX + 1, this.panelY + this.panelHeight - 1, 1627389951);
      graphics.method_25294(this.panelX + this.panelWidth - 1, this.panelY + 1, this.panelX + this.panelWidth, this.panelY + this.panelHeight - 1, 1627389951);
   }

   private void renderTitleAndDescription(class_332 graphics) {
      int scaledPadding = 20;
      int titleY = this.panelY + scaledPadding;
      String title = "Choose Your Favorite Color";
      int titleWidth = this.field_22793.method_1727(title);
      int titleX = this.panelX + (this.panelWidth - titleWidth) / 2;
      graphics.method_51433(this.field_22793, title, titleX, titleY, 16777215, false);
      titleY += 9 + 8;
      String desc1 = "This color will be used for your figure box";
      int desc1Width = this.field_22793.method_1727(desc1);
      int desc1X = this.panelX + (this.panelWidth - desc1Width) / 2;
      graphics.method_51433(this.field_22793, desc1, desc1X, titleY, 11184810, false);
      titleY += 9 + 2;
      String desc2 = "in the World Players collection.";
      int desc2Width = this.field_22793.method_1727(desc2);
      int desc2X = this.panelX + (this.panelWidth - desc2Width) / 2;
      graphics.method_51433(this.field_22793, desc2, desc2X, titleY, 11184810, false);
   }

   public boolean method_25421() {
      return false;
   }

   public boolean method_25422() {
      return false;
   }

   public void method_57734(float partialTick) {
   }
}
