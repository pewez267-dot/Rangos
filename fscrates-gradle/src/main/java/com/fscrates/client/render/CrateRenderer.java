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
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.fscrates.config.Rarity;
import com.fscrates.config.CrateConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import com.fscrates.animation.CrateAnimation;
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
        this.model = new CrateModel(ctx.m_173582_(CrateModel.LAYER));
        this.font = ctx.m_173586_();
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
        final float spin = this.chestSpin(be, partialTick);
        final float sc = this.chestScale(be, partialTick);
        final float wob = this.chestWobble(be, partialTick);
        pose.m_85836_();
        pose.m_85837_(0.5, 0.5 + bob + hop, 0.5);
        pose.m_252781_(Axis.f_252436_.m_252977_(-rot + spin));
        if (wob != 0.0f) {
            pose.m_252781_(Axis.f_252403_.m_252977_(wob));
        }
        pose.m_252880_(shake, 0.0f, 0.0f);
        pose.m_85841_(sc, sc, sc);
        pose.m_85837_(-0.5, -0.5, -0.5);
        // Modelo 3D por rareza (Crates and Stuff Model Pack). Sin tinte: el quad
        // no lleva tintindex, asi que se ven los colores reales de la textura.
        final BakedModel baked = CrateBakedModels.get(rarity);
        final VertexConsumer vc = buffers.m_6299_(RenderType.m_110463_());
        final BlockState state = be.m_58900_();
        Minecraft.m_91087_().m_91289_().m_110937_().m_111067_(pose.m_85850_(), vc, state, baked, 1.0f, 1.0f, 1.0f, light, overlay);
        pose.m_85849_();
        if (be.animating && anim.hasBeam() && p >= 0.1f) {
            this.renderBeam(be, pose, buffers, partialTick);
        }
        if (be.animating && style != CrateAnimation.Style.INSTANT && p >= 0.22f) {
            this.renderReel(be, style == CrateAnimation.Style.SLOT_MACHINE, partialTick, pose, buffers, light, overlay);
        }
        else if (be.animating && style == CrateAnimation.Style.INSTANT && !be.getCandidates().isEmpty()) {
            final float camYaw = Minecraft.m_91087_().m_91290_().f_114358_.m_90590_();
            pose.m_85836_();
            pose.m_85837_(0.5, 1.5, 0.5);
            pose.m_252781_(Axis.f_252436_.m_252977_(-camYaw));
            this.renderItem(be, be.getCandidates().get(be.getWinnerIndex()), pose, buffers, light, overlay, 0.0f, 0.0f, 0.0f, 0.9f, 0.0f);
            pose.m_85849_();
        }
        this.renderHolograms(be, cfg, rarity, pose, buffers, light);
    }
    
    private float chestHop(final CrateBlockEntity be, final float partial) {
        if (!be.animating) {
            return 0.0f;
        }
        final float p = (be.animTick + partial) / Math.max(1, be.animTotal);
        if (p < 0.1f) {
            return 0.0f;
        }
        if (p < 0.22f) {
            final float t = (p - 0.1f) / 0.12f;
            return (float)Math.sin(t * 3.141592653589793) * 0.18f;
        }
        if (p < 0.94f) {
            return 0.04f + (float)Math.sin((be.animTick + partial) * 0.15f) * 0.015f;
        }
        return 0.0f;
    }
    
    private float chestSpin(final CrateBlockEntity be, final float partial) {
        if (!be.animating) {
            return 0.0f;
        }
        final float p = (be.animTick + partial) / Math.max(1, be.animTotal);
        if (p < 0.22f) {
            return 0.0f;
        }
        if (p < 0.88f) {
            return (be.animTick + partial) * 3.0f;
        }
        return (be.animTick + partial) * 9.0f;
    }
    
    private float chestScale(final CrateBlockEntity be, final float partial) {
        if (!be.animating) {
            return 1.0f;
        }
        final float p = (be.animTick + partial) / Math.max(1, be.animTotal);
        if (p < 0.1f) {
            return 1.0f + (float)Math.sin((be.animTick + partial) * 1.6f) * 0.05f;
        }
        final float fp = be.finaleProgress(partial);
        if (fp > 0.0f) {
            return 1.0f + (float)Math.sin(fp * 3.141592653589793) * 0.18f;
        }
        return 1.0f + (float)Math.sin((be.animTick + partial) * 0.2f) * 0.02f;
    }
    
    private float chestWobble(final CrateBlockEntity be, final float partial) {
        if (!be.animating) {
            return 0.0f;
        }
        final float p = (be.animTick + partial) / Math.max(1, be.animTotal);
        if (p >= 0.1f) {
            return 0.0f;
        }
        final float intensity = (0.1f - p) / 0.1f;
        return (float)Math.sin((be.animTick + partial) * 2.0f) * 6.0f * intensity;
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
        final float camYaw = Minecraft.m_91087_().m_91290_().f_114358_.m_90590_();
        pose.m_85836_();
        pose.m_85837_(0.5, 1.5, 0.5);
        pose.m_252781_(Axis.f_252436_.m_252977_(-camYaw));
        final float spacing = 0.55f;
        final int loops = 4;
        final float maxTravel = (float)(n * loops + winner);
        final float scroll = easeOutQuart(Math.min(1.0f, rp)) * maxTravel;
        final int base = (int)Math.floor(scroll);
        final float frac = scroll - base;
        final boolean stopped = rp >= 1.0f;
        for (int k = -3; k <= 3; ++k) {
            final int idx = Math.floorMod(base + k, n);
            final float off = (k - frac) * spacing;
            if (Math.abs(off) <= 1.75f) {
                final boolean center = Math.abs(off) < spacing * 0.45f;
                float scale = 0.66f - Math.abs(off) * 0.17f;
                if (stopped && center) {
                    scale += pulse(fp, be.animTick, partial) * 0.5f;
                }
                final float x = vertical ? 0.0f : off;
                final float y = vertical ? off : 0.0f;
                final float yaw = center ? ((be.animTick + partial) * 2.0f) : 0.0f;
                this.renderItem(be, cands.get(idx), pose, buffers, light, overlay, x, y, 0.0f, Math.max(0.1f, scale), yaw);
            }
        }
        pose.m_85849_();
    }
    
    private void renderBeam(final CrateBlockEntity be, final PoseStack pose, final MultiBufferSource buffers, final float partial) {
        final float p = (be.animTick + partial) / Math.max(1, be.animTotal);
        float grow;
        if (p < 0.22f) {
            grow = (p - 0.1f) / 0.12f;
        }
        else if (p < 0.9f) {
            grow = 1.0f;
        }
        else {
            grow = 1.0f - (p - 0.9f) / 0.1f;
        }
        grow = Math.max(0.0f, Math.min(1.0f, grow));
        if (grow <= 0.01f) {
            return;
        }
        final int color = be.getAnimColor();
        final float rr = (color >> 16 & 0xFF) / 255.0f;
        final float gg = (color >> 8 & 0xFF) / 255.0f;
        final float bb = (color & 0xFF) / 255.0f;
        final float top = 0.4f + grow * 2.2f;
        final float halfW = 0.1f + 0.03f * (float)Math.sin((be.animTick + partial) * 0.4f);
        final VertexConsumer vc = buffers.m_6299_(RenderType.m_110502_());
        final Matrix4f m = pose.m_85850_().m_252922_();
        final float cx = 0.5f;
        final float cz = 0.5f;
        final float bottom = 0.4f;
        final float a = 0.22f * grow;
        final float[][] c = { { cx - halfW, cz - halfW }, { cx + halfW, cz - halfW }, { cx + halfW, cz + halfW }, { cx - halfW, cz + halfW } };
        for (int i = 0; i < 4; ++i) {
            final float[] p2 = c[i];
            final float[] p3 = c[(i + 1) % 4];
            vert(vc, m, p2[0], bottom, p2[1], rr, gg, bb, a);
            vert(vc, m, p3[0], bottom, p3[1], rr, gg, bb, a);
            vert(vc, m, p3[0], top, p3[1], rr, gg, bb, 0.0f);
            vert(vc, m, p2[0], top, p2[1], rr, gg, bb, 0.0f);
        }
    }
    
    private static void vert(final VertexConsumer vc, final Matrix4f m, final float x, final float y, final float z, final float r, final float g, final float b, final float a) {
        vc.m_252986_(m, x, y, z).m_85950_(r, g, b, a).m_5752_();
    }
    
    private void renderItem(final CrateBlockEntity be, final ItemStack stack, final PoseStack pose, final MultiBufferSource buffers, final int light, final int overlay, final float x, final float y, final float z, final float scale, final float yaw) {
        if (stack == null || stack.m_41619_()) {
            return;
        }
        pose.m_85836_();
        pose.m_252880_(x, y, z);
        pose.m_252781_(Axis.f_252436_.m_252977_(yaw));
        pose.m_85841_(scale, scale, scale);
        Minecraft.m_91087_().m_91291_().m_269128_(stack, ItemDisplayContext.FIXED, 15728880, overlay, pose, buffers, be.m_58904_(), 0);
        pose.m_85849_();
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
            lines.add((Component)Component.m_237113_(colorize(cfg.displayName)).m_130940_(rarity.color()));
        }
        for (final String l : cfg.floatingText) {
            if (l != null && !l.isEmpty()) {
                lines.add((Component)Component.m_237113_(colorize(l)));
            }
        }
        if (cfg.showOdds && !cfg.rewards.isEmpty()) {
            lines.add((Component)Component.m_237113_("§7§l\u2014 Probabilidades \u2014"));
            int shown = 0;
            for (RewardEntry rw : cfg.rewards) {
                if (shown >= 8) {
                    lines.add((Component)Component.m_237113_("§8... y mas"));
                    break;
                }
                final String pct = rw.guaranteed ? "§a100%" : ("§f" + fmt1(cfg.normalizedPercent(rw)));
                lines.add((Component)Component.m_237113_("§7" + trim(rw.describe(), 22) + " " + pct));
                ++shown;
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        final Minecraft mc = Minecraft.m_91087_();
        final float baseY = be.animating ? 2.45f : 1.4f;
        final float lineH = 0.26f;
        for (int i = 0; i < lines.size(); ++i) {
            final Component line = lines.get(i);
            pose.m_85836_();
            pose.m_85837_(0.5, (double)(baseY + (lines.size() - 1 - i) * lineH), 0.5);
            pose.m_252781_(mc.m_91290_().m_253208_());
            pose.m_85841_(-0.025f, -0.025f, 0.025f);
            final Matrix4f mat = pose.m_85850_().m_252922_();
            final float bgOpacity = mc.f_91066_.m_92141_(0.25f);
            final int bg = (int)(bgOpacity * 255.0f) << 24;
            final float x = -this.font.m_92852_((FormattedText)line) / 2.0f;
            this.font.m_272077_(line, x, 0.0f, -1, false, mat, buffers, Font.DisplayMode.SEE_THROUGH, bg, light);
            this.font.m_272077_(line, x, 0.0f, -1, false, mat, buffers, Font.DisplayMode.NORMAL, 0, light);
            pose.m_85849_();
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
    
    private static float easeOutQuart(final float t) {
        final float x = 1.0f - t;
        return 1.0f - x * x * x * x;
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
            final Direction d = (Direction)be.m_58900_().m_61143_((Property)CrateBlock.FACING);
            return d.m_122435_();
        }
        catch (final Exception e) {
            return 0.0f;
        }
    }
    
    public boolean shouldRenderOffScreen(final CrateBlockEntity be) {
        return true;
    }
    
    public int m_142163_() {
        return 128;
    }
    
    static {
        TEXTURE = new ResourceLocation("fscrates", "textures/entity/crate/crate.png");
    }
}
