package com.fscrates.client.render;

import com.fscrates.animation.CrateAnimation;
import com.fscrates.block.CrateBlock;
import com.fscrates.block.CrateBlockEntity;
import com.fscrates.client.render.CrateBakedModels;
import com.fscrates.client.render.CrateModel;
import com.fscrates.client.render.CrateStyles;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.config.RewardEntry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class CrateRenderer
implements BlockEntityRenderer<CrateBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("fscrates", "textures/entity/crate/crate.png");
    private final CrateModel model;
    private final Font font;
    // Cache del centro XZ (en unidades de bloque 0..1) de cada modelo base, para AUTO-CENTRAR
    // cualquier crate en el punto central del bloque sin importar como este autorada su
    // geometria (algunos modelos venian descentrados; ver footprintCenter).
    private static final java.util.Map<BakedModel, float[]> CENTER_CACHE = new java.util.IdentityHashMap<BakedModel, float[]>();

    public CrateRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new CrateModel(ctx.bakeLayer(CrateModel.LAYER));
        this.font = ctx.getFont();
    }

    public void render(CrateBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        CrateConfig cfg = be.getConfig();
        Rarity rarity = cfg.rarity;
        CrateAnimation anim = be.getAnimation();
        CrateAnimation.Style style = anim.style();
        float p = be.progress();
        float rot = CrateRenderer.facingYRot(be);
        float lidAngle = be.lidOpen(partialTick) * 1.5707964f;
        float shake = be.shake(partialTick);
        float hop = this.chestHop(be, partialTick);
        float bob = (float)Math.sin((be.ambientTime + partialTick) * 0.1f) * 0.02f;
        float sc = this.chestScale(be, partialTick);
        float wob = this.chestWobble(be, partialTick);
        pose.pushPose();
        pose.translate(0.5, (double)(bob + hop + cfg.yOffset), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-rot + 180.0f + cfg.yawOffset));
        if (wob != 0.0f) {
            pose.mulPose(Axis.ZP.rotationDegrees(wob));
        }
        pose.translate(shake, 0.0f, 0.0f);
        float baseScale = CrateBakedModels.scaleFor(cfg) * Math.max(0.05f, cfg.sizeScale);
        float cineY = this.cineStretchY(be, partialTick);
        float cineXZ = 1.0f / (float)Math.sqrt(cineY);
        float S = sc * baseScale;
        pose.scale(S * cineXZ, S * cineY, S * cineXZ);
        VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
        BlockState state = be.getBlockState();
        ModelBlockRenderer modelRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        int crateLight = 0xF000F0;
        BakedModel baseModel = CrateBakedModels.baseModel(cfg);
        // AUTO-CENTRADO: en vez de asumir que el modelo esta centrado en (0.5,0.5) (lo que
        // dejaba varias crates descentradas en su bloque), se calcula el centro real del
        // footprint XZ del modelo base y se traslada por -ese centro, de modo que TODAS las
        // crates queden centradas en el punto central del bloque (como la dorada). La tapa
        // usa el mismo frame, asi que se mantiene alineada con la base.
        float[] ctr = CrateRenderer.footprintCenter(baseModel);
        pose.translate(-ctr[0], 0.0, -ctr[1]);
        modelRenderer.renderModel(pose.last(), vc, state, baseModel, 1.0f, 1.0f, 1.0f, 0xF000F0, overlay);
        BakedModel lidModel = CrateBakedModels.lidModel(cfg);
        if (lidModel != null) {
            float[] h = CrateBakedModels.hingeFor(cfg);
            // Los cofres del pack "cine" (scene crates) tienen la geometria/UV de la tapa
            // rotada 180 respecto al resto: con XP(+angulo) sobre la bisagra natural abren
            // la tapa HACIA ATRAS (queja del usuario) pese a tener la cara al frente. Para
            // ellos se refleja el pivote en Z (1 - h[2]) y se NIEGA el angulo, igual que en
            // la cinematica, para que la tapa abra hacia el jugador. Las demas no se tocan.
            CrateStyles.Style st = CrateStyles.get(cfg.styleId);
            boolean cine = st != null && st.isCinematic();
            float lidDeg = be.lidOpen(partialTick) * 100.0f;
            float pivotZ = cine ? 1.0f - h[2] : h[2];
            float lidRot = cine ? -lidDeg : lidDeg;
            pose.pushPose();
            pose.translate(h[0], h[1], pivotZ);
            pose.mulPose(Axis.XP.rotationDegrees(lidRot));
            pose.translate(-h[0], -h[1], -pivotZ);
            modelRenderer.renderModel(pose.last(), vc, state, lidModel, 1.0f, 1.0f, 1.0f, 0xF000F0, overlay);
            pose.popPose();
        }
        pose.popPose();
        float szc = Math.max(0.05f, cfg.sizeScale);
        float crateTop = cfg.yOffset + 1.15f * szc;
        if (be.animating && !be.sceneLidMode && style != CrateAnimation.Style.INSTANT && be.animTick >= be.getSpiralEndTick()) {
            this.renderBeam(be, pose, buffers, partialTick, cfg.yOffset, szc);
        }
        if (be.animating && !be.sceneLidMode && style != CrateAnimation.Style.INSTANT && be.animTick >= be.getOpenEndTick()) {
            this.renderReel(be, false, partialTick, pose, buffers, light, overlay, crateTop, szc);
        } else if (be.animating && style == CrateAnimation.Style.INSTANT && !be.getCandidates().isEmpty()) {
            float camYaw = Minecraft.getInstance().getEntityRenderDispatcher().camera.getYRot();
            pose.pushPose();
            pose.translate(0.5, (double)(crateTop + 0.35f), 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-camYaw));
            pose.scale(szc, szc, szc);
            this.renderItem(be, be.getCandidates().get(be.getWinnerIndex()), pose, buffers, light, overlay, 0.0f, 0.0f, 0.0f, 0.9f, 0.0f);
            pose.popPose();
        }
        this.renderHolograms(be, cfg, rarity, pose, buffers, light, crateTop);
    }

    private float chestHop(CrateBlockEntity be, float partial) {
        if (!be.animating) {
            return 0.0f;
        }
        float t = (float)be.animTick + partial;
        float openStart = be.getSpiralEndTick();
        float u = t - openStart;
        if (u < -8.0f) {
            return 0.0f;
        }
        if (u < 0.0f) {
            float a = (u + 8.0f) / 8.0f;
            return -0.05f * a;
        }
        float JUMP_TICKS = 11.0f;
        if (u < JUMP_TICKS) {
            float p = u / JUMP_TICKS;
            return (float)Math.sin((double)p * Math.PI) * 0.85f;
        }
        float d = (u - JUMP_TICKS) / 10.0f;
        float env = (float)Math.exp(-3.0f * d);
        return (float)Math.abs(Math.sin((double)d * 3.0 * Math.PI)) * 0.12f * env;
    }

    private float chestScale(CrateBlockEntity be, float partial) {
        if (!be.animating) {
            return 1.0f;
        }
        float t = (float)be.animTick + partial;
        if (t < (float)be.getSpiralEndTick()) {
            return 1.0f + (float)Math.sin(t * 1.6f) * 0.05f;
        }
        float fp = be.finaleProgress(partial);
        return fp > 0.0f ? 1.0f + (float)Math.sin((double)fp * Math.PI) * 0.18f : 1.0f + (float)Math.sin(t * 0.2f) * 0.02f;
    }

    private float cineStretchY(CrateBlockEntity be, float partial) {
        if (!be.animating) {
            return 1.0f;
        }
        float t = (float)be.animTick + partial;
        float openStart = be.getSpiralEndTick();
        float u = t - openStart;
        if (u < -8.0f) {
            return 1.0f;
        }
        if (u < 0.0f) {
            float a = (u + 8.0f) / 8.0f;
            return 1.0f - 0.16f * a;
        }
        float d = u / 9.0f;
        float env = (float)Math.exp(-2.0f * d);
        float osc = (float)Math.sin(Math.PI * (0.5 + (double)(d * 2.0f)));
        return 1.0f + 0.3f * env * osc;
    }

    private float chestWobble(CrateBlockEntity be, float partial) {
        if (be.animating && !be.isInstant()) {
            float t = (float)be.animTick + partial;
            int spiralEnd = be.getSpiralEndTick();
            if (t >= (float)spiralEnd) {
                return 0.0f;
            }
            float intensity = ((float)spiralEnd - t) / Math.max(1.0f, (float)spiralEnd);
            return (float)Math.sin(t * 2.0f) * 6.0f * intensity;
        }
        return 0.0f;
    }

    private void renderReel(CrateBlockEntity be, boolean vertical, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float crateTop, float sizeScale) {
        List<ItemStack> cands = be.getCandidates();
        if (!cands.isEmpty()) {
            int n = cands.size();
            int winner = Math.max(0, Math.min(n - 1, be.getWinnerIndex()));
            float rp = be.revealProgress(partial);
            float fp = be.finaleProgress(partial);
            float cp = be.closeProgress(partial);
            float ce = cp * cp;
            float camYaw = Minecraft.getInstance().getEntityRenderDispatcher().camera.getYRot();
            pose.pushPose();
            pose.translate(0.5, (double)(crateTop + 0.35f) - (double)(ce * 1.05f * sizeScale), 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-camYaw));
            pose.scale(sizeScale, sizeScale, sizeScale);
            float spacing = 0.55f;
            float maxTravel = CrateBlockEntity.reelTravel(n, winner);
            float scroll = CrateBlockEntity.easeOutReel(Math.min(1.0f, rp)) * maxTravel;
            boolean stopped = rp >= 1.0f;
            int window = 7;
            if (n <= 7) {
                for (int i = 0; i < n; ++i) {
                    float yaw;
                    float d = CrateRenderer.wrapSigned((float)i - scroll, n);
                    float off = d * 0.55f * (1.0f - cp);
                    boolean center = Math.abs(d) < 0.5f;
                    float scale = 0.66f - Math.abs(d) * 0.14f;
                    if (stopped && center) {
                        scale += CrateRenderer.pulse(fp, be.animTick, partial) * 0.5f;
                    }
                    if (cp > 0.0f) {
                        scale *= center ? 1.0f - 0.7f * ce : Math.max(0.0f, 1.0f - 2.2f * cp);
                    }
                    if (!(scale > 0.02f)) continue;
                    float x = vertical ? 0.0f : off;
                    float y = vertical ? off : 0.0f;
                    float f = yaw = center ? ((float)be.animTick + partial) * 2.0f : 0.0f;
                    if (center && cp > 0.0f) {
                        yaw += ce * 360.0f;
                    }
                    this.renderItem(be, cands.get(i), pose, buffers, light, overlay, x, y, 0.0f, Math.max(0.02f, scale), yaw);
                }
            } else {
                int base = (int)Math.floor(scroll);
                float frac = scroll - (float)base;
                for (int k = -3; k <= 3; ++k) {
                    float yaw2;
                    int idx = Math.floorMod(base + k, n);
                    float off2 = ((float)k - frac) * 0.55f;
                    if (!(Math.abs(off2) <= 1.75f)) continue;
                    boolean center2 = Math.abs(off2) < 0.2475f;
                    float scale2 = 0.66f - Math.abs(off2) * 0.17f;
                    if (stopped && center2) {
                        scale2 += CrateRenderer.pulse(fp, be.animTick, partial) * 0.5f;
                    }
                    float off3 = off2 * (1.0f - cp);
                    if (cp > 0.0f) {
                        scale2 = center2 ? (scale2 *= 1.0f - 0.7f * ce) : (scale2 *= Math.max(0.0f, 1.0f - 2.2f * cp));
                    }
                    if (!(scale2 > 0.02f)) continue;
                    float x2 = vertical ? 0.0f : off3;
                    float y2 = vertical ? off3 : 0.0f;
                    float f = yaw2 = center2 ? ((float)be.animTick + partial) * 2.0f : 0.0f;
                    if (center2 && cp > 0.0f) {
                        yaw2 += ce * 360.0f;
                    }
                    this.renderItem(be, cands.get(idx), pose, buffers, light, overlay, x2, y2, 0.0f, Math.max(0.02f, scale2), yaw2);
                }
            }
            Matrix4f pm = pose.last().pose();
            VertexConsumer pvc = buffers.getBuffer(RenderType.lightning());
            float pw = 0.12f;
            float yIn = 0.4f;
            float yOut = 0.6f;
            float pa = 0.95f * (1.0f - cp);
            CrateRenderer.triangle(pvc, pm, 0.0f, 0.4f, -0.12f, 0.6f, 0.12f, 0.6f, 1.0f, 1.0f, 1.0f, pa);
            CrateRenderer.triangle(pvc, pm, 0.0f, -0.4f, -0.12f, -0.6f, 0.12f, -0.6f, 1.0f, 1.0f, 1.0f, pa);
            pose.popPose();
        }
    }

    private void renderBeam(CrateBlockEntity be, PoseStack pose, MultiBufferSource buffers, float partial, float yOff, float sizeScale) {
        float t = (float)be.animTick + partial;
        int spiralEnd = be.getSpiralEndTick();
        int openEnd = be.getOpenEndTick();
        int holdEnd = be.getHoldEndTick();
        int total = be.animTotal;
        float grow = t < (float)spiralEnd ? 0.0f : (t < (float)openEnd ? (t - (float)spiralEnd) / Math.max(1.0f, (float)(openEnd - spiralEnd)) : (t < (float)holdEnd ? 1.0f : 1.0f - (t - (float)holdEnd) / Math.max(1.0f, (float)(total - holdEnd))));
        if (!((grow = Math.max(0.0f, Math.min(1.0f, grow))) <= 0.01f)) {
            int color = be.getAnimColor();
            List<ItemStack> cands = be.getCandidates();
            int[] rar = be.getCandidateRarities();
            if (!cands.isEmpty() && rar.length > 0) {
                int n = cands.size();
                int winner = Math.max(0, Math.min(n - 1, be.getWinnerIndex()));
                float rp = be.revealProgress(partial);
                float maxTravel = CrateBlockEntity.reelTravel(n, winner);
                float scroll = CrateBlockEntity.easeOutReel(Math.min(1.0f, rp)) * maxTravel;
                int centerIdx = Math.floorMod(Math.round(scroll), n);
                if (centerIdx < rar.length) {
                    Rarity[] rv = Rarity.values();
                    color = rv[Math.max(0, Math.min(rv.length - 1, rar[centerIdx]))].rgb();
                }
            }
            float rr = (float)(color >> 16 & 0xFF) / 255.0f;
            float gg = (float)(color >> 8 & 0xFF) / 255.0f;
            float bb = (float)(color & 0xFF) / 255.0f;
            float hr = rr + (1.0f - rr) * 0.5f;
            float hg = gg + (1.0f - gg) * 0.5f;
            float hb = bb + (1.0f - bb) * 0.5f;
            VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
            pose.pushPose();
            pose.translate(0.5, (double)yOff, 0.5);
            pose.scale(sizeScale, sizeScale, sizeScale);
            pose.translate(-0.5, 0.0, -0.5);
            Matrix4f m = pose.last().pose();
            float cx = 0.5f;
            float cz = 0.5f;
            float bottom = 0.55f;
            float top = 0.55f + grow * 0.62f;
            float pulse = 0.03f * (float)Math.sin(((float)be.animTick + partial) * 0.35f);
            float halfBot = 0.3f + pulse;
            float halfTop = 0.4f + pulse;
            CrateRenderer.beamCone(vc, m, 0.5f, 0.5f, halfBot, halfTop, 0.55f, top, rr, gg, bb, 0.55f * grow, 0.0f);
            CrateRenderer.beamCone(vc, m, 0.5f, 0.5f, halfBot * 0.7f, halfTop * 0.62f, 0.55f, top, rr, gg, bb, 0.78f * grow, 0.06f * grow);
            CrateRenderer.beamCone(vc, m, 0.5f, 0.5f, halfBot * 0.42f, halfTop * 0.34f, 0.55f, top, rr, gg, bb, 0.95f * grow, 0.12f * grow);
            CrateRenderer.beamCone(vc, m, 0.5f, 0.5f, halfBot * 0.2f, halfTop * 0.16f, 0.55f, top, hr, hg, hb, 0.9f * grow, 0.18f * grow);
            CrateRenderer.beamDisc(vc, m, 0.5f, 0.5f, halfBot * 1.1f, 0.56f, rr, gg, bb, 0.6f * grow);
            CrateRenderer.beamDisc(vc, m, 0.5f, 0.5f, halfBot * 0.55f, 0.57f, hr, hg, hb, 0.55f * grow);
            pose.popPose();
        }
    }

    private static void beamCone(VertexConsumer vc, Matrix4f m, float cx, float cz, float halfBot, float halfTop, float bottom, float top, float r, float g, float b, float aBot, float aTop) {
        float[][] cb = new float[][]{{cx - halfBot, cz - halfBot}, {cx + halfBot, cz - halfBot}, {cx + halfBot, cz + halfBot}, {cx - halfBot, cz + halfBot}};
        float[][] ct = new float[][]{{cx - halfTop, cz - halfTop}, {cx + halfTop, cz - halfTop}, {cx + halfTop, cz + halfTop}, {cx - halfTop, cz + halfTop}};
        for (int i = 0; i < 4; ++i) {
            float[] b2 = cb[i];
            float[] b3 = cb[(i + 1) % 4];
            float[] t2 = ct[i];
            float[] t3 = ct[(i + 1) % 4];
            CrateRenderer.vert(vc, m, b2[0], bottom, b2[1], r, g, b, aBot);
            CrateRenderer.vert(vc, m, b3[0], bottom, b3[1], r, g, b, aBot);
            CrateRenderer.vert(vc, m, t3[0], top, t3[1], r, g, b, aTop);
            CrateRenderer.vert(vc, m, t2[0], top, t2[1], r, g, b, aTop);
        }
    }

    private static void beamDisc(VertexConsumer vc, Matrix4f m, float cx, float cz, float half, float y, float r, float g, float b, float a) {
        CrateRenderer.vert(vc, m, cx - half, y, cz - half, r, g, b, a);
        CrateRenderer.vert(vc, m, cx + half, y, cz - half, r, g, b, a);
        CrateRenderer.vert(vc, m, cx + half, y, cz + half, r, g, b, a);
        CrateRenderer.vert(vc, m, cx - half, y, cz + half, r, g, b, a);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
        vc.vertex(m, x, y, z).color(r, g, b, a).endVertex();
    }

    private static void triangle(VertexConsumer vc, Matrix4f m, float ax, float ay, float bx, float by, float cx, float cy, float r, float g, float b, float a) {
        CrateRenderer.vert(vc, m, ax, ay, 0.0f, r, g, b, a);
        CrateRenderer.vert(vc, m, bx, by, 0.0f, r, g, b, a);
        CrateRenderer.vert(vc, m, cx, cy, 0.0f, r, g, b, a);
        CrateRenderer.vert(vc, m, cx, cy, 0.0f, r, g, b, a);
    }

    private void renderItem(CrateBlockEntity be, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float x, float y, float z, float scale, float yaw) {
        if (stack != null && !stack.isEmpty()) {
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.scale(scale, scale, scale);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, 0xF000F0, overlay, pose, buffers, be.getLevel(), 0);
            pose.popPose();
        }
    }

    private static float wrapSigned(float d, int n) {
        float m = d % (float)n;
        if (m < 0.0f) {
            m += (float)n;
        }
        if (m > (float)n / 2.0f) {
            m -= (float)n;
        }
        return m;
    }

    private static float pulse(float fp, int tick, float partial) {
        return fp <= 0.0f ? 0.0f : (float)(Math.sin((double)fp * Math.PI * 2.0 + (double)(((float)tick + partial) * 0.4f)) * (double)0.08f * (double)(1.0f - fp));
    }

    private void renderHolograms(CrateBlockEntity be, CrateConfig cfg, Rarity rarity, PoseStack pose, MultiBufferSource buffers, int light, float crateTop) {
        Vec3 camPos = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
        if (be.getBlockPos().getCenter().distanceToSqr(camPos) > 576.0) {
            return;
        }
        ArrayList<MutableComponent> lines = new ArrayList<MutableComponent>();
        if (cfg.floatingName && cfg.displayName != null && !cfg.displayName.isEmpty()) {
            lines.add(Component.literal((String)CrateRenderer.colorize(cfg.displayName)).withStyle(rarity.color()));
        }
        for (String string : cfg.floatingText) {
            if (string == null || string.isEmpty()) continue;
            lines.add(Component.literal((String)CrateRenderer.colorize(string)));
        }
        if (cfg.showOdds && !cfg.rewards.isEmpty()) {
            lines.add(Component.literal((String)"\u00a77\u00a7l\u2014 Probabilidades \u2014"));
            int shown = 0;
            for (RewardEntry rw : cfg.rewards) {
                if (shown >= 8) {
                    lines.add(Component.literal((String)"\u00a78... y m\u00e1s"));
                    break;
                }
                String pct = rw.guaranteed ? "\u00a7a100%" : "\u00a7f" + CrateRenderer.fmt1(cfg.normalizedPercent(rw));
                lines.add(Component.literal((String)("\u00a77" + CrateRenderer.trim(rw.describe(), 22) + " " + pct)));
                ++shown;
            }
        }
        if (!lines.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            float f = be.animating ? Math.max(2.45f, crateTop + 1.3f) : Math.max(1.4f, crateTop + 0.25f);
            float lineH = 0.26f;
            for (int i = 0; i < lines.size(); ++i) {
                Component line = (Component)lines.get(i);
                pose.pushPose();
                pose.translate(0.5, (double)(f + (float)(lines.size() - 1 - i) * 0.26f), 0.5);
                pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                pose.scale(-0.025f, -0.025f, 0.025f);
                Matrix4f mat = pose.last().pose();
                float bgOpacity = mc.options.getBackgroundOpacity(0.25f);
                int bg = (int)(bgOpacity * 255.0f) << 24;
                float x = (float)(-this.font.width((FormattedText)line)) / 2.0f;
                this.font.drawInBatch(line, x, 0.0f, -1, false, mat, buffers, Font.DisplayMode.NORMAL, bg, light);
                pose.popPose();
            }
        }
    }

    private static String colorize(String s) {
        if (s != null && s.indexOf(38) >= 0) {
            char[] c = s.toCharArray();
            for (int i = 0; i < c.length - 1; ++i) {
                if (c[i] != '&' || "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(c[i + 1]) < 0) continue;
                c[i] = 167;
            }
            return new String(c);
        }
        return s;
    }

    private static String fmt1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1);
    }

    // Centro XZ (unidades de bloque 0..1) del footprint del modelo base, calculado del
    // bounding box de sus quads y cacheado por identidad de modelo. Para centrar cualquier
    // crate en el bloque sin depender de como este autorada la geometria.
    private static float[] footprintCenter(BakedModel base) {
        if (base == null) {
            return new float[]{0.5f, 0.5f};
        }
        float[] cached = CENTER_CACHE.get(base);
        if (cached != null) {
            return cached;
        }
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create(42L);
        Direction[] sides = new Direction[]{null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (Direction side : sides) {
            for (net.minecraft.client.renderer.block.model.BakedQuad q : base.getQuads(null, side, rnd)) {
                int[] verts = q.getVertices();
                int stride = verts.length / 4;
                for (int i = 0; i < 4; ++i) {
                    float x = Float.intBitsToFloat(verts[i * stride]);
                    float z = Float.intBitsToFloat(verts[i * stride + 2]);
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (z < minZ) minZ = z;
                    if (z > maxZ) maxZ = z;
                }
            }
        }
        float[] r = minX > maxX ? new float[]{0.5f, 0.5f} : new float[]{(minX + maxX) * 0.5f, (minZ + maxZ) * 0.5f};
        CENTER_CACHE.put(base, r);
        return r;
    }

    private static float facingYRot(CrateBlockEntity be) {
        try {
            Direction d = (Direction)be.getBlockState().getValue((Property)CrateBlock.FACING);
            return d.toYRot();
        }
        catch (Exception var2) {
            return 0.0f;
        }
    }

    public boolean shouldRenderOffScreen(CrateBlockEntity be) {
        return true;
    }

    public int getViewDistance() {
        return 64;
    }
}

