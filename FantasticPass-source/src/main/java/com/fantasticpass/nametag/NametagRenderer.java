package com.fantasticpass.nametag;

import com.fantasticpass.config.PassConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import java.lang.reflect.Field;

public final class NametagRenderer {

    private static final int FULL_BRIGHT_TEXT = 0x20FFFFFF;
    private static final double BASE_TEXT_SCALE = 0.025;

    // --- Integracion con el mod cliente NoNameTags (para fotos): si el jugador oculta los nombres,
    // tambien ocultamos la linea del pase. Se resuelve por reflexion; si el mod no esta, no pasa nada.
    private static boolean noNameTagsChecked;
    private static Field hideField;

    private NametagRenderer() {
    }

    /** true si el mod NoNameTags esta activo y con los nombres ocultos. */
    private static boolean nameTagsHidden() {
        if (!noNameTagsChecked) {
            noNameTagsChecked = true;
            try {
                Class<?> clazz = Class.forName("no.name.tags.NoNameTags");
                hideField = clazz.getField("hideNameTags");
            } catch (Throwable ignored) {
                hideField = null;
            }
        }
        if (hideField == null) {
            return false;
        }
        try {
            return hideField.getBoolean(null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void render(Entity entity, Component line, PoseStack poseStack, MultiBufferSource buffer, int packedLight, EntityRenderDispatcher dispatcher, Font font) {
        // Si el jugador oculto los nombres (NoNameTags), no dibujamos la linea del pase tampoco.
        if (nameTagsHidden()) {
            return;
        }
        float lineScale = (float) PassConfig.LINE_SCALE.get().doubleValue();
        float verticalOffset = (float) PassConfig.VERTICAL_OFFSET.get().doubleValue();
        float base = entity.getBbHeight() + 0.5f;
        poseStack.pushPose();
        poseStack.translate(0.0, base + verticalOffset, 0.0);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(-0.025f * lineScale, -0.025f * lineScale, 0.025f * lineScale);
        Matrix4f matrix = poseStack.last().pose();
        double backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25f);
        int backgroundColor = (int) (backgroundOpacity * 255.0) << 24;
        float x = -font.width((FormattedText) line) / 2.0f;
        boolean seeThrough = !entity.isDiscrete();
        font.drawInBatch(line, x, 0.0f, FULL_BRIGHT_TEXT, false, matrix, buffer,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, backgroundColor, packedLight);
        if (seeThrough) {
            font.drawInBatch(line, x, 0.0f, -1, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        }
        poseStack.popPose();
    }
}
