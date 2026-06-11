package com.fscrates.client.render;

import com.fscrates.FSCrates;
import com.fscrates.block.CrateBlock;
import com.fscrates.block.CrateBlockEntity;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import com.mojang.math.Axis;

/**
 * In-world renderer for the crate. Always draws the chest (tinted by tier) and,
 * while opening, animates the lid, jolts the chest, and floats the spinning
 * reward above it. Also draws the optional floating name and hologram text.
 */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FSCrates.MOD_ID, "textures/entity/crate/crate.png");

    private final CrateModel model;
    private final Font font;

    public CrateRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new CrateModel(ctx.bakeLayer(CrateModel.LAYER));
        this.font = ctx.getFont();
    }

    @Override
    public void render(CrateBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        CrateConfig cfg = be.getConfig();
        Rarity rarity = cfg.rarity;

        float rot = facingYRot(be);
        float lidAngle = be.lidOpen(partialTick) * ((float) Math.PI / 2f);
        float shake = be.shake(partialTick);
        float bob = (float) Math.sin((be.ambientTime + partialTick) * 0.1f) * 0.02f;

        // --- chest body ---
        pose.pushPose();
        pose.translate(0.5, 0.5 + bob, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-rot));
        pose.translate(shake, 0, 0);
        pose.translate(-0.5, -0.5, -0.5);

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        model.render(pose, vc, light, overlay, lidAngle,
                rarity.redF(), rarity.greenF(), rarity.blueF(), 1.0f);
        pose.popPose();

        // --- floating spinning reward during the reveal ---
        if (be.animating) {
            float p = be.progress();
            if (p >= 0.45f && !be.getRewardIcon().isEmpty()) {
                renderFloatingReward(be, partialTick, pose, buffers, light, overlay, p);
            }
        }

        // --- hologram text (name + free text) ---
        renderHolograms(be, cfg, rarity, pose, buffers, light);
    }

    private void renderFloatingReward(CrateBlockEntity be, float partial, PoseStack pose,
                                      MultiBufferSource buffers, int light, int overlay, float p) {
        float rise = Math.min(1f, (p - 0.45f) / 0.35f);
        float y = 1.05f + rise * 0.45f;
        float spin = (be.animTick + partial) * 9f;
        float scale = 0.55f + 0.15f * (float) Math.sin((be.animTick + partial) * 0.3f);

        pose.pushPose();
        pose.translate(0.5, y, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                be.getRewardIcon(), ItemDisplayContext.GROUND, light, overlay,
                pose, buffers, be.getLevel(), 0);
        pose.popPose();
    }

    private void renderHolograms(CrateBlockEntity be, CrateConfig cfg, Rarity rarity,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        if (cfg.floatingName && cfg.displayName != null && !cfg.displayName.isEmpty()) {
            lines.add(Component.literal(colorize(cfg.displayName)).withStyle(rarity.color()));
        }
        for (String l : cfg.floatingText) {
            if (l != null && !l.isEmpty()) {
                lines.add(Component.literal(colorize(l)));
            }
        }
        if (lines.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        float baseY = 1.35f;
        float lineH = 0.26f;
        // draw from top down so the name sits highest
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            pose.pushPose();
            pose.translate(0.5, baseY + (lines.size() - 1 - i) * lineH, 0.5);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(-0.025f, -0.025f, 0.025f);
            Matrix4f mat = pose.last().pose();
            float bgOpacity = mc.options.getBackgroundOpacity(0.25f);
            int bg = (int) (bgOpacity * 255.0f) << 24;
            float x = -font.width(line) / 2f;
            font.drawInBatch(line, x, 0, 0xFFFFFFFF, false, mat, buffers,
                    Font.DisplayMode.SEE_THROUGH, bg, light);
            font.drawInBatch(line, x, 0, 0xFFFFFFFF, false, mat, buffers,
                    Font.DisplayMode.NORMAL, 0, light);
            pose.popPose();
        }
    }

    /** Translate legacy {@code &} colour codes to the section sign. */
    private static String colorize(String s) {
        if (s == null || s.indexOf('&') < 0) {
            return s;
        }
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length - 1; i++) {
            if (c[i] == '&' && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(c[i + 1]) >= 0) {
                c[i] = '\u00A7';
            }
        }
        return new String(c);
    }

    private static float facingYRot(CrateBlockEntity be) {
        try {
            Direction d = be.getBlockState().getValue(CrateBlock.FACING);
            return d.toYRot();
        } catch (Exception e) {
            return 0f;
        }
    }

    @Override
    public boolean shouldRenderOffScreen(CrateBlockEntity be) {
        return true; // so tall holograms / beams keep rendering near screen edges
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
