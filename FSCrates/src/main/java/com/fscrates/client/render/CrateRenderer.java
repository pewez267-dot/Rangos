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
 * In-world renderer for the crate. Always draws the chest (tinted by tier).
 * During the opening, every {@link Style} is rendered with its own staging so
 * each animation feels distinct: roulette/slot reels with hard deceleration
 * and a winner pop, an orbit that eliminates losers one by one, an item rain
 * that lands and converges, an explosion that bursts and returns, a beam that
 * descends, a shatter that breaks the chest open, a portal/summon-circle that
 * grows on top, a giant card that flips, a wave-pulse that swells, a
 * galaxy-swirl that condenses, and a fireworks finale.
 *
 * <p>The reward stays in <b>full view</b> at all times — particles and the
 * chest always render below the floating reward (Y-translation moves the
 * winner well above the chest top).
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

        // ---- chest body (with style-specific transforms) ----
        float chestScale = 1f;
        float extraSpin = 0f;
        if (be.animating) {
            if (style == Style.SPIN && p < CrateBlockEntity.P_REVEAL_END) {
                float t = (p - CrateBlockEntity.P_OPEN_END) / Math.max(0.001f,
                        CrateBlockEntity.P_REVEAL_END - CrateBlockEntity.P_OPEN_END);
                t = Math.max(0f, Math.min(1f, t));
                extraSpin = t * 720f * (1f - t * 0.4f);
            }
            if (style == Style.SHATTER && p >= 0.30f) {
                chestScale = Math.max(0f, 1f - (p - 0.30f) / 0.20f);
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

            float r = 0.55f + 0.45f * rarity.redF();
            float g = 0.55f + 0.45f * rarity.greenF();
            float b = 0.55f + 0.45f * rarity.blueF();
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
            model.render(pose, vc, light, overlay, lidAngle, r, g, b, 1.0f);
            pose.popPose();
        }

        // ---- the reveal: well above the chest, billboarded to the camera ----
        if (be.animating && p >= CrateBlockEntity.P_OPEN_END) {
            renderReveal(be, style, partialTick, pose, buffers, light, overlay);
        }

        // ---- holograms ----
        renderHolograms(be, cfg, rarity, pose, buffers, light);
    }

    // ------------------------------------------------------------------
    // Reveal staging — one renderer per style
    // ------------------------------------------------------------------

    private void renderReveal(CrateBlockEntity be, Style style, float partial, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        List<ItemStack> cands = be.getCandidates();
        if (cands.isEmpty()) {
            return;
        }
        float rp = be.revealProgress(partial);
        float fp = be.finaleProgress(partial);
        int n = cands.size();
        int winner = Math.max(0, Math.min(n - 1, be.getWinnerIndex()));

        // Billboard: anchor 1.55 above the block (well above the 0.875-tall chest)
        float camYaw = Minecraft.getInstance().getEntityRenderDispatcher().camera.getYRot();
        pose.pushPose();
        pose.translate(0.5, 1.55, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-camYaw));

        switch (style) {
            case ROULETTE -> renderRoulette(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case SLOT_MACHINE -> renderSlot(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case ORBIT -> renderOrbit(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case GALAXY_SWIRL -> renderGalaxy(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case ITEM_RAIN -> renderRain(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case LOOT_EXPLOSION -> renderExplosion(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case BEAM_REVEAL -> renderBeam(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case CARD_FLIP -> renderCardFlip(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case SHATTER -> renderShatter(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case PORTAL -> renderPortal(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case SUMMON_CIRCLE -> renderSummon(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case WAVE_PULSE -> renderWave(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case FIREWORKS -> renderFireworks(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case SPIN -> renderSpin(be, cands, winner, rp, fp, partial, pose, buffers, light, overlay);
            case INSTANT -> renderItem(be, cands.get(winner), pose, buffers, light, overlay,
                    0, 0, 0, 0.9f, 0, 1f);
        }

        pose.popPose();
    }

    // -------------------- ROULETTE: horizontal reel --------------------
    private void renderRoulette(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                                float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int n = cands.size();
        float spacing = 0.55f;
        // travel: start fast, end on the winner
        float loops = 3.5f;
        float maxTravel = n * loops + winner;
        float scroll = easeOutQuart(rp) * maxTravel;
        int base = (int) Math.floor(scroll);
        float frac = scroll - base;
        boolean stopped = rp >= 1f;

        for (int k = -3; k <= 3; k++) {
            int idx = Math.floorMod(base + k, n);
            float off = (k - frac) * spacing;
            if (Math.abs(off) > 1.7f) continue;
            boolean center = Math.abs(off) < spacing * 0.45f;
            float tilt = (float) (Math.sin(off * 1.2) * 8f);
            float scale = 0.65f - Math.abs(off) * 0.18f;
            if (stopped && center) {
                scale += pulseScale(fp, partial, be.animTick) * 0.45f;
            }
            renderItem(be, cands.get(idx), pose, buffers, light, overlay,
                    off, 0, 0, scale, tilt, 1f);
        }
    }

    // -------------------- SLOT MACHINE: 3 vertical reels stop one by one
    private void renderSlot(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                            float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int n = cands.size();
        float[] lanes = { -0.65f, 0f, 0.65f };
        float spacing = 0.5f;
        // each reel finishes at a different rp threshold
        float[] finish = { 0.55f, 0.75f, 0.95f };
        float[] phase = { 0.4f * n, 0.7f * n, 0.2f * n };

        for (int lane = 0; lane < 3; lane++) {
            float t = Math.min(1f, rp / finish[lane]);
            float scroll = easeOutCubic(t) * (n * 4f + winner) + phase[lane];
            int base = (int) Math.floor(scroll);
            float frac = scroll - base;
            for (int k = -2; k <= 2; k++) {
                int idx = Math.floorMod(base + k, n);
                if (lane == 1 && rp >= finish[lane] && k == 0) {
                    idx = winner;
                }
                float off = (k - frac) * spacing;
                if (Math.abs(off) > 1.4f) continue;
                boolean center = Math.abs(off) < spacing * 0.5f;
                float scale = 0.45f - Math.abs(off) * 0.15f;
                if (rp > 0.95f && lane == 1 && center) {
                    scale += pulseScale(fp, partial, be.animTick) * 0.35f;
                }
                renderItem(be, cands.get(idx), pose, buffers, light, overlay,
                        lanes[lane], off, 0, scale, 0, 1f);
            }
        }
    }

    // -------------------- ORBIT: candidates orbit, losers vanish, winner remains
    private void renderOrbit(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                             float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int n = cands.size();
        float spin = (be.animTick + partial) * (220f * (1.05f - rp)) / 60f;
        float radius = (1f - easeOutCubic(rp)) * 0.95f + 0.05f;
        // eliminate losers gradually
        int alive = Math.max(1, (int) Math.ceil(n * (1f - rp)));
        for (int i = 0; i < n; i++) {
            if (i != winner && i >= alive - 1 && i != 0) {
                continue; // dropped
            }
            double a = Math.toRadians(spin + i * (360.0 / Math.max(1, n)));
            float x = (float) (Math.cos(a) * radius);
            float y = (float) (Math.sin(a) * radius * 0.7);
            float scale = 0.42f * (i == winner ? 1f + rp * 0.3f : 1f);
            renderItem(be, cands.get(i), pose, buffers, light, overlay, x, y, 0, scale, 0, 1f);
        }
        if (rp > 0.92f) {
            float pop = pulseScale(fp, partial, be.animTick);
            renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, 0, 0, 0.7f + pop * 0.5f, 0, 1f);
        }
    }

    // -------------------- GALAXY: spiral inwards
    private void renderGalaxy(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                              float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int n = cands.size();
        float spin = (be.animTick + partial) * 6f;
        float baseR = (1f - easeOutCubic(rp)) * 1.1f;
        for (int i = 0; i < n; i++) {
            float r = baseR * (0.5f + 0.5f * (i / (float) n));
            double a = Math.toRadians(spin + i * (360.0 / n));
            float x = (float) (Math.cos(a) * r);
            float y = (float) (Math.sin(a) * r * 0.5);
            float scale = 0.4f * (1f - rp * 0.5f);
            renderItem(be, cands.get(i), pose, buffers, light, overlay, x, y, 0, scale, 0, 1f);
        }
        renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, 0, 0,
                0.4f + easeOutCubic(rp) * 0.6f + pulseScale(fp, partial, be.animTick) * 0.3f, 0, 1f);
    }

    // -------------------- ITEM RAIN: items fall, then converge to the winner
    private void renderRain(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                            float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int n = cands.size();
        float t = be.animTick + partial;
        for (int i = 0; i < n; i++) {
            float startDelay = (i % 5) * 0.07f;
            float ti = Math.max(0f, Math.min(1f, (rp - startDelay) / 0.55f));
            float fallY = 1.5f - ti * 1.5f; // top to centre
            float xOff = ((i % 4) - 1.5f) * 0.4f;
            float zOff = (((i / 4) % 3) - 1f) * 0.4f;
            float convergence = Math.max(0f, (rp - 0.65f) / 0.30f);
            xOff *= 1f - convergence;
            zOff *= 1f - convergence;
            renderItem(be, cands.get(i), pose, buffers, light, overlay, xOff, fallY, zOff, 0.4f, t * 2f, 1f);
        }
        if (rp >= 0.98f) {
            renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, 0, 0,
                    0.7f + pulseScale(fp, partial, be.animTick) * 0.5f, 0, 1f);
        }
    }

    // -------------------- LOOT EXPLOSION: burst then return
    private void renderExplosion(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                                 float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int n = cands.size();
        // out-and-back curve: peak at rp=0.5
        float dist = (float) Math.sin(Math.PI * Math.min(1f, rp / 0.95f)) * 1.0f;
        float spin = (be.animTick + partial) * 8f;
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(i * (360.0 / n));
            float x = (float) (Math.cos(a) * dist);
            float y = (float) (Math.sin(a) * dist * 0.6);
            renderItem(be, cands.get(i), pose, buffers, light, overlay, x, y, 0, 0.45f, spin, 1f);
        }
        if (rp > 0.85f) {
            renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, 0, 0,
                    0.55f + (rp - 0.85f) / 0.15f * 0.4f + pulseScale(fp, partial, be.animTick) * 0.2f, 0, 1f);
        }
    }

    // -------------------- BEAM REVEAL: descend through a beam
    private void renderBeam(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                            float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float y = 1.4f - easeOutQuart(rp) * 1.4f;
        float scale = 0.55f + easeOutCubic(rp) * 0.3f + pulseScale(fp, partial, be.animTick) * 0.2f;
        renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, y, 0, scale, 0, 1f);
    }

    // -------------------- CARD FLIP: huge flipping card
    private void renderCardFlip(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                                float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        // float face down, then flip near the end
        if (rp < 0.7f) {
            float bob = (float) Math.sin((be.animTick + partial) * 0.2f) * 0.05f;
            // X-scale negative until flip — render losers cycling on the back
            int n = cands.size();
            int idx = ((int) ((be.animTick + partial) * 0.5f)) % n;
            renderItem(be, cands.get(idx), pose, buffers, light, overlay, 0, bob, 0, 0.85f, 0, -1f);
            return;
        }
        float t = (rp - 0.7f) / 0.30f;
        float xs = (float) Math.cos(t * Math.PI); // 1 -> -1, sign flip in the middle
        renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, 0, 0,
                0.85f + pulseScale(fp, partial, be.animTick) * 0.3f, 0, xs >= 0 ? -xs : -xs);
        // when |xs| crosses 0 the card "flips" — the renderItem handles xScale
    }

    // -------------------- SHATTER: chest already shattered, item flies up
    private void renderShatter(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                               float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float y = -0.6f + easeOutCubic(rp) * 0.8f;
        float spin = (be.animTick + partial) * 10f * (1f - rp * 0.7f);
        renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, y, 0,
                0.55f + rp * 0.3f + pulseScale(fp, partial, be.animTick) * 0.25f, spin, 1f);
    }

    // -------------------- PORTAL: spiraling above, item rises through
    private void renderPortal(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                              float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float spin = (be.animTick + partial) * 18f * (1.05f - rp);
        float y = -0.4f + easeOutCubic(rp) * 0.4f;
        renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, y, 0,
                0.5f + rp * 0.4f + pulseScale(fp, partial, be.animTick) * 0.2f, spin, 1f);
    }

    // -------------------- SUMMON CIRCLE: rising then steady
    private void renderSummon(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                              float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float y = (1f - easeOutCubic(rp)) * 0.7f;
        float spin = (be.animTick + partial) * 6f;
        renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, y, 0,
                0.55f + rp * 0.3f + pulseScale(fp, partial, be.animTick) * 0.2f, spin, 1f);
    }

    // -------------------- WAVE PULSE: scale pulses
    private void renderWave(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                            float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float t = be.animTick + partial;
        float s = 0.55f + 0.12f * (float) Math.sin(t * 0.5f) + easeOutCubic(rp) * 0.3f
                + pulseScale(fp, partial, be.animTick) * 0.25f;
        renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, 0, 0, s, 0, 1f);
    }

    // -------------------- FIREWORKS: item arrives early, explosions happen via particles
    private void renderFireworks(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                                 float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float bob = (float) Math.sin((be.animTick + partial) * 0.15f) * 0.04f;
        renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, bob, 0,
                0.65f + rp * 0.25f + pulseScale(fp, partial, be.animTick) * 0.3f, (be.animTick + partial) * 3f, 1f);
    }

    // -------------------- SPIN: item lands after the chest spin
    private void renderSpin(CrateBlockEntity be, List<ItemStack> cands, int winner, float rp, float fp,
                            float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (rp < 0.4f) {
            return; // chest is spinning; reward not visible yet
        }
        float t = (rp - 0.4f) / 0.6f;
        float yaw = (be.animTick + partial) * (90f * (1.05f - t));
        float scale = 0.45f + easeOutBack(t) * 0.4f + pulseScale(fp, partial, be.animTick) * 0.25f;
        renderItem(be, cands.get(winner), pose, buffers, light, overlay, 0, 0, 0, scale, yaw, 1f);
    }

    // ------------------------------------------------------------------
    // Generic helpers
    // ------------------------------------------------------------------

    private void renderItem(CrateBlockEntity be, ItemStack stack, PoseStack pose, MultiBufferSource buffers,
                            int light, int overlay, float x, float y, float z,
                            float scale, float yaw, float xScale) {
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

    /** Brief celebratory bounce on the winner during the finale phase. */
    private static float pulseScale(float fp, float partial, int tick) {
        if (fp <= 0f) return 0f;
        return (float) (Math.sin(fp * Math.PI * 2 + (tick + partial) * 0.4f) * 0.08f * (1f - fp));
    }

    private static float easeOutCubic(float t) { float x = 1f - t; return 1f - x * x * x; }
    private static float easeOutQuart(float t) { float x = 1f - t; return 1f - x * x * x * x; }
    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1f, x = t - 1f;
        return 1f + c3 * x * x * x + c1 * x * x;
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
        float baseY = be.animating ? 2.45f : 1.4f;
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
        if (s == null || s.indexOf('&') < 0) return s;
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
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
