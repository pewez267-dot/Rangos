package com.fscrates.client.render;

import com.fscrates.FSCrates;
import com.fscrates.animation.CrateAnimation.Style;
import com.fscrates.block.CrateBlock;
import com.fscrates.block.CrateBlockEntity;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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

import java.util.List;

/**
 * In-world renderer for the crate. Always draws the chest (tinted by tier). When
 * opening, the lid swings up and a Trial-Chamber-style reward roulette spins
 * above the chest and decelerates onto the winner — the reward always stays in
 * full view, never covered by the effects. The selected {@link Style} changes
 * how the reveal is staged (roulette, slot reels, orbit, beam, rise, flip...).
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
        Style style = be.getAnimation().style();
        float p = be.progress();

        // ---- chest body ----
        float chestScale = 1f;
        float extraSpin = 0f;
        if (be.animating) {
            if (style == Style.SPIN && p < 0.42f) {
                extraSpin = (be.animTick + partialTick) * (18f * (1f - Math.min(1f, p / 0.42f)));
            }
            if (style == Style.SHATTER && p >= 0.30f) {
                chestScale = Math.max(0f, 1f - (p - 0.30f) / 0.15f);
            }
        }

        if (chestScale > 0.001f) {
            float rot = facingYRot(be);
            float lidAngle = be.lidOpen(partialTick) * ((float) Math.PI / 2f);
            float shake = be.shake(partialTick);
            float bob = (float) Math.sin((be.ambientTime + partialTick) * 0.1f) * 0.02f;

            pose.pushPose();
            pose.translate(0.5, 0.5 + bob, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-rot + extraSpin));
            pose.translate(shake, 0, 0);
            pose.scale(chestScale, chestScale, chestScale);
            pose.translate(-0.5, -0.5, -0.5);

            // softer tint so the wood reads but is themed by tier
            float r = 0.55f + 0.45f * rarity.redF();
            float g = 0.55f + 0.45f * rarity.greenF();
            float b = 0.55f + 0.45f * rarity.blueF();
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
            model.render(pose, vc, light, overlay, lidAngle, r, g, b, 1.0f);
            pose.popPose();
        }

        // ---- reward reveal (always on top, facing the camera) ----
        if (be.animating && p >= 0.42f) {
            renderReveal(be, style, partialTick, pose, buffers, light, overlay);
        }

        // ---- hologram text ----
        renderHolograms(be, cfg, rarity, pose, buffers, light);
    }

    // ------------------------------------------------------------------
    // Reveal staging
    // ------------------------------------------------------------------

    private void renderReveal(CrateBlockEntity be, Style style, float partial, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        List<ItemStack> cands = be.getCandidates();
        if (cands.isEmpty()) {
            return;
        }
        float rp = be.revealProgress(partial);
        int n = cands.size();
        int winner = Math.max(0, Math.min(n - 1, be.getWinnerIndex()));

        // billboard the whole reveal to face the camera (horizontal)
        float camYaw = Minecraft.getInstance().getEntityRenderDispatcher().camera.getYRot();
        pose.pushPose();
        pose.translate(0.5, 1.25, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-camYaw));

        switch (style) {
            case ROULETTE, SLOT_MACHINE -> renderStrip(be, style, cands, winner, rp, partial, pose, buffers, light, overlay);
            case ORBIT, GALAXY_SWIRL -> renderOrbit(be, cands, winner, rp, partial, pose, buffers, light, overlay);
            default -> renderSingle(be, style, cands.get(winner), rp, partial, pose, buffers, light, overlay);
        }

        pose.popPose();
    }

    /** Horizontal (roulette) or vertical (slot) scrolling reel that lands on the winner. */
    private void renderStrip(CrateBlockEntity be, Style style, List<ItemStack> cands, int winner, float rp,
                             float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int n = cands.size();
        boolean vertical = style == Style.SLOT_MACHINE;
        float spacing = 0.5f;
        int loops = 3;
        float maxTravel = (float) (n * loops) + winner;
        float posScroll = easeOutCubic(rp) * maxTravel;
        int baseIndex = (int) Math.floor(posScroll);
        float frac = posScroll - baseIndex;

        for (int k = -2; k <= 2; k++) {
            int idx = Math.floorMod(baseIndex + k, n);
            float off = (k - frac) * spacing;
            if (Math.abs(off) > 1.05f) {
                continue;
            }
            boolean center = Math.abs(off) < spacing * 0.5f;
            float scale = 0.62f - Math.abs(off) * 0.18f;
            if (rp > 0.96f && center) {
                scale = 0.62f + (rp - 0.96f) / 0.04f * 0.35f; // winner pops
            }
            float x = vertical ? 0f : off;
            float y = vertical ? off : 0f;
            renderItem(be, cands.get(idx), pose, buffers, light, overlay, x, y, 0, scale,
                    (be.animTick + partial) * (center ? 2f : 0f));
        }
    }

    /** Candidates orbit, then converge on the winner. */
    private void renderOrbit(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp,
                             float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int n = cands.size();
        float radius = (1f - easeOutCubic(rp)) * 0.7f;
        float spin = (be.animTick + partial) * 8f;
        if (rp < 0.85f) {
            for (int i = 0; i < n; i++) {
                double a = Math.toRadians(spin + i * (360.0 / n));
                float x = (float) (Math.cos(a) * radius);
                float y = (float) (Math.sin(a) * radius * 0.6);
                renderItem(be, cands.get(i), pose, buffers, light, overlay, x, y, 0, 0.4f, 0);
            }
        } else {
            float s = 0.6f + (rp - 0.85f) / 0.15f * 0.4f;
            renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, 0, 0, s, spin);
        }
    }

    /** Single-winner reveal with a style-specific entrance. */
    private void renderSingle(CrateBlockEntity be, Style style, ItemStack winner, float rp,
                              float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float e = easeOutCubic(rp);
        float y = 0f, x = 0f, scale = 0.45f + e * 0.4f, yaw = 0f, xScale = 1f;
        float t = be.animTick + partial;

        switch (style) {
            case SPIN -> yaw = t * (40f * (1.05f - rp));
            case BEAM_REVEAL -> { y = (1f - e) * 0.9f; }
            case ITEM_RAIN -> { y = (1f - e) * 1.1f; if (rp > 0.85f) y += (float) Math.sin((rp - 0.85f) * 40f) * 0.05f; }
            case LOOT_EXPLOSION -> { y = -0.5f + e * 0.5f; yaw = t * 6f; }
            case CARD_FLIP -> { xScale = (float) Math.abs(Math.sin(rp * Math.PI / 2f * 3f)); yaw = e * 360f; }
            case PORTAL -> { y = -0.4f + e * 0.4f; yaw = t * 12f * (1.05f - rp); }
            case SUMMON_CIRCLE -> { y = (1f - e) * 0.7f; yaw = t * 5f; }
            case WAVE_PULSE -> scale = 0.5f + 0.12f * (float) Math.sin(t * 0.5f) + e * 0.25f;
            default -> yaw = t * 3f;
        }
        renderItem(be, winner, pose, buffers, light, overlay, x, y, 0, scale, yaw, xScale);
    }

    private void renderItem(CrateBlockEntity be, ItemStack stack, PoseStack pose, MultiBufferSource buffers,
                            int light, int overlay, float x, float y, float z, float scale, float yaw) {
        renderItem(be, stack, pose, buffers, light, overlay, x, y, z, scale, yaw, 1f);
    }

    private void renderItem(CrateBlockEntity be, ItemStack stack, PoseStack pose, MultiBufferSource buffers,
                            int light, int overlay, float x, float y, float z, float scale, float yaw, float xScale) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.scale(scale * xScale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                0xF000F0, overlay, pose, buffers, be.getLevel(), 0);
        pose.popPose();
    }

    // ------------------------------------------------------------------
    // Holograms
    // ------------------------------------------------------------------

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
        float baseY = be.animating ? 2.05f : 1.4f;
        float lineH = 0.26f;
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
            font.drawInBatch(line, x, 0, 0xFFFFFFFF, false, mat, buffers, Font.DisplayMode.SEE_THROUGH, bg, light);
            font.drawInBatch(line, x, 0, 0xFFFFFFFF, false, mat, buffers, Font.DisplayMode.NORMAL, 0, light);
            pose.popPose();
        }
    }

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

    private static float easeOutCubic(float t) {
        float x = 1f - t;
        return 1f - x * x * x;
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
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
