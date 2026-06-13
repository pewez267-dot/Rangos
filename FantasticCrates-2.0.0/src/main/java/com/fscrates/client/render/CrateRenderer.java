// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.client.render;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.Property;
import com.fscrates.block.CrateBlock;
import net.minecraft.core.Direction;
import java.util.Locale;
import java.util.Iterator;
import net.minecraft.network.chat.FormattedText;
import com.fscrates.config.RewardEntry;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import java.util.List;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.fscrates.config.Rarity;
import com.fscrates.config.CrateConfig;
import net.minecraft.world.item.ItemStack;
import com.fscrates.animation.CrateAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import com.fscrates.block.CrateBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity>
{
    private static final ResourceLocation TEXTURE;
    private final CrateModel model;
    private final Font font;
    
    public CrateRenderer(final BlockEntityRendererProvider.Context ctx) {
        this.model = new CrateModel(ctx.bakeLayer(CrateModel.LAYER));
        this.font = ctx.getFont();
    }
    
    public void render(final CrateBlockEntity be, final float partialTick, final PoseStack pose, final MultiBufferSource buffers, final int light, final int overlay) {
        final CrateConfig cfg = be.getConfig();
        final Rarity rarity = cfg.rarity;
        final CrateAnimation anim = be.getAnimation();
        final CrateAnimation.Style style = anim.style();
        final float p = be.progress();
        final float rot = facingYRot(be);
        final float lidAngle = be.lidOpen(partialTick) * 1.5707964f;
        final float shake = be.shake(partialTick);
        final float hop = this.chestHop(be, partialTick);
        final float bob = (float)Math.sin((be.ambientTime + partialTick) * 0.1f) * 0.02f;
        final float sc = this.chestScale(be, partialTick);
        final float wob = this.chestWobble(be, partialTick);
        pose.pushPose();
        pose.translate(0.5, (double)(bob + hop), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-rot + 180.0f));
        if (wob != 0.0f) {
            pose.mulPose(Axis.ZP.rotationDegrees(wob));
        }
        pose.translate(shake, 0.0f, 0.0f);
        final float baseScale = CrateBakedModels.renderScale(rarity);
        pose.scale(sc * baseScale, sc * baseScale, sc * baseScale);
        pose.translate(-0.5, 0.0, -0.5);
        final VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
        final BlockState state = be.getBlockState();
        final ModelBlockRenderer modelRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        final int crateLight = 15728880;
        final BakedModel baseModel = CrateBakedModels.get(rarity);
        modelRenderer.renderModel(pose.last(), vc, state, baseModel, 1.0f, 1.0f, 1.0f, 15728880, overlay);
        final BakedModel lidModel = CrateBakedModels.getLid(rarity);
        final float[] h = CrateBakedModels.hinge(rarity);
        pose.pushPose();
        pose.translate(h[0], h[1], h[2]);
        pose.mulPose(Axis.XP.rotationDegrees(be.lidOpen(partialTick) * 100.0f));
        pose.translate(-h[0], -h[1], -h[2]);
        modelRenderer.renderModel(pose.last(), vc, state, lidModel, 1.0f, 1.0f, 1.0f, 15728880, overlay);
        pose.popPose();
        pose.popPose();
        if (be.animating && style != CrateAnimation.Style.INSTANT && be.animTick >= be.getSpiralEndTick()) {
            this.renderBeam(be, pose, buffers, partialTick);
        }
        if (be.animating && style != CrateAnimation.Style.INSTANT && be.animTick >= be.getOpenEndTick()) {
            this.renderReel(be, false, partialTick, pose, buffers, light, overlay);
        }
        else if (be.animating && style == CrateAnimation.Style.INSTANT && !be.getCandidates().isEmpty()) {
            final float camYaw = Minecraft.getInstance().getEntityRenderDispatcher().camera.getYRot();
            pose.pushPose();
            pose.translate(0.5, 1.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-camYaw));
            this.renderItem(be, be.getCandidates().get(be.getWinnerIndex()), pose, buffers, light, overlay, 0.0f, 0.0f, 0.0f, 0.9f, 0.0f);
            pose.popPose();
        }
        this.renderHolograms(be, cfg, rarity, pose, buffers, light);
    }
    
    private float chestHop(final CrateBlockEntity be, final float partial) {
        if (!be.animating || be.isInstant()) {
            return 0.0f;
        }
        final float t = be.animTick + partial;
        final int spiralEnd = be.getSpiralEndTick();
        final int openEnd = be.getOpenEndTick();
        final int holdEnd = be.getHoldEndTick();
        if (t < spiralEnd) {
            return 0.0f;
        }
        if (t < openEnd) {
            final float tt = (t - spiralEnd) / Math.max(1.0f, (float)(openEnd - spiralEnd));
            return (float)Math.sin(tt * 3.141592653589793) * 0.18f;
        }
        if (t < holdEnd) {
            return 0.04f + (float)Math.sin(t * 0.15f) * 0.015f;
        }
        return 0.0f;
    }
    
    private float chestScale(final CrateBlockEntity be, final float partial) {
        if (!be.animating) {
            return 1.0f;
        }
        final float t = be.animTick + partial;
        if (t < be.getSpiralEndTick()) {
            return 1.0f + (float)Math.sin(t * 1.6f) * 0.05f;
        }
        final float fp = be.finaleProgress(partial);
        if (fp > 0.0f) {
            return 1.0f + (float)Math.sin(fp * 3.141592653589793) * 0.18f;
        }
        return 1.0f + (float)Math.sin(t * 0.2f) * 0.02f;
    }
    
    private float chestWobble(final CrateBlockEntity be, final float partial) {
        if (!be.animating || be.isInstant()) {
            return 0.0f;
        }
        final float t = be.animTick + partial;
        final int spiralEnd = be.getSpiralEndTick();
        if (t >= spiralEnd) {
            return 0.0f;
        }
        final float intensity = (spiralEnd - t) / Math.max(1.0f, (float)spiralEnd);
        return (float)Math.sin(t * 2.0f) * 6.0f * intensity;
    }
    
    private void renderReel(final CrateBlockEntity be, final boolean vertical, final float partial, final PoseStack pose, final MultiBufferSource buffers, final int light, final int overlay) {
        final List<ItemStack> cands = be.getCandidates();
        if (cands.isEmpty()) {
            return;
        }
        final int n = cands.size();
        final int winner = Math.max(0, Math.min(n - 1, be.getWinnerIndex()));
        final float rp = be.revealProgress(partial);
        final float fp = be.finaleProgress(partial);
        final float cp = be.closeProgress(partial);
        final float ce = cp * cp;
        final float camYaw = Minecraft.getInstance().getEntityRenderDispatcher().camera.getYRot();
        pose.pushPose();
        pose.translate(0.5, 1.5 - ce * 1.05f, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-camYaw));
        final float spacing = 0.55f;
        final float maxTravel = CrateBlockEntity.reelTravel(n, winner);
        final float scroll = CrateBlockEntity.easeOutReel(Math.min(1.0f, rp)) * maxTravel;
        final boolean stopped = rp >= 1.0f;
        final int window = 7;
        if (n <= 7) {
            for (int i = 0; i < n; ++i) {
                final float d = wrapSigned(i - scroll, n);
                final float off = d * 0.55f * (1.0f - cp);
                final boolean center = Math.abs(d) < 0.5f;
                float scale = 0.66f - Math.abs(d) * 0.14f;
                if (stopped && center) {
                    scale += pulse(fp, be.animTick, partial) * 0.5f;
                }
                if (cp > 0.0f) {
                    scale *= (center ? (1.0f - 0.7f * ce) : Math.max(0.0f, 1.0f - 2.2f * cp));
                }
                if (scale > 0.02f) {
                    final float x = vertical ? 0.0f : off;
                    final float y = vertical ? off : 0.0f;
                    float yaw = center ? ((be.animTick + partial) * 2.0f) : 0.0f;
                    if (center && cp > 0.0f) {
                        yaw += ce * 360.0f;
                    }
                    this.renderItem(be, cands.get(i), pose, buffers, light, overlay, x, y, 0.0f, Math.max(0.02f, scale), yaw);
                }
            }
        }
        else {
            final int base = (int)Math.floor(scroll);
            final float frac = scroll - base;
            for (int k = -3; k <= 3; ++k) {
                final int idx = Math.floorMod(base + k, n);
                final float off2 = (k - frac) * 0.55f;
                if (Math.abs(off2) <= 1.75f) {
                    final boolean center2 = Math.abs(off2) < 0.2475f;
                    float scale2 = 0.66f - Math.abs(off2) * 0.17f;
                    if (stopped && center2) {
                        scale2 += pulse(fp, be.animTick, partial) * 0.5f;
                    }
                    final float off3 = off2 * (1.0f - cp);
                    if (cp > 0.0f) {
                        if (center2) {
                            scale2 *= 1.0f - 0.7f * ce;
                        }
                        else {
                            scale2 *= Math.max(0.0f, 1.0f - 2.2f * cp);
                        }
                    }
                    if (scale2 > 0.02f) {
                        final float x2 = vertical ? 0.0f : off3;
                        final float y2 = vertical ? off3 : 0.0f;
                        float yaw2 = center2 ? ((be.animTick + partial) * 2.0f) : 0.0f;
                        if (center2 && cp > 0.0f) {
                            yaw2 += ce * 360.0f;
                        }
                        this.renderItem(be, cands.get(idx), pose, buffers, light, overlay, x2, y2, 0.0f, Math.max(0.02f, scale2), yaw2);
                    }
                }
            }
        }
        final Matrix4f pm = pose.last().pose();
        final VertexConsumer pvc = buffers.getBuffer(RenderType.lightning());
        final float pw = 0.12f;
        final float yIn = 0.4f;
        final float yOut = 0.6f;
        final float pa = 0.95f * (1.0f - cp);
        triangle(pvc, pm, 0.0f, 0.4f, -0.12f, 0.6f, 0.12f, 0.6f, 1.0f, 1.0f, 1.0f, pa);
        triangle(pvc, pm, 0.0f, -0.4f, -0.12f, -0.6f, 0.12f, -0.6f, 1.0f, 1.0f, 1.0f, pa);
        pose.popPose();
    }
    
    private void renderBeam(final CrateBlockEntity be, final PoseStack pose, final MultiBufferSource buffers, final float partial) {
        final float t = be.animTick + partial;
        final int spiralEnd = be.getSpiralEndTick();
        final int openEnd = be.getOpenEndTick();
        final int holdEnd = be.getHoldEndTick();
        final int total = be.animTotal;
        float grow;
        if (t < spiralEnd) {
            grow = 0.0f;
        }
        else if (t < openEnd) {
            grow = (t - spiralEnd) / Math.max(1.0f, (float)(openEnd - spiralEnd));
        }
        else if (t < holdEnd) {
            grow = 1.0f;
        }
        else {
            grow = 1.0f - (t - holdEnd) / Math.max(1.0f, (float)(total - holdEnd));
        }
        grow = Math.max(0.0f, Math.min(1.0f, grow));
        if (grow <= 0.01f) {
            return;
        }
        int color = be.getAnimColor();
        final List<ItemStack> cands = be.getCandidates();
        final int[] rar = be.getCandidateRarities();
        if (!cands.isEmpty() && rar.length > 0) {
            final int n = cands.size();
            final int winner = Math.max(0, Math.min(n - 1, be.getWinnerIndex()));
            final float rp = be.revealProgress(partial);
            final float maxTravel = CrateBlockEntity.reelTravel(n, winner);
            final float scroll = CrateBlockEntity.easeOutReel(Math.min(1.0f, rp)) * maxTravel;
            final int centerIdx = Math.floorMod(Math.round(scroll), n);
            if (centerIdx < rar.length) {
                final Rarity[] rv = Rarity.values();
                color = rv[Math.max(0, Math.min(rv.length - 1, rar[centerIdx]))].rgb();
            }
        }
        final float rr = (color >> 16 & 0xFF) / 255.0f;
        final float gg = (color >> 8 & 0xFF) / 255.0f;
        final float bb = (color & 0xFF) / 255.0f;
        final float hr = rr + (1.0f - rr) * 0.5f;
        final float hg = gg + (1.0f - gg) * 0.5f;
        final float hb = bb + (1.0f - bb) * 0.5f;
        final VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        final Matrix4f m = pose.last().pose();
        final float cx = 0.5f;
        final float cz = 0.5f;
        final float bottom = 0.55f;
        final float top = 0.55f + grow * 0.62f;
        final float pulse = 0.03f * (float)Math.sin((be.animTick + partial) * 0.35f);
        final float halfBot = 0.3f + pulse;
        final float halfTop = 0.4f + pulse;
        beamCone(vc, m, 0.5f, 0.5f, halfBot, halfTop, 0.55f, top, rr, gg, bb, 0.55f * grow, 0.0f);
        beamCone(vc, m, 0.5f, 0.5f, halfBot * 0.7f, halfTop * 0.62f, 0.55f, top, rr, gg, bb, 0.78f * grow, 0.06f * grow);
        beamCone(vc, m, 0.5f, 0.5f, halfBot * 0.42f, halfTop * 0.34f, 0.55f, top, rr, gg, bb, 0.95f * grow, 0.12f * grow);
        beamCone(vc, m, 0.5f, 0.5f, halfBot * 0.2f, halfTop * 0.16f, 0.55f, top, hr, hg, hb, 0.9f * grow, 0.18f * grow);
        beamDisc(vc, m, 0.5f, 0.5f, halfBot * 1.1f, 0.56f, rr, gg, bb, 0.6f * grow);
        beamDisc(vc, m, 0.5f, 0.5f, halfBot * 0.55f, 0.57f, hr, hg, hb, 0.55f * grow);
    }
    
    private static void beamCone(final VertexConsumer vc, final Matrix4f m, final float cx, final float cz, final float halfBot, final float halfTop, final float bottom, final float top, final float r, final float g, final float b, final float aBot, final float aTop) {
        final float[][] cb = { { cx - halfBot, cz - halfBot }, { cx + halfBot, cz - halfBot }, { cx + halfBot, cz + halfBot }, { cx - halfBot, cz + halfBot } };
        final float[][] ct = { { cx - halfTop, cz - halfTop }, { cx + halfTop, cz - halfTop }, { cx + halfTop, cz + halfTop }, { cx - halfTop, cz + halfTop } };
        for (int i = 0; i < 4; ++i) {
            final float[] b2 = cb[i];
            final float[] b3 = cb[(i + 1) % 4];
            final float[] t2 = ct[i];
            final float[] t3 = ct[(i + 1) % 4];
            vert(vc, m, b2[0], bottom, b2[1], r, g, b, aBot);
            vert(vc, m, b3[0], bottom, b3[1], r, g, b, aBot);
            vert(vc, m, t3[0], top, t3[1], r, g, b, aTop);
            vert(vc, m, t2[0], top, t2[1], r, g, b, aTop);
        }
    }
    
    private static void beamDisc(final VertexConsumer vc, final Matrix4f m, final float cx, final float cz, final float half, final float y, final float r, final float g, final float b, final float a) {
        vert(vc, m, cx - half, y, cz - half, r, g, b, a);
        vert(vc, m, cx + half, y, cz - half, r, g, b, a);
        vert(vc, m, cx + half, y, cz + half, r, g, b, a);
        vert(vc, m, cx - half, y, cz + half, r, g, b, a);
    }
    
    private static void vert(final VertexConsumer vc, final Matrix4f m, final float x, final float y, final float z, final float r, final float g, final float b, final float a) {
        vc.vertex(m, x, y, z).color(r, g, b, a).endVertex();
    }
    
    private static void triangle(final VertexConsumer vc, final Matrix4f m, final float ax, final float ay, final float bx, final float by, final float cx, final float cy, final float r, final float g, final float b, final float a) {
        vert(vc, m, ax, ay, 0.0f, r, g, b, a);
        vert(vc, m, bx, by, 0.0f, r, g, b, a);
        vert(vc, m, cx, cy, 0.0f, r, g, b, a);
        vert(vc, m, cx, cy, 0.0f, r, g, b, a);
    }
    
    private void renderItem(final CrateBlockEntity be, final ItemStack stack, final PoseStack pose, final MultiBufferSource buffers, final int light, final int overlay, final float x, final float y, final float z, final float scale, final float yaw) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, 15728880, overlay, pose, buffers, be.getLevel(), 0);
        pose.popPose();
    }
    
    private static float wrapSigned(final float d, final int n) {
        float m = d % n;
        if (m < 0.0f) {
            m += n;
        }
        if (m > n / 2.0f) {
            m -= n;
        }
        return m;
    }
    
    private static float pulse(final float fp, final int tick, final float partial) {
        if (fp <= 0.0f) {
            return 0.0f;
        }
        return (float)(Math.sin(fp * 3.141592653589793 * 2.0 + (tick + partial) * 0.4f) * 0.07999999821186066 * (1.0f - fp));
    }
    
    private void renderHolograms(final CrateBlockEntity be, final CrateConfig cfg, final Rarity rarity, final PoseStack pose, final MultiBufferSource buffers, final int light) {
        final List<Component> lines = new ArrayList<Component>();
        if (cfg.floatingName && cfg.displayName != null && !cfg.displayName.isEmpty()) {
            lines.add((Component)Component.literal(colorize(cfg.displayName)).withStyle(rarity.color()));
        }
        for (final String l : cfg.floatingText) {
            if (l != null && !l.isEmpty()) {
                lines.add((Component)Component.literal(colorize(l)));
            }
        }
        if (cfg.showOdds && !cfg.rewards.isEmpty()) {
            lines.add((Component)Component.literal("§7§l\u2014 Probabilidades \u2014"));
            int shown = 0;
            for (RewardEntry rw : cfg.rewards) {
                if (shown >= 8) {
                    lines.add((Component)Component.literal("§8... y más"));
                    break;
                }
                final String pct = rw.guaranteed ? "§a100%" : ("§f" + fmt1(cfg.normalizedPercent(rw)));
                lines.add((Component)Component.literal("§7" + trim(rw.describe(), 22) + " " + pct));
                ++shown;
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        final float baseY = be.animating ? 2.45f : 1.4f;
        final float lineH = 0.26f;
        for (int i = 0; i < lines.size(); ++i) {
            final Component line = lines.get(i);
            pose.pushPose();
            pose.translate(0.5, (double)(baseY + (lines.size() - 1 - i) * 0.26f), 0.5);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(-0.025f, -0.025f, 0.025f);
            final Matrix4f mat = pose.last().pose();
            final float bgOpacity = mc.options.getBackgroundOpacity(0.25f);
            final int bg = (int)(bgOpacity * 255.0f) << 24;
            final float x = -this.font.width((FormattedText)line) / 2.0f;
            this.font.drawInBatch(line, x, 0.0f, -1, false, mat, buffers, Font.DisplayMode.SEE_THROUGH, bg, light);
            this.font.drawInBatch(line, x, 0.0f, -1, false, mat, buffers, Font.DisplayMode.NORMAL, 0, light);
            pose.popPose();
        }
    }
    
    private static String colorize(final String s) {
        if (s == null || s.indexOf(38) < 0) {
            return s;
        }
        final char[] c = s.toCharArray();
        for (int i = 0; i < c.length - 1; ++i) {
            if (c[i] == '&' && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(c[i + 1]) >= 0) {
                c[i] = '§';
            }
        }
        return new String(c);
    }
    
    private static String fmt1(final double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }
    
    private static String trim(final String s, final int max) {
        if (s == null) {
            return "";
        }
        return (s.length() <= max) ? s : s.substring(0, max - 1);
    }
    
    private static float facingYRot(final CrateBlockEntity be) {
        try {
            final Direction d = (Direction)be.getBlockState().getValue((Property)CrateBlock.FACING);
            return d.toYRot();
        }
        catch (final Exception e) {
            return 0.0f;
        }
    }
    
    public boolean shouldRenderOffScreen(final CrateBlockEntity be) {
        return true;
    }
    
    public int getViewDistance() {
        return 128;
    }
    
    static {
        TEXTURE = new ResourceLocation("fscrates", "textures/entity/crate/crate.png");
    }
}
