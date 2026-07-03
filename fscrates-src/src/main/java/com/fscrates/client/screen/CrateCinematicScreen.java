package com.fscrates.client.screen;

import com.fscrates.block.CrateBlockEntity;
import com.fscrates.client.CinematicDiag;
import com.fscrates.client.render.CrateBakedModels;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.registry.ModRegistry;
import com.fscrates.util.CrateSfx;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CrateCinematicScreen
extends Screen {
    private static final int TOTAL = 300;
    private static final int LAND = 16;
    private static final int LID_START = 44;
    private static final int LID_END = 66;
    private static final int ROLL_START = 80;
    private static final int ROLL_END = 248;
    private static final int REVEAL = 254;
    private static final int REEL_EXTRA_LOOPS = 16;
    private static final float REEL_BREAK = 0.55f;
    private static final float LID_OPEN = 20.0f;
    private final CrateConfig cfg;
    private final int rarityColor;
    private final Rarity winnerRarity;
    private final int winnerIndex;
    private final List<ItemStack> candidates;
    private final int[] candidateRarities;
    private int ticks = 0;
    private boolean finished = false;
    private int[] reelStrip = null;
    private int reelLandingIndex = 0;
    private static boolean crateRenderFaulted = false;
    private int soundStage = 0;
    private int lastRiseTick = -100;
    private boolean peakPlayed = false;
    private int lastReelIndex = -1;
    private int winTick = -1;
    private boolean tailPlayed = false;
    private final CrateSfx.Sink sfxSink = (ev, vol, pitch) -> this.playUi(ev, pitch, vol);
    private boolean geomReady = false;
    private BakedModel cBase;
    private BakedModel cLid;
    private BlockState cState;
    private float cBaseScale;
    private float cCenterY;
    private float cScaledH;
    private float cPx;
    private float[] cHinge;
    private float cUnitPx;

    public CrateCinematicScreen(CrateConfig cfg, int rarityColor, int winnerRarity, int winnerIndex, List<ItemStack> candidates, int[] candidateRarities) {
        super((Component)Component.literal((String)"Cinem\u00e1tica de cofre"));
        this.cfg = cfg == null ? new CrateConfig() : cfg;
        this.rarityColor = rarityColor;
        Rarity[] rv = Rarity.values();
        this.winnerRarity = rv[Math.max(0, Math.min(rv.length - 1, winnerRarity))];
        this.candidates = candidates;
        this.winnerIndex = candidates == null || candidates.isEmpty() ? 0 : Math.max(0, Math.min(candidates.size() - 1, winnerIndex));
        this.candidateRarities = candidateRarities == null ? new int[]{} : candidateRarities;
    }

    public boolean isPauseScreen() {
        return false;
    }

    public void tick() {
        ++this.ticks;
        this.playAtmosphere();
        this.advanceRaritySounds();
        if (this.ticks >= 300 && !this.finished) {
            this.finished = true;
            this.onClose();
        }
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 32 || key == 257) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void playAtmosphere() {
        switch (this.ticks) {
            case 2: {
                this.playUi(SoundEvents.WITCH_THROW, 1.0f, 1.0f);
                break;
            }
            case 16: {
                this.playUi(SoundEvents.RAVAGER_STEP, 1.0f, 0.9f);
                break;
            }
            case 44: {
                this.playUi(SoundEvents.ENDER_CHEST_OPEN, 1.0f, 1.0f);
                break;
            }
            case 64: {
                this.playUi(SoundEvents.CHICKEN_EGG, 2.0f, 1.0f);
                break;
            }
        }
    }

    private void advanceRaritySounds() {
        float p;
        int interval;
        int t = this.ticks;
        Rarity buildupRarity = this.cfg.rarity;
        if (this.soundStage == 0 && t == 2) {
            CrateSfx.unlock(this.sfxSink, buildupRarity);
            this.soundStage = 1;
        }
        if (this.soundStage == 1 && t == 6) {
            CrateSfx.spiralCharge(this.sfxSink, buildupRarity);
        }
        if (this.soundStage == 1 && t > 6 && t < 44 && t - this.lastRiseTick >= (interval = Math.max(2, Math.round(10.0f - (p = Math.min(1.0f, (float)(t - 6) / (float)Math.max(1, 38))) * 8.0f)))) {
            this.lastRiseTick = t;
            CrateSfx.spiralRise(this.sfxSink, buildupRarity, p);
        }
        if (this.soundStage == 1 && !this.peakPlayed && t >= 44) {
            this.peakPlayed = true;
            CrateSfx.spiralPeak(this.sfxSink, buildupRarity);
        }
        if (this.soundStage == 1 && t >= 44) {
            CrateSfx.openAccent(this.sfxSink, buildupRarity);
            this.soundStage = 2;
        }
        // NOTA: el "tick" de la ruleta ya NO se dispara aca (tick() = 20Hz, no alcanzaba
        // a sonar cada item cuando la ruleta iba a >1 slot/tick -> se veia desincronizado).
        // Ahora se dispara en render() (144fps) por cada cruce real de item. Ver updateReelClicks().
        if (t >= 248 && this.soundStage >= 2 && this.soundStage < 60) {
            CrateSfx.win(this.sfxSink, this.winnerRarity);
            this.soundStage = 60;
            this.winTick = t;
        }
        if (this.soundStage == 60 && !this.tailPlayed && t - this.winTick >= 4) {
            this.tailPlayed = true;
            CrateSfx.winTail(this.sfxSink, this.winnerRarity);
        }
        if (t == 286) {
            this.playUi(SoundEvents.ITEM_PICKUP, 1.0f, 1.0f);
        }
    }

    private void playUi(SoundEvent ev, float pitch, float volume) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play((SoundInstance)SimpleSoundInstance.forUI((SoundEvent)ev, (float)pitch, (float)volume));
        }
    }

    // Dispara el "tick" de la ruleta por cada item que cruza el centro, en el frame EXACTO
    // (render corre a la tasa de FPS, no a 20Hz). Asi el sonido cuadra con el movimiento a
    // cualquier velocidad: rattle rapido en el giro, ticks cada vez mas lentos al frenar.
    private void updateReelClicks(float t) {
        if (this.candidates == null || this.candidates.isEmpty() || t < 80.0f || t >= 248.0f) {
            return;
        }
        float rp = Math.max(0.0f, Math.min(1.0f, (t - 80.0f) / 168.0f));
        int n = this.candidates.size();
        float maxTravel = this.reelTravelFast(n);
        int idx = (int)Math.floor(CrateCinematicScreen.reelPosFrac(rp) * maxTravel);
        if (this.lastReelIndex < 0) {
            this.lastReelIndex = idx;
            return;
        }
        if (idx > this.lastReelIndex) {
            this.lastReelIndex = idx;
            float pitch = 0.9f + rp * 0.7f;
            this.playUi((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.5f);
        }
    }

    private long dbgLastNanos = 0L;
    private double dbgFrameMs = 0.0;
    private double dbgCrateMs = 0.0;
    private double dbgReelMs = 0.0;
    private double dbgFxMs = 0.0;

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long dbgNow = System.nanoTime();
        if (this.dbgLastNanos != 0L) {
            double dms = (double)(dbgNow - this.dbgLastNanos) / 1000000.0;
            this.dbgFrameMs = this.dbgFrameMs <= 0.0 ? dms : this.dbgFrameMs * 0.9 + dms * 0.1;
        }
        this.dbgLastNanos = dbgNow;
        double dbgCrateRaw = 0.0;
        double dbgReelRaw = 0.0;
        double dbgFxRaw = 0.0;
        long dbgS;
        float t = (float)this.ticks + partialTick;
        int w = this.width;
        int h = this.height;
        int cx = w / 2;
        int cy = h / 2;
        int crateCY = cy + 26;
        int rouletteY = cy - 86;
        int glow = 0xFFFFFF & this.rarityColor;
        this.ensureGeom();
        this.updateReelClicks(t);
        g.fill(0, 0, w, h, -16317168);
        g.fillGradient(0, 0, w, h, -15069650, -16580087);
        g.fillGradient(0, 0, w, h / 3, 0x66000000, 0);
        g.fillGradient(0, h - h / 3, w, h, 0, 0x66000000);
        float glowA = Math.min(1.0f, Math.max(0.0f, (t - 32.0f) / 22.0f)) * 0.26f;
        glowA *= 0.85f + 0.15f * (float)Math.sin(t * 0.28f);
        if (t >= 254.0f) {
            glowA = Math.max(glowA, 0.26f * Math.max(0.0f, 1.0f - (t - 254.0f) / 26.0f));
        }
        int ga = (int)(glowA * 255.0f) << 24;
        g.fillGradient(cx - 170, crateCY - 140, cx + 170, crateCY + 110, ga | glow, 0 | glow);
        int shakeX = 0;
        int shakeY = 0;
        float amp = 0.0f;
        if (t >= 16.0f && t < 26.0f) {
            float d = t - 16.0f;
            amp = (1.0f - d / 10.0f) * 7.0f;
            shakeX = (int)(Math.sin(d * 2.7f) * (double)amp);
            shakeY = (int)(Math.cos(d * 3.3f) * (double)amp * 0.5);
        } else if (t >= 26.0f && t < 66.0f) {
            float rp = (t - 26.0f) / (float)Math.max(1, 40);
            amp = 0.5f + rp * 2.4f;
            shakeX = (int)(Math.sin(t * 1.9f) * (double)amp);
        }
        dbgS = System.nanoTime();
        if (t < 254.0f) {
            this.renderMouthGlow(g, cx, crateCY, t);
        }
        dbgFxRaw += (double)(System.nanoTime() - dbgS) / 1000000.0;
        dbgS = System.nanoTime();
        if (!crateRenderFaulted && t < 254.0f) {
            g.flush();
            try {
                this.renderCrate(g, cx + shakeX, crateCY + shakeY, t);
            }
            catch (Throwable err) {
                crateRenderFaulted = true;
                LogUtils.getLogger().error("[FSCrates] cinematic crate 3D render failed - disabling it for this session", err);
            }
        }
        dbgCrateRaw += (double)(System.nanoTime() - dbgS) / 1000000.0;
        dbgS = System.nanoTime();
        this.renderSparks(g, cx, crateCY, t);
        dbgFxRaw += (double)(System.nanoTime() - dbgS) / 1000000.0;
        dbgS = System.nanoTime();
        if (this.candidates != null && !this.candidates.isEmpty()) {
            if (t >= 80.0f && t < 254.0f) {
                this.renderRoulettePanel(g, cx, rouletteY, w, t);
                this.renderRoulette(g, cx, rouletteY, t);
            } else if (t >= 254.0f) {
                this.renderRevealBurst(g, cx, crateCY, t - 254.0f);
                this.renderShockwaveRing(g, cx, crateCY, t - 254.0f);
                this.renderReveal(g, cx, cy, t);
            }
        }
        dbgReelRaw += (double)(System.nanoTime() - dbgS) / 1000000.0;
        float barsP = Math.min(1.0f, t / 8.0f);
        if (t > 290.0f) {
            barsP = Math.max(0.0f, (300.0f - t) / 10.0f);
        }
        int barH = (int)((float)h * 0.12f * barsP);
        g.fill(0, 0, w, barH, -16777216);
        g.fill(0, h - barH, w, h, -16777216);
        g.drawCenteredString(this.font, "\u00a77[ESC] para saltar", cx, h - barH - 12, -1716868438);
        this.dbgCrateMs = this.dbgCrateMs * 0.9 + dbgCrateRaw * 0.1;
        this.dbgReelMs = this.dbgReelMs * 0.9 + dbgReelRaw * 0.1;
        this.dbgFxMs = this.dbgFxMs * 0.9 + dbgFxRaw * 0.1;
        this.renderDiagnostics(g, t);
    }

    private void renderDiagnostics(GuiGraphics g, float t) {
        Minecraft mc = Minecraft.getInstance();
        double fps = this.dbgFrameMs > 0.01 ? 1000.0 / this.dbgFrameMs : 0.0;
        long sinceCull = System.nanoTime() - CinematicDiag.lastCullNanos;
        boolean cullActive = CinematicDiag.lastCullNanos != 0L && sinceCull < 500000000L;
        String phase = t < 16.0f ? "caida" : (t < 66.0f ? "temblor/apertura" : (t < 80.0f ? "abriendo" : (t < 248.0f ? "giro" : (t < 254.0f ? "aterrizo" : "reveal"))));
        int reelItems = this.candidates == null ? 0 : Math.min(7, this.candidates.size());
        java.util.List<String> lines = new java.util.ArrayList<String>();
        lines.add("\u00a7e\u00a7lFSCrates DIAG \u00a77v2.7.5");
        lines.add(String.format("\u00a7fFPS: \u00a7a%.0f  \u00a77(%.1f ms/frame)", fps, this.dbgFrameMs));
        lines.add(cullActive ? "\u00a7aworld-cull: ON \u00a77(Mixin OK, frames=" + CinematicDiag.cullFrames + ")" : "\u00a7c\u00a7lworld-cull: OFF \u00a7r\u00a7c(el Mixin NO aplica!)");
        lines.add(String.format("\u00a7b cofre 3D: \u00a7f%5.2f ms", this.dbgCrateMs));
        lines.add(String.format("\u00a7b ruleta:   \u00a7f%5.2f ms \u00a77(%d items)", this.dbgReelMs, reelItems));
        lines.add(String.format("\u00a7b fx/part:  \u00a7f%5.2f ms", this.dbgFxMs));
        lines.add(String.format("\u00a77 tick=%d  fase=%s", (int)t, phase));
        lines.add(String.format("\u00a77 render=%dx%d  gui x%.1f", mc.getWindow().getWidth(), mc.getWindow().getHeight(), mc.getWindow().getGuiScale()));
        int bw = 0;
        for (String s : lines) {
            bw = Math.max(bw, this.font.width(s));
        }
        g.fill(4, 4, 4 + bw + 8, 8 + lines.size() * 10, 0xB0000000);
        int y = 8;
        for (String s : lines) {
            g.drawString(this.font, s, 8, y, -1);
            y += 10;
        }
    }

    private void renderMouthGlow(GuiGraphics g, int cx, int crateCY, float t) {
        float fade;
        if (t < 44.0f || this.cUnitPx <= 0.0f) {
            return;
        }
        float open = Math.min(1.0f, (t - 44.0f) / 12.0f);
        float f = fade = t >= 254.0f ? Math.max(0.0f, 1.0f - (t - 254.0f) / 10.0f) : 1.0f;
        if (fade <= 0.02f) {
            return;
        }
        int color = 0xFFFFFF & this.rarityColor;
        float mouthW = this.cUnitPx * 0.62f;
        int mouthY = crateCY - (int)(this.cUnitPx * 0.1f);
        float pulse = 0.82f + 0.18f * (float)Math.sin(t * 0.5f);
        float a = open * fade * pulse;
        int r1 = (int)(mouthW * open);
        g.fillGradient(cx - r1, mouthY - r1 / 2, cx + r1, mouthY + r1 / 3, (int)(a * 120.0f) << 24 | color, 0 | color);
        int r2 = (int)(mouthW * 0.5f * open);
        g.fillGradient(cx - r2, mouthY - r2 / 2, cx + r2, mouthY + r2 / 3, (int)(a * 200.0f) << 24 | 0xFFFFFF, 0xFFFFFF);
        // Resplandor suave extra que sube de la boca (reemplaza los "rayos" cuadrados
        // que se dibujaban con rectangulos y se veian toscos/pixelados).
        int r3 = (int)(mouthW * 1.45f * open);
        int gh = (int)(this.cUnitPx * 0.7f * open);
        g.fillGradient(cx - r3, mouthY - gh, cx + r3, mouthY + r3 / 4, 0 | color, (int)(a * 70.0f) << 24 | color);
        float spillH = this.cUnitPx * 0.85f;
        int count = 18;
        for (int i = 0; i < count; ++i) {
            float phase;
            float seed = (float)i * 7.13f + 2.0f;
            float rx = CrateCinematicScreen.frac((float)Math.sin(seed) * 43758.547f);
            float spd = 0.7f + CrateCinematicScreen.frac((float)Math.sin(seed + 1.1f) * 22578.11f) * 1.3f;
            float life = CrateCinematicScreen.frac((t - 44.0f) * 0.03f * spd + (phase = CrateCinematicScreen.frac((float)Math.sin(seed + 3.7f) * 13795.77f)));
            float pa = (1.0f - life) * open * fade;
            if (pa <= 0.03f) continue;
            float rise = life * spillH;
            float x = (float)cx + (rx - 0.5f) * (mouthW * 0.4f + rise * 0.9f);
            float y = (float)mouthY - rise;
            int alpha = (int)(pa * 230.0f) << 24;
            int sz = 1 + (i % 4 == 0 ? 1 : 0);
            int col = i % 4 == 0 ? 0xFFFFFF : color;
            g.fill((int)x, (int)y, (int)x + sz, (int)y + sz, alpha | col);
        }
    }

    private static void drawTriangleFill(GuiGraphics g, float ax, float ay, float bx, float by, float cx2, float cy2, int color) {
        int steps = 5;
        for (int i = 0; i < steps; ++i) {
            float t0 = (float)i / (float)steps;
            float t1 = (float)(i + 1) / (float)steps;
            float x0a = ax + (cx2 - ax) * t0;
            float y0a = ay + (cy2 - ay) * t0;
            float x0b = bx + (cx2 - bx) * t0;
            float y0b = by + (cy2 - by) * t0;
            float x1a = ax + (cx2 - ax) * t1;
            float y1a = ay + (cy2 - ay) * t1;
            float x1b = bx + (cx2 - bx) * t1;
            float y1b = by + (cy2 - by) * t1;
            int minX = (int)Math.floor(Math.min(Math.min(x0a, x0b), Math.min(x1a, x1b)));
            int maxX = (int)Math.ceil(Math.max(Math.max(x0a, x0b), Math.max(x1a, x1b)));
            int minY = (int)Math.floor(Math.min(Math.min(y0a, y0b), Math.min(y1a, y1b)));
            int maxY = (int)Math.ceil(Math.max(Math.max(y0a, y0b), Math.max(y1a, y1b)));
            if (maxX <= minX || maxY <= minY) continue;
            g.fill(minX, minY, maxX, maxY, color);
        }
    }

    private void renderRoulettePanel(GuiGraphics g, int cx, int cy, int w, float t) {
        float in = Math.min(1.0f, (t - 80.0f) / 6.0f);
        int half = (int)((float)w * 0.34f * in);
        int top = cy - 26;
        int bot = cy + 26;
        int rc = 0xFFFFFF & this.rarityColor;
        g.fill(cx - half, top, cx + half, bot, -435550443);
        g.fill(cx - half, top, cx + half, top + 2, 0xFF000000 | rc);
        g.fill(cx - half, bot - 2, cx + half, bot, 0xFF000000 | rc);
        g.fill(cx - 21, top, cx - 19, bot, 0xFF000000 | rc);
        g.fill(cx + 19, top, cx + 21, bot, 0xFF000000 | rc);
    }

    private void ensureGeom() {
        if (this.geomReady) {
            return;
        }
        this.geomReady = true;
        this.cBaseScale = CrateBakedModels.scaleFor(this.cfg) * Math.max(0.05f, this.cfg.sizeScale);
        this.cState = ((Block)ModRegistry.CRATE_BLOCK.get()).defaultBlockState();
        this.cBase = CrateBakedModels.baseModel(this.cfg);
        this.cLid = CrateBakedModels.lidModel(this.cfg);
        this.cHinge = CrateBakedModels.hingeFor(this.cfg);
        float[] yr = CrateCinematicScreen.modelYRange(this.cBase, this.cLid, this.cState);
        float rawCentre = (yr[0] + yr[1]) * 0.5f;
        float rawHeight = Math.max(0.1f, yr[1] - yr[0]);
        this.cCenterY = this.cBaseScale * rawCentre;
        this.cScaledH = this.cBaseScale * rawHeight;
        float target = (float)this.height * 0.3f;
        this.cPx = Math.max(1.0f, target / this.cScaledH);
        this.cUnitPx = this.cPx * this.cBaseScale;
    }

    private void renderCrate(GuiGraphics g, int cx, int cy, float t) {
        float lid;
        float dropUnits;
        Minecraft mc = Minecraft.getInstance();
        this.ensureGeom();
        float baseScale = this.cBaseScale;
        BlockState state = this.cState;
        BakedModel base = this.cBase;
        BakedModel lidModel = this.cLid;
        float centerY = this.cCenterY;
        float scaledH = this.cScaledH;
        float px = this.cPx;
        if (t < 16.0f) {
            float p = t / 16.0f;
            dropUnits = 2.0f * scaledH * (1.0f - p * p);
        } else {
            float b = t - 16.0f;
            dropUnits = (float)Math.abs(Math.sin((double)b * 0.5)) * 0.1f * scaledH * (float)Math.exp(-0.18 * (double)b);
        }
        if (t < 44.0f) {
            lid = 0.0f;
        } else if (t < 66.0f) {
            float p = (t - 44.0f) / 22.0f;
            lid = (1.0f - (1.0f - p) * (1.0f - p)) * 20.0f;
        } else {
            lid = 20.0f;
        }
        float yaw = 150.0f + t * 0.45f;
        float pitch = 22.0f;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate((double)cx, (double)cy, 250.0);
        pose.scale(px, -px, px);
        pose.translate(0.0f, dropUnits, 0.0f);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5f, -centerY, -0.5f);
        pose.translate(0.5f, 0.0f, 0.5f);
        pose.scale(baseScale, baseScale, baseScale);
        pose.translate(-0.5f, 0.0f, -0.5f);
        Lighting.setupForFlatItems();
        RenderSystem.enableDepthTest();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.cutout());
        ModelBlockRenderer mr = mc.getBlockRenderer().getModelRenderer();
        int fullBright = LightTexture.pack((int)15, (int)15);
        mr.renderModel(pose.last(), vc, state, base, 1.0f, 1.0f, 1.0f, fullBright, OverlayTexture.NO_OVERLAY);
        if (lidModel != null) {
            float[] hinge = this.cHinge;
            pose.pushPose();
            pose.translate(hinge[0], hinge[1], hinge[2]);
            pose.mulPose(Axis.XP.rotationDegrees(lid));
            pose.translate(-hinge[0], -hinge[1], -hinge[2]);
            mr.renderModel(pose.last(), vc, state, lidModel, 1.0f, 1.0f, 1.0f, fullBright, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buf.endBatch();
        pose.popPose();
        Lighting.setupFor3DItems();
    }

    private static float[] modelYRange(BakedModel base, BakedModel lid, BlockState state) {
        float[] yr = new float[]{Float.MAX_VALUE, -3.4028235E38f};
        CrateCinematicScreen.accumY(base, state, yr);
        CrateCinematicScreen.accumY(lid, state, yr);
        if (yr[0] > yr[1]) {
            return new float[]{0.0f, 1.0f};
        }
        return yr;
    }

    private static void accumY(BakedModel model, BlockState state, float[] yr) {
        if (model == null) {
            return;
        }
        RandomSource rnd = RandomSource.create((long)42L);
        ArrayList<BakedQuad> quads = new ArrayList<BakedQuad>(model.getQuads(state, null, rnd));
        for (Direction d : Direction.values()) {
            quads.addAll(model.getQuads(state, d, rnd));
        }
        for (BakedQuad q : quads) {
            int[] v = q.getVertices();
            int stride = v.length / 4;
            for (int i = 0; i < 4; ++i) {
                float y = Float.intBitsToFloat(v[i * stride + 1]);
                if (y < yr[0]) {
                    yr[0] = y;
                }
                if (!(y > yr[1])) continue;
                yr[1] = y;
            }
        }
    }

    private float reelTravelFast(int n) {
        // Recorrido total de la ruleta en "slots". Con la curva de abajo da una fase rapida
        // de ~29 slots/seg (rattle rapido) que frena hasta parar. El ganador ya NO se alinea
        // por modulo: se coloca explicitamente en reelStrip[reelLandingIndex] (ver
        // ensureReelStrip), asi que este valor solo define cuanto viaja/que tan rapido gira.
        return 190.0f;
    }

    // Genera el "strip" visible de la ruleta: una tira de items en orden VARIADO (aleatorio),
    // no en alternancia periodica. Con solo 2 premios, el orden D,S,D,S se veia estatico
    // (efecto rueda de carreta): al desplazarse un periodo se ve igual. Un orden variado
    // (con corridas aleatorias) hace el scroll CLARAMENTE visible = se siente fluido y rapido.
    // El ganador se coloca en la posicion exacta de aterrizaje para que quede centrado al final.
    private void ensureReelStrip() {
        if (this.reelStrip != null) {
            return;
        }
        int n = this.candidates == null ? 0 : this.candidates.size();
        if (n <= 0) {
            this.reelStrip = new int[0];
            return;
        }
        int landing = (int)Math.floor(this.reelTravelFast(n));
        int len = landing + 8;
        int[] strip = new int[len];
        java.util.Random rnd = new java.util.Random(0x5EEDL + (long)this.winnerIndex * 31L + (long)n * 131L);
        int prev = -1;
        for (int i = 0; i < len; ++i) {
            int v = rnd.nextInt(n);
            // permite repeticiones a veces (corridas) pero evita periodicidad perfecta:
            // si salio igual al anterior 2 veces seguidas, fuerza cambio.
            if (n > 1 && v == prev && rnd.nextInt(3) != 0) {
                v = (v + 1 + rnd.nextInt(n - 1)) % n;
            }
            strip[i] = v;
            prev = v;
        }
        int win = Math.max(0, Math.min(n - 1, this.winnerIndex));
        if (landing >= 0 && landing < len) {
            strip[landing] = win;
        }
        this.reelLandingIndex = landing;
        this.reelStrip = strip;
    }

    private static float reelPosFrac(float p) {
        p = Math.max(0.0f, Math.min(1.0f, p));
        // Gira rapido (velocidad casi constante) y frena con easeOutQuad -> desaceleracion
        // VISIBLE pero SIN arrastre: aterriza en el premio justo al final del giro (p=1 en
        // el tick 248) y deja un settle natural de ~6 ticks antes del reveal (254).
        // Nada de congelados largos que se sentian muertos.
        float fastTime = 0.62f;
        float fastShare = 0.80f;
        if (p <= fastTime) {
            return fastShare * (p / fastTime);
        }
        float local = (p - fastTime) / (1.0f - fastTime);
        float x = 1.0f - local;
        float easeOutQuad = 1.0f - x * x;
        return fastShare + (1.0f - fastShare) * easeOutQuad;
    }

    private void renderRoulette(GuiGraphics g, int cx, int cy, float t) {
        this.ensureReelStrip();
        if (this.reelStrip == null || this.reelStrip.length == 0) {
            return;
        }
        int n = this.candidates.size();
        int stripLen = this.reelStrip.length;
        float p = Math.max(0.0f, Math.min(1.0f, (t - 80.0f) / 168.0f));
        float maxTravel = this.reelTravelFast(n);
        float centerPos = CrateCinematicScreen.reelPosFrac(p) * maxTravel;
        int baseIdx = (int)Math.floor(centerPos);
        float frac = centerPos - (float)baseIdx;
        float slotW = 40.0f;
        float centerScale = 1.55f;
        PoseStack pose = g.pose();
        for (int k = -3; k <= 3; ++k) {
            int stripPos = Math.floorMod(baseIdx + k, stripLen);
            int idx = this.reelStrip[stripPos];
            float off = ((float)k - frac) * slotW;
            float dist = Math.abs(off) / slotW;
            float scale = centerScale * Math.max(0.0f, 1.0f - dist * 0.24f);
            if (scale < 0.35f) continue;
            ItemStack st = this.candidates.get(idx);
            if (st == null || st.isEmpty()) continue;
            // Posicion sub-pixel (float, sin Math.round) para movimiento fluido a 144fps;
            // el redondeo a pixel entero causaba pequenos escalones al desplazarse.
            pose.pushPose();
            pose.translate((float)cx + off, (float)cy, 0.0f);
            pose.scale(scale, scale, 1.0f);
            g.renderItem(st, -8, -8);
            pose.popPose();
        }
    }

    private void renderReveal(GuiGraphics g, int cx, int cy, float t) {
        ItemStack win = this.candidates.get(this.winnerIndex);
        if (win == null || win.isEmpty()) {
            return;
        }
        float since = t - 254.0f;
        float pop = Math.min(1.0f, since / 6.0f);
        float scale = 3.0f + (1.0f - (1.0f - pop) * (1.0f - pop)) * 1.4f;
        float flashA = Math.max(0.0f, 1.0f - since / 4.0f);
        if (flashA > 0.02f) {
            int fa = (int)(flashA * flashA * 235.0f) << 24;
            g.fill(0, 0, this.width, this.height, fa | 0xFFFFFF);
        }
        this.renderRevealBurst(g, cx, cy, since);
        int rc = 0xFFFFFF & this.winnerRarity.rgb();
        float ringA = Math.max(0.0f, 1.0f - since / 10.0f) * 0.6f;
        int ra = (int)(ringA * 255.0f) << 24;
        g.fillGradient(cx - 70, cy - 70, cx + 70, cy + 70, ra | rc, 0 | rc);
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate((float)cx, (float)cy, 0.0f);
        pose.scale(scale, scale, 1.0f);
        g.renderItem(win, -8, -8);
        pose.popPose();
        String name = win.getHoverName().getString();
        g.drawCenteredString(this.font, "\u00a7l" + name, cx, cy + 40, this.winnerRarity.rgb() | 0xFF000000);
        g.drawCenteredString(this.font, this.winnerRarity.color() + "Rareza " + this.winnerRarity.displayName(), cx, cy + 54, -1);
        g.drawCenteredString(this.font, "\u00a7a\u00a7lRECOMPENSA ENTREGADA", cx, cy + 72, -16777216);
    }

    private static float frac(float x) {
        return x - (float)Math.floor(x);
    }

    private static int color(int rgb) {
        return 0xFFFFFF & rgb;
    }

    private void renderSparks(GuiGraphics g, int cx, int cy, float t) {
        int color = 0xFFFFFF & this.rarityColor;
        int count = 14 + this.cfg.rarity.ordinal() * 6;
        for (int i = 0; i < count; ++i) {
            float phase;
            float seed = (float)i * 12.9898f + 4.233f;
            float rx = CrateCinematicScreen.frac((float)Math.sin(seed) * 43758.547f);
            float spd = 0.6f + CrateCinematicScreen.frac((float)Math.sin(seed + 1.7f) * 22578.11f) * 1.1f;
            float life = CrateCinematicScreen.frac(t * 0.01f * spd + (phase = CrateCinematicScreen.frac((float)Math.sin(seed + 5.3f) * 13795.77f)));
            float a = (float)Math.sin((double)life * Math.PI);
            if (a <= 0.03f) continue;
            float x = (float)cx + (rx - 0.5f) * 280.0f + (float)Math.sin((life + phase) * 6.2832f) * 10.0f;
            float y = (float)cy + 130.0f - life * 320.0f;
            int alpha = (int)(a * 210.0f) << 24;
            int sz = 2 + (i % 3 == 0 ? 1 : 0);
            g.fill((int)x, (int)y, (int)x + sz, (int)y + sz, alpha | color);
        }
    }

    private void renderShockwaveRing(GuiGraphics g, int cx, int cy, float since) {
        if (since > 14.0f) {
            return;
        }
        float ba = Math.max(0.0f, 1.0f - since / 14.0f);
        int bc = 0xFFFFFF & this.winnerRarity.rgb();
        float radius = since * 13.0f;
        int dots = 40;
        int alpha = (int)(ba * 200.0f) << 24;
        for (int i = 0; i < dots; ++i) {
            float ang = (float)((double)i * (Math.PI * 2 / (double)dots));
            float x = (float)cx + (float)Math.cos(ang) * radius;
            float y = (float)cy + (float)Math.sin(ang) * radius * 0.7f;
            g.fill((int)x - 1, (int)y - 1, (int)x + 1, (int)y + 1, alpha | 0xFFFFFF);
        }
        float radius2 = Math.max(0.0f, since - 3.0f) * 10.0f;
        if (radius2 > 0.0f) {
            float ba2 = Math.max(0.0f, 1.0f - (since - 3.0f) / 12.0f);
            int alpha2 = (int)(ba2 * 170.0f) << 24;
            for (int i = 0; i < dots; ++i) {
                float ang = (float)((double)i * (Math.PI * 2 / (double)dots));
                float x = (float)cx + (float)Math.cos(ang) * radius2;
                float y = (float)cy + (float)Math.sin(ang) * radius2 * 0.7f;
                g.fill((int)x - 1, (int)y - 1, (int)x + 1, (int)y + 1, alpha2 | bc);
            }
        }
    }

    private void renderRevealBurst(GuiGraphics g, int cx, int cy, float since) {
        int bc = 0xFFFFFF & this.winnerRarity.rgb();
        float ba = Math.max(0.0f, 1.0f - since / 12.0f);
        if (ba <= 0.02f) {
            return;
        }
        float br = since * 9.0f;
        int burst = 28;
        int alpha = (int)(ba * 220.0f) << 24;
        for (int i = 0; i < burst; ++i) {
            float ang = (float)((double)i * (Math.PI * 2 / (double)burst));
            float x = (float)cx + (float)Math.cos(ang) * br;
            float y = (float)cy + (float)Math.sin(ang) * br;
            g.fill((int)x - 1, (int)y - 1, (int)x + 2, (int)y + 2, alpha | bc);
        }
    }

    private static String stripAmp(String s) {
        return s == null ? "" : s.replace('&', '\u00a7');
    }
}

