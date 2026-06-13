package com.theplumteam.client.gui.widget;

import com.theplumteam.figure.FigureCollection;
import com.theplumteam.figure.FigureDefinition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4280;
import org.jetbrains.annotations.Nullable;

public class FigureListWidget extends class_4280<FigureEntry> {
   @Nullable
   private FigureCollection currentCollection;
   private int xPosition;
   private float modelScale = 1.0F;
   private float xRotation = 0.0F;
   private float yRotation = 150.0F;
   private float zRotation = 0.0F;
   private float xOffset = 0.0F;
   private float yOffset = 15.0F;
   private float zOffset = 0.0F;

   public FigureListWidget(class_310 mc, int width, int height, int y, int entryHeight) {
      super(mc, width, height, y, entryHeight);
      this.xPosition = 0;
   }

   public void setXPosition(int x) {
      this.xPosition = x;
      this.method_46421(x);
   }

   public void setCollection(@Nullable FigureCollection collection) {
      this.currentCollection = collection;
      this.method_25339();
      if (collection != null) {
         List<FigureDefinition> figures = collection.getFigures();
         List<FigureDefinition> currentRow = new ArrayList<>();

         for (int i = 0; i < figures.size(); i++) {
            currentRow.add(figures.get(i));
            if (currentRow.size() == 4 || i == figures.size() - 1) {
               FigureEntry entry = new FigureEntry(currentRow, collection.getId());
               entry.setConfiguration(this.modelScale, this.xRotation, this.yRotation, this.zRotation, this.xOffset, this.yOffset, this.zOffset);
               this.method_25321(entry);
               currentRow = new ArrayList<>();
            }
         }
      }

      this.method_25307(0.0);
   }

   public void updateConfiguration(float modelScale, float xRotation, float yRotation, float zRotation, float xOffset, float yOffset, float zOffset) {
      this.modelScale = modelScale;
      this.xRotation = xRotation;
      this.yRotation = yRotation;
      this.zRotation = zRotation;
      this.xOffset = xOffset;
      this.yOffset = yOffset;
      this.zOffset = zOffset;

      for (FigureEntry entry : this.method_25396()) {
         entry.setConfiguration(modelScale, xRotation, yRotation, zRotation, xOffset, yOffset, zOffset);
      }
   }

   public float getModelScale() {
      return this.modelScale;
   }

   public float getXRotation() {
      return this.xRotation;
   }

   public float getYRotation() {
      return this.yRotation;
   }

   public float getZRotation() {
      return this.zRotation;
   }

   public float getXOffset() {
      return this.xOffset;
   }

   public float getYOffset() {
      return this.yOffset;
   }

   public float getZOffset() {
      return this.zOffset;
   }

   @Nullable
   public FigureCollection getCurrentCollection() {
      return this.currentCollection;
   }

   public void method_48579(class_332 graphics, int mouseX, int mouseY, float partialTick) {
      super.method_48579(graphics, mouseX, mouseY, partialTick);
   }

   public int method_25322() {
      return this.field_22758 - 8;
   }

   public int method_46426() {
      return this.xPosition;
   }

   public int method_25342() {
      return this.xPosition + 4;
   }

   protected int method_25329() {
      return this.method_55442() - 6;
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      return super.method_25402(mouseX, mouseY, button);
   }

   public boolean method_25401(double mouseX, double mouseY, double scrollX, double scrollY) {
      this.method_25307(this.method_25341() - scrollY * 20.0);
      return true;
   }

   public boolean method_25403(double mouseX, double mouseY, int button, double dragX, double dragY) {
      return super.method_25403(mouseX, mouseY, button, dragX, dragY);
   }
}
