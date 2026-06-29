package com.fantasticpass.nametag;

import com.fantasticpass.config.PassConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

public final class NametagRenderer {
   private static final int FULL_BRIGHT_TEXT = 553648127;
   private static final double BASE_TEXT_SCALE = 0.025;

   private NametagRenderer() {
   }

   public static void render(
      Entity entity, Component line, PoseStack poseStack, MultiBufferSource buffer, int packedLight, EntityRenderDispatcher dispatcher, Font font
   ) {
      float lineScale = (float)((Double)PassConfig.LINE_SCALE.get()).doubleValue();
      float verticalOffset = (float)((Double)PassConfig.VERTICAL_OFFSET.get()).doubleValue();
      float base = entity.getBbHeight() + 0.5F;
      poseStack.pushPose();
      poseStack.translate(0.0, (double)(base + verticalOffset), 0.0);
      poseStack.mulPose(dispatcher.cameraOrientation());
      poseStack.scale(-0.025F * lineScale, -0.025F * lineScale, 0.025F * lineScale);
      Matrix4f matrix = poseStack.last().pose();
      float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
      int backgroundColor = (int)(backgroundOpacity * 255.0F) << 24;
      float x = (float)(-font.width(line) / 2);
      boolean seeThrough = !entity.isDiscrete();
      font.drawInBatch(line, x, 0.0F, 553648127, false, matrix, buffer, seeThrough ? DisplayMode.SEE_THROUGH : DisplayMode.NORMAL, backgroundColor, packedLight);
      if (seeThrough) {
         font.drawInBatch(line, x, 0.0F, -1, false, matrix, buffer, DisplayMode.NORMAL, 0, packedLight);
      }

      poseStack.popPose();
   }
}
