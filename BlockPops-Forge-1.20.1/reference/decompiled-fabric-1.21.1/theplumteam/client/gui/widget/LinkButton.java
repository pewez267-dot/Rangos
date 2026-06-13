package com.theplumteam.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.theplumteam.BlockPopsMod;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_7919;

public class LinkButton extends class_4185 {
   private static final class_2960 WIDGETS_LOCATION = class_2960.method_60656("textures/gui/widgets.png");
   private final class_2960 texture;
   private final int textureWidth;
   private final int textureHeight;

   public LinkButton(int x, int y, int width, int height, class_2960 texture, String url, class_2561 tooltip) {
      super(x, y, width, height, class_2561.method_43473(), button -> {
         if (url != null) {
            openLink(url);
         }
      }, field_40754);
      this.texture = texture;
      this.textureWidth = 256;
      this.textureHeight = 256;
      this.method_47400(class_7919.method_47407(tooltip));
   }

   public void method_48579(class_332 graphics, int mouseX, int mouseY, float partialTicks) {
      int bgColor = this.method_25367() ? -2130706433 : 1627389951;
      graphics.method_25294(
         this.method_46426(), this.method_46427(), this.method_46426() + this.method_25368(), this.method_46427() + this.method_25364(), bgColor
      );
      int borderColor = -2130706433;
      graphics.method_25294(this.method_46426(), this.method_46427(), this.method_46426() + this.method_25368(), this.method_46427() + 1, borderColor);
      graphics.method_25294(
         this.method_46426(),
         this.method_46427() + this.method_25364() - 1,
         this.method_46426() + this.method_25368(),
         this.method_46427() + this.method_25364(),
         borderColor
      );
      graphics.method_25294(this.method_46426(), this.method_46427() + 1, this.method_46426() + 1, this.method_46427() + this.method_25364() - 1, borderColor);
      graphics.method_25294(
         this.method_46426() + this.method_25368() - 1,
         this.method_46427() + 1,
         this.method_46426() + this.method_25368(),
         this.method_46427() + this.method_25364() - 1,
         borderColor
      );
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.field_22765);
      int padding = 2;
      graphics.method_25293(
         this.texture,
         this.method_46426() + padding,
         this.method_46427() + padding,
         this.field_22758 - padding * 2,
         this.field_22759 - padding * 2,
         0.0F,
         0.0F,
         this.textureWidth,
         this.textureHeight,
         this.textureWidth,
         this.textureHeight
      );
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void openLink(String url) {
      class_156.method_668().method_670(url);
      BlockPopsMod.logDebug("Opening link: {}", url);
   }

   protected boolean method_25351(int button) {
      return button == 0;
   }
}
