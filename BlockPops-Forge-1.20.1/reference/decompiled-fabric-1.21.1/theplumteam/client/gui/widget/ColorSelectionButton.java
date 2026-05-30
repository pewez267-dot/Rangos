package com.theplumteam.client.gui.widget;

import com.theplumteam.block.PopBlockColor;
import com.theplumteam.client.gui.FavoriteColorSelectionScreen;
import com.theplumteam.registry.ModItems;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_2487;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_4587;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import org.joml.Quaternionf;

public class ColorSelectionButton extends class_4185 {
   private final PopBlockColor color;
   private final FavoriteColorSelectionScreen parentScreen;
   private final class_1799 boxItem;
   private boolean isSelected = false;
   private float rotationX = 30.0F;
   private float rotationY = 45.0F;
   private float rotationZ = 0.0F;
   private float scale = 1.0F;
   private float offsetX = 0.0F;
   private float offsetY = 0.0F;
   private float offsetZ = 0.0F;

   public ColorSelectionButton(int x, int y, int size, PopBlockColor color, FavoriteColorSelectionScreen parentScreen) {
      super(x, y, size, size, class_2561.method_43473(), button -> {
         if (parentScreen != null) {
            parentScreen.setSelectedColor(color);
         }
      }, field_40754);
      this.color = color;
      this.parentScreen = parentScreen;
      this.boxItem = new class_1799((class_1935)ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(color).get());
      class_2487 blockEntityTag = new class_2487();
      blockEntityTag.method_10556("HideLogo", true);
      blockEntityTag.method_10582("Color", color.name());
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null) {
         blockEntityTag.method_10582("CollectionId", "world_players");
         blockEntityTag.method_10582("FigureId", mc.field_1724.method_5667().toString());
         blockEntityTag.method_10556("IsFigureExtracted", false);
         blockEntityTag.method_10549("FigureOffsetX", -0.53);
         blockEntityTag.method_10549("FigureOffsetY", 0.01);
         blockEntityTag.method_10549("FigureOffsetZ", -0.55);
         blockEntityTag.method_10549("FigureScale", 1.0);
      }

      this.boxItem.method_57379(class_9334.field_49611, class_9279.method_57456(blockEntityTag));
   }

   public void setSelected(boolean selected) {
      this.isSelected = selected;
   }

   public PopBlockColor getColor() {
      return this.color;
   }

   public void setTransforms(float rotX, float rotY, float rotZ, float scale, float offX, float offY, float offZ) {
      this.rotationX = rotX;
      this.rotationY = rotY;
      this.rotationZ = rotZ;
      this.scale = scale;
      this.offsetX = offX;
      this.offsetY = offY;
      this.offsetZ = offZ;
   }

   public void setShowFigure(boolean showFigure) {
      class_2487 blockEntityTag = new class_2487();
      blockEntityTag.method_10556("HideLogo", true);
      blockEntityTag.method_10582("Color", this.color.name());
      if (showFigure) {
         class_310 mc = class_310.method_1551();
         if (mc.field_1724 != null) {
            blockEntityTag.method_10582("CollectionId", "world_players");
            blockEntityTag.method_10582("FigureId", mc.field_1724.method_5667().toString());
            blockEntityTag.method_10556("IsFigureExtracted", false);
            blockEntityTag.method_10549("FigureOffsetX", -0.53);
            blockEntityTag.method_10549("FigureOffsetY", 0.01);
            blockEntityTag.method_10549("FigureOffsetZ", -0.55);
            blockEntityTag.method_10549("FigureScale", 1.0);
         }
      } else {
         blockEntityTag.method_10582("CollectionId", "");
         blockEntityTag.method_10582("FigureId", "");
      }

      this.boxItem.method_57379(class_9334.field_49611, class_9279.method_57456(blockEntityTag));
   }

   public void method_48579(class_332 graphics, int mouseX, int mouseY, float partialTick) {
      class_310 minecraft = class_310.method_1551();
      int backgroundColor;
      int borderColor;
      if (this.isSelected) {
         backgroundColor = -2130706433;
         borderColor = -1;
      } else if (this.method_25367()) {
         backgroundColor = 1627389951;
         borderColor = -2130706433;
      } else {
         backgroundColor = 1090519039;
         borderColor = 1627389951;
      }

      graphics.method_25294(
         this.method_46426(), this.method_46427(), this.method_46426() + this.field_22758, this.method_46427() + this.field_22759, backgroundColor
      );
      if (this.isSelected) {
         graphics.method_25294(this.method_46426(), this.method_46427(), this.method_46426() + this.field_22758, this.method_46427() + 2, borderColor);
         graphics.method_25294(
            this.method_46426(),
            this.method_46427() + this.field_22759 - 2,
            this.method_46426() + this.field_22758,
            this.method_46427() + this.field_22759,
            borderColor
         );
         graphics.method_25294(this.method_46426(), this.method_46427() + 2, this.method_46426() + 2, this.method_46427() + this.field_22759 - 2, borderColor);
         graphics.method_25294(
            this.method_46426() + this.field_22758 - 2,
            this.method_46427() + 2,
            this.method_46426() + this.field_22758,
            this.method_46427() + this.field_22759 - 2,
            borderColor
         );
      } else {
         graphics.method_25294(this.method_46426(), this.method_46427(), this.method_46426() + this.field_22758, this.method_46427() + 1, borderColor);
         graphics.method_25294(
            this.method_46426(),
            this.method_46427() + this.field_22759 - 1,
            this.method_46426() + this.field_22758,
            this.method_46427() + this.field_22759,
            borderColor
         );
         graphics.method_25294(this.method_46426(), this.method_46427() + 1, this.method_46426() + 1, this.method_46427() + this.field_22759 - 1, borderColor);
         graphics.method_25294(
            this.method_46426() + this.field_22758 - 1,
            this.method_46427() + 1,
            this.method_46426() + this.field_22758,
            this.method_46427() + this.field_22759 - 1,
            borderColor
         );
      }

      int itemSize = (int)((float)this.field_22758 * 0.6F);
      int itemX = this.method_46426() + (this.field_22758 - itemSize) / 2;
      int itemY = this.method_46427() + (this.field_22759 - itemSize) / 2;
      class_4587 pose = graphics.method_51448();
      pose.method_22903();
      pose.method_46416((float)itemX + (float)itemSize / 2.0F, (float)itemY + (float)itemSize / 2.0F, 100.0F);
      pose.method_46416(this.offsetX, this.offsetY, this.offsetZ);
      pose.method_22907(
         new Quaternionf()
            .rotationXYZ(
               (float)Math.toRadians((double)this.rotationX), (float)Math.toRadians((double)this.rotationY), (float)Math.toRadians((double)this.rotationZ)
            )
      );
      float finalScale = (float)itemSize / 16.0F * this.scale;
      pose.method_22905(finalScale, finalScale, finalScale);
      pose.method_46416(-8.0F, -8.0F, 0.0F);
      graphics.method_51427(this.boxItem, 0, 0);
      pose.method_22909();
   }
}
