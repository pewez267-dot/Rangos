package com.theplumteam.client.gui.widget;

import com.theplumteam.client.gui.CollectionSelectionScreen;
import com.theplumteam.figure.FigureCollection;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4280;

public class CollectionListWidget extends class_4280<CollectionEntry> {
   private final CollectionSelectionScreen parentScreen;
   private int xPosition;

   public CollectionListWidget(CollectionSelectionScreen parentScreen, class_310 mc, int width, int height, int y, int entryHeight) {
      super(mc, width, height, y, entryHeight);
      this.parentScreen = parentScreen;
      this.xPosition = 0;
   }

   public void setXPosition(int x) {
      this.xPosition = x;
      this.method_46421(x);
   }

   public void addCollectionEntry(FigureCollection collection) {
      this.method_25321(new CollectionEntry(this, collection));
   }

   public void clearAllEntries() {
      this.method_25339();
   }

   public void onCollectionSelected(CollectionEntry entry) {
      this.parentScreen.onCollectionSelected(entry);
   }

   public void selectByCollectionId(String collectionId) {
      if (collectionId != null && !collectionId.isEmpty()) {
         for (CollectionEntry entry : this.method_25396()) {
            if (entry.getCollection().getId().equals(collectionId)) {
               this.method_25313(entry);
               this.method_25328(entry);
               break;
            }
         }
      }
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
      this.method_25307(this.method_25341() - scrollY * 15.0);
      return true;
   }

   public boolean method_25403(double mouseX, double mouseY, int button, double dragX, double dragY) {
      return super.method_25403(mouseX, mouseY, button, dragX, dragY);
   }

   protected void method_44398(class_332 graphics, int top, int width, int height, int outerColor, int innerColor) {
   }
}
