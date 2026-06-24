package com.fantasticpass.nametag;

import com.fantasticpass.config.PassConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

/**
 * Renders the extra rank line below a player's vanilla name. The math mirrors vanilla
 * {@code EntityRenderer#renderNameTag} (billboard towards the camera, same 0.025 text
 * scale) but applies the configured line scale and a vertical offset so the line sits
 * just beneath the name without modifying any vanilla rendering.
 */
public final class NametagRenderer {

    private static final int FULL_BRIGHT_TEXT = 553648127; // vanilla "see-through" text color
    private static final double BASE_TEXT_SCALE = 0.025D;

    private NametagRenderer() {
    }

    public static void render(Entity entity, Component line, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight,
                              EntityRenderDispatcher dispatcher, Font font) {
        float lineScale = (float) (double) PassConfig.LINE_SCALE.get();
        float verticalOffset = (float) (double) PassConfig.VERTICAL_OFFSET.get();

        float base = entity.getBbHeight() + 0.5F;

        poseStack.pushPose();
        poseStack.translate(0.0D, base + verticalOffset, 0.0D);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(-(float) BASE_TEXT_SCALE * lineScale,
                -(float) BASE_TEXT_SCALE * lineScale,
                (float) BASE_TEXT_SCALE * lineScale);

        Matrix4f matrix = poseStack.last().pose();
        float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int backgroundColor = (int) (backgroundOpacity * 255.0F) << 24;
        float x = (float) (-font.width(line) / 2);
        boolean seeThrough = !entity.isDiscrete();

        font.drawInBatch(line, x, 0.0F, FULL_BRIGHT_TEXT, false, matrix, buffer,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
                backgroundColor, packedLight);

        if (seeThrough) {
            font.drawInBatch(line, x, 0.0F, -1, false, matrix, buffer,
                    Font.DisplayMode.NORMAL, 0, packedLight);
        }

        poseStack.popPose();
    }
}
