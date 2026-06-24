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
        // spin eliminado: el cofre ya no gira sobre su eje al abrirse.
        final float sc = this.chestScale(be, partialTick);
        final float wob = this.chestWobble(be, partialTick);
        pose.pushPose();
        // Posicion: centro horizontal del bloque. La Y arranca en el SUELO (0)
        // en lugar del centro del bloque (0.5), de modo que el cofre queda
        // apoyado sobre el piso aunque lo agrandemos por rareza.
        pose.translate(0.5, bob + hop, 0.5);
        // +180: los modelos de Blockbench miran a +Z; el bloque/renderer usa la
        // formula de cofre vanilla (-rot), por lo que sin este offset salen
        // colocados al reves (la cara frontal apuntaba hacia atras).
        // spin eliminado: se quitó chestSpin para que el cofre no gire al abrir.
        pose.mulPose(Axis.YP.rotationDegrees(-rot + 180.0f));
        if (wob != 0.0f) {
            pose.mulPose(Axis.ZP.rotationDegrees(wob));
        }
        pose.translate(shake, 0.0f, 0.0f);
        // Escala base por rareza (legendary y mythic son mas grandes) multiplicada
        // por la escala animada.
        final float baseScale = CrateBakedModels.renderScale(rarity);
        pose.scale(sc * baseScale, sc * baseScale, sc * baseScale);
        // Anclaje al suelo: solo desplazamos X/Z para centrar el modelo; la Y se
        // deja en 0 para que la base del modelo (y=0 del JSON) descanse sobre el
        // piso. Antes se restaba 0.5 en Y, lo que al escalar hundia el cofre.
        pose.translate(-0.5, 0.0, -0.5);
        // Modelo 3D por rareza (Crates and Stuff Model Pack). Sin tinte: el quad
        // no lleva tintindex, asi que se ven los colores reales de la textura.
        final VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
        final BlockState state = be.getBlockState();
        final var modelRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        // 1) Cuerpo (base), estatico.
        final BakedModel baseModel = CrateBakedModels.get(rarity);
        modelRenderer.renderModel(pose.last(), vc, state, baseModel, 1.0f, 1.0f, 1.0f, light, overlay);
        // 2) Tapa (lid), rotada segun lidOpen() alrededor de su bisagra.
        final BakedModel lidModel = CrateBakedModels.getLid(rarity);
        final float[] h = CrateBakedModels.hinge(rarity);
        pose.pushPose();
        pose.translate(h[0], h[1], h[2]);
        pose.mulPose(Axis.XP.rotationDegrees(be.lidOpen(partialTick) * CrateBakedModels.OPEN_ANGLE_DEG));
        pose.translate(-h[0], -h[1], -h[2]);
        modelRenderer.renderModel(pose.last(), vc, state, lidModel, 1.0f, 1.0f, 1.0f, light, overlay);
        pose.popPose();
        pose.popPose();
        if (be.animating && style != CrateAnimation.Style.INSTANT && p >= 0.1f) {
            // Luz de faro con el COLOR DE LA RAREZA del item ganado. Sale al abrir
            // la tapa, en TODAS las animaciones (antes solo en ciertos themes).
            this.renderBeam(be, pose, buffers, partialTick);
        }
        if (be.animating && style != CrateAnimation.Style.INSTANT && p >= 0.22f) {
            // Ruleta SIEMPRE horizontal (se elimino el modo vertical/tragamonedas).
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
        final float camYaw = Minecraft.getInstance().getEntityRenderDispatcher().camera.getYRot();
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-camYaw));
        final float spacing = 0.55f;
        // Distancia ~fija (no depende del numero de items) -> misma velocidad
        // con pool grande o pequeno. Identico a la formula del sonido.
        final float maxTravel = CrateBlockEntity.reelTravel(n, winner);
        final float scroll = CrateBlockEntity.easeOutReel(Math.min(1.0f, rp)) * maxTravel;
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
        // Puntero/indicador en el CENTRO (tipo ruleta real): dos flechitas blancas
        // que enmarcan el item que esta pasando por el centro.
        final Matrix4f pm = pose.last().pose();
        final VertexConsumer pvc = buffers.getBuffer(RenderType.lightning());
        final float pw = 0.12f;
        final float yIn = 0.40f;
        final float yOut = 0.60f;
        triangle(pvc, pm, 0.0f, yIn, -pw, yOut, pw, yOut, 1.0f, 1.0f, 1.0f, 0.95f);   // arriba, apunta abajo
        triangle(pvc, pm, 0.0f, -yIn, -pw, -yOut, pw, -yOut, 1.0f, 1.0f, 1.0f, 0.95f); // abajo, apunta arriba
        pose.popPose();
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
        // El color del haz CAMBIA con el item que esta pasando por el centro de la
        // ruleta (no se queda fijo en el color del premio, asi no "spoilea" la
        // rareza ganadora). Al frenar, queda en el color del item premiado.
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
        final float top = 0.4f + grow * 2.8f;
        final float halfW = 0.14f + 0.04f * (float)Math.sin((be.animTick + partial) * 0.4f);
        final VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        final Matrix4f m = pose.last().pose();
        final float cx = 0.5f;
        final float cz = 0.5f;
        final float bottom = 0.4f;
        // Haz con MAS FUERZA de color: un nucleo interior vivo + un glow exterior.
        // Antes el alpha (0.34) lo dejaba muy palido/translucido.
        // Glow exterior (suave, da volumen).
        beamColumn(vc, m, cx, cz, halfW, bottom, top, rr, gg, bb, 0.6f * grow, 0.12f * grow);
        // Nucleo interior mas estrecho y MUCHO mas vivo (le da fuerza al color sin
        // ensanchar el haz ni convertirlo en un pilar solido).
        beamColumn(vc, m, cx, cz, halfW * 0.5f, bottom, top, rr, gg, bb, Math.min(1.0f, 0.95f * grow), Math.min(1.0f, 0.25f * grow));
    }

    /** Dibuja una "columna" de haz (4 caras) con alpha en la base y en la punta. */
    private static void beamColumn(final VertexConsumer vc, final Matrix4f m, final float cx, final float cz, final float halfW, final float bottom, final float top, final float r, final float g, final float b, final float aBot, final float aTop) {
        final float[][] c = { { cx - halfW, cz - halfW }, { cx + halfW, cz - halfW }, { cx + halfW, cz + halfW }, { cx - halfW, cz + halfW } };
        for (int i = 0; i < 4; ++i) {
            final float[] p2 = c[i];
            final float[] p3 = c[(i + 1) % 4];
            vert(vc, m, p2[0], bottom, p2[1], r, g, b, aBot);
            vert(vc, m, p3[0], bottom, p3[1], r, g, b, aBot);
            vert(vc, m, p3[0], top, p3[1], r, g, b, aTop);
            vert(vc, m, p2[0], top, p2[1], r, g, b, aTop);
        }
    }
    
    private static void vert(final VertexConsumer vc, final Matrix4f m, final float x, final float y, final float z, final float r, final float g, final float b, final float a) {
        vc.vertex(m, x, y, z).color(r, g, b, a).endVertex();
    }

    /** Dibuja un triangulo (como quad degenerado) en el plano z=0 para el puntero. */
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
                    lines.add((Component)Component.literal("§8... y mas"));
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
            pose.translate(0.5, (double)(baseY + (lines.size() - 1 - i) * lineH), 0.5);
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
