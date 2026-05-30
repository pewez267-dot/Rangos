package com.theplumteam.client.gui.widget;

import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_4185.class_4241;

public class TabButton extends class_4185 {
   private boolean selected;
   private static final int SELECTED_BG = -16777216;
   private static final int UNSELECTED_BG = -16777216;
   private static final int SELECTED_OUTLINE = -1;
   private static final int UNSELECTED_OUTLINE = 1090519039;
   private static final int SELECTED_TEXT = -1;
   private static final int UNSELECTED_TEXT = -6710887;

   public TabButton(int x, int y, int width, int height, class_2561 label, boolean selected, class_4241 onPress) {
      super(x, y, width, height, label, onPress, field_40754);
      this.selected = selected;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   public boolean isSelected() {
      return this.selected;
   }

   public void method_48579(class_332 graphics, int mouseX, int mouseY, float partialTicks) {
      int bgColor = this.selected ? -16777216 : -16777216;
      int outlineColor = this.selected ? -1 : 1090519039;
      int textColor = this.selected ? -1 : -6710887;
      if (!this.selected && this.method_49606()) {
         bgColor = -16777216;
         textColor = -3355444;
      }

      graphics.method_25294(this.method_46426(), this.method_46427(), this.method_46426() + this.field_22758, this.method_46427() + this.field_22759, bgColor);
      graphics.method_25294(this.method_46426(), this.method_46427(), this.method_46426() + this.field_22758, this.method_46427() + 1, outlineColor);
      graphics.method_25294(this.method_46426(), this.method_46427(), this.method_46426() + 1, this.method_46427() + this.field_22759, outlineColor);
      graphics.method_25294(
         this.method_46426() + this.field_22758 - 1,
         this.method_46427(),
         this.method_46426() + this.field_22758,
         this.method_46427() + this.field_22759,
         outlineColor
      );
      if (!this.selected) {
         graphics.method_25294(
            this.method_46426(),
            this.method_46427() + this.field_22759 - 1,
            this.method_46426() + this.field_22758,
            this.method_46427() + this.field_22759,
            outlineColor
         );
      }

      graphics.method_27534(
         class_310.method_1551().field_1772,
         this.method_25369(),
         this.method_46426() + this.field_22758 / 2,
         this.method_46427() + (this.field_22759 - 8) / 2,
         textColor
      );
   }

   protected boolean method_25351(int button) {
      return button == 0;
   }
}
