package com.fscrates.client.render;

import com.fscrates.FSCrates;
import com.fscrates.animation.CrateAnimation;
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
 * In-world renderer for the crate. The reward is ALWAYS revealed as a spinning
 * roulette (horizontal reel) or slot reel (vertical) that decelerates onto the
 * winner — the reward stays in full view above the chest. A coloured light beam
 * (themed) shines up through the reel, the chest lid swings and the chest hops
 * when it opens. The reward never gets covered by the effects.
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
        CrateAnimation anim = be.getAnimation();
        Style style = anim.style();
        float p = be.progress();

        // ---- chest body (lid swing + opening hop + reveal spin + squash + wobble) ----
        float rot = facingYRot(be);
        float lidAngle = be.lidOpen(partialTick) * ((float) Math.PI / 2f);
        float shake = be.shake(partialTick);
        float hop = chestHop(be, partialTick);
        float bob = (float) Math.sin((be.ambientTime + partialTick) * 0.1f) * 0.02f;
        float spin = chestSpin(be, partialTick);
        float sc = chestScale(be, partialTick);
        float wob = chestWobble(be, partialTick);

        pose.pushPose();
        pose.translate(0.5, 0.5 + bob + hop, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-rot + spin));
        if (wob != 0f) {
            pose.mulPose(Axis.ZP.rotationDegrees(wob));
        }
        pose.translate(shake, 0, 0);
        pose.scale(sc, sc, sc);
        pose.translate(-0.5, -0.5, -0.5);
        float r = 0.55f + 0.45f * rarity.redF();
        float g = 0.55f + 0.45f * rarity.greenF();
        float b = 0.55f + 0.45f * rarity.blueF();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        model.render(pose, vc, light, overlay, lidAngle, r, g, b, 1.0f);
        pose.popPose();

        // ---- light beam (themed) ----
        if (be.animating && anim.hasBeam() && p >= CrateBlockEntity.P_ANTICIPATION_END) {
            renderBeam(be, pose, buffers, partialTick);
        }

        // ---- the reward roulette (always, above the chest, facing camera) ----
        if (be.animating && style != Style.INSTANT && p >= CrateBlockEntity.P_OPEN_END) {
            renderReel(be, style == Style.SLOT_MACHINE, partialTick, pose, buffers, light, overlay);
        } else if (be.animating && style == Style.INSTANT && !be.getCandidates().isEmpty()) {
            float camYaw = Minecraft.getInstance().getEntityRenderDispatcher().camera.getYRot();
            pose.pushPose();
            pose.translate(0.5, 1.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-camYaw));
            renderItem(be, be.getCandidates().get(be.getWinnerIndex()), pose, buffers, light, overlay, 0, 0, 0, 0.9f, 0);
            pose.popPose();
        }

        // ---- holograms ----
        renderHolograms(be, cfg, rarity, pose, buffers, light);
    }

    /** Small upward hop while the lid pops, settling slightly raised during the reveal. */
    private float chestHop(CrateBlockEntity be, float partial) {
        if (!be.animating) return 0f;
        float p = (be.animTick + partial) / Math.max(1, be.animTotal);
        if (p < CrateBlockEntity.P_ANTICIPATION_END) {
            return 0f;
        }
        if (p < CrateBlockEntity.P_OPEN_END) {
            float t = (p - CrateBlockEntity.P_ANTICIPATION_END)
                    / (CrateBlockEntity.P_OPEN_END - CrateBlockEntity.P_ANTICIPATION_END);
            return (float) Math.sin(t * Math.PI) * 0.18f;
        }
        if (p < 0.94f) {
            return 0.04f + (float) Math.sin((be.animTick + partial) * 0.15f) * 0.015f;
        }
        return 0f;
    }

    /** Slow spin during the reveal, faster celebratory spin in the finale. */
    private float chestSpin(CrateBlockEntity be, float partial) {
        if (!be.animating) return 0f;
        float p = (be.animTick + partial) / Math.max(1, be.animTotal);
        if (p < CrateBlockEntity.P_OPEN_END) return 0f;
        if (p < CrateBlockEntity.P_REVEAL_END) return (be.animTick + partial) * 3.0f;
        return (be.animTick + partial) * 9.0f;
    }

    /** Squash/stretch breathing: trembles while charging, pops on the win. */
    private float chestScale(CrateBlockEntity be, float partial) {
        if (!be.animating) return 1f;
        float p = (be.animTick + partial) / Math.max(1, be.animTotal);
        if (p < CrateBlockEntity.P_ANTICIPATION_END) {
            return 1f + (float) Math.sin((be.animTick + partial) * 1.6f) * 0.05f;
        }
        float fp = be.finaleProgress(partial);
        if (fp > 0f) {
            return 1f + (float) Math.sin(fp * Math.PI) * 0.18f; // a satisfying pop
        }
        return 1f + (float) Math.sin((be.animTick + partial) * 0.2f) * 0.02f;
    }

    /** Side-to-side tilt during the tense charging phase. */
    private float chestWobble(CrateBlockEntity be, float partial) {
        if (!be.animating) return 0f;
        float p = (be.animTick + partial) / Math.max(1, be.animTotal);
        if (p >= CrateBlockEntity.P_ANTICIPATION_END) return 0f;
        float intensity = (CrateBlockEntity.P_ANTICIPATION_END - p) / CrateBlockEntity.P_ANTICIPATION_END;
        return (float) Math.sin((be.animTick + partial) * 2.0f) * 6f * intensity;
    }

    // ------------------------------------------------------------------
    // Reward reel (roulette / slot) — the working reveal, used by all anims
    // ------------------------------------------------------------------

    private void renderReel(CrateBlockEntity be, boolean vertical, float partial, PoseStack pose,
                            MultiBufferSource buffers, int light, int overlay) {
        List<ItemStack> cands = be.getCandidates();
        if (cands.isEmpty()) {
            return;
        }
        int n = cands.size();
        int winner = Math.max(0, Math.min(n - 1, be.getWinnerIndex()));
        float rp = be.revealProgress(partial);
        float fp = be.finaleProgress(partial);

        float camYaw = Minecraft.getInstance().getEntityRenderDispatcher().camera.getYRot();
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-camYaw));

        float spacing = 0.55f;
        int loops = 4;
        float maxTravel = n * loops + winner;
        float scroll = easeOutQuart(Math.min(1f, rp)) * maxTravel;
        int base = (int) Math.floor(scroll);
        float frac = scroll - base;
        boolean stopped = rp >= 1f;

        for (int k = -3; k <= 3; k++) {
            int idx = Math.floorMod(base + k, n);
            float off = (k - frac) * spacing;
            if (Math.abs(off) > 1.75f) continue;
            boolean center = Math.abs(off) < spacing * 0.45f;
            float scale = 0.66f - Math.abs(off) * 0.17f;
            if (stopped && center) {
                scale += pulse(fp, be.animTick, partial) * 0.5f;
            }
            float x = vertical ? 0f : off;
            float y = vertical ? off : 0f;
            float yaw = center ? (be.animTick + partial) * 2.0f : 0f;
            renderItem(be, cands.get(idx), pose, buffers, light, overlay, x, y, 0, Math.max(0.1f, scale), yaw);
        }
        pose.popPose();
    }

    // ------------------------------------------------------------------
    // Light beam (RenderType.lightning = additive glow, no texture)
    // ------------------------------------------------------------------

    private void renderBeam(CrateBlockEntity be, PoseStack pose, MultiBufferSource buffers, float partial) {
        float p = (be.animTick + partial) / Math.max(1, be.animTotal);
        float grow;
        if (p < CrateBlockEntity.P_OPEN_END) {
            grow = (p - CrateBlockEntity.P_ANTICIPATION_END)
                    / (CrateBlockEntity.P_OPEN_END - CrateBlockEntity.P_ANTICIPATION_END);
        } else if (p < 0.9f) {
            grow = 1f;
        } else {
            grow = 1f - (p - 0.9f) / 0.1f;
        }
        grow = Math.max(0f, Math.min(1f, grow));
        if (grow <= 0.01f) return;

        int color = be.getAnimColor();
        float rr = ((color >> 16) & 255) / 255f;
        float gg = ((color >> 8) & 255) / 255f;
        float bb = (color & 255) / 255f;
        float top = 0.4f + grow * 2.2f;
        float halfW = 0.10f + 0.03f * (float) Math.sin((be.animTick + partial) * 0.4f);

        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        float cx = 0.5f, cz = 0.5f, bottom = 0.4f;
        float a = 0.22f * grow;
        float[][] c = {
                { cx - halfW, cz - halfW }, { cx + halfW, cz - halfW },
                { cx + halfW, cz + halfW }, { cx - halfW, cz + halfW }
        };
        for (int i = 0; i < 4; i++) {
            float[] p1 = c[i];
            float[] p2 = c[(i + 1) % 4];
            // top alpha fades to 0 for a soft tip
            vert(vc, m, p1[0], bottom, p1[1], rr, gg, bb, a);
            vert(vc, m, p2[0], bottom, p2[1], rr, gg, bb, a);
            vert(vc, m, p2[0], top, p2[1], rr, gg, bb, 0f);
            vert(vc, m, p1[0], top, p1[1], rr, gg, bb, 0f);
        }
    }

    private static void vert(VertexConsumer vc, Matrix4f m, float x, float y, float z,
                             float r, float g, float b, float a) {
        vc.vertex(m, x, y, z).color(r, g, b, a).endVertex();
    }

    // ------------------------------------------------------------------
    // Item / holograms
    // ------------------------------------------------------------------

    private void renderItem(CrateBlockEntity be, ItemStack stack, PoseStack pose, MultiBufferSource buffers,
                            int light, int overlay, float x, float y, float z, float scale, float yaw) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                0xF000F0, overlay, pose, buffers, be.getLevel(), 0);
        pose.popPose();
    }

    private static float pulse(float fp, int tick, float partial) {
        if (fp <= 0f) return 0f;
        return (float) (Math.sin(fp * Math.PI * 2 + (tick + partial) * 0.4f) * 0.08f * (1f - fp));
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
        if (cfg.showOdds && !cfg.rewards.isEmpty()) {
            lines.add(Component.literal("\u00A77\u00A7l\u2014 Probabilidades \u2014"));
            int shown = 0;
            for (var rw : cfg.rewards) {
                if (shown >= 8) {
                    lines.add(Component.literal("\u00A78... y mas"));
                    break;
                }
                String pct = rw.guaranteed ? "\u00A7a100%" : "\u00A7f" + fmt1(cfg.normalizedPercent(rw)) + "%";
                lines.add(Component.literal("\u00A77" + trim(rw.describe(), 22) + " " + pct));
                shown++;
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

    private static float easeOutQuart(float t) { float x = 1f - t; return 1f - x * x * x * x; }

    private static String fmt1(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
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
        return 128;
    }
}
