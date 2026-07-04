package com.fscrates.client.screen;

import com.fscrates.block.CrateBlockEntity;
import com.fscrates.client.CinematicDiag;
import com.fscrates.client.render.CrateBakedModels;
import com.fscrates.client.render.CrateStyles;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.registry.ModRegistry;
import com.fscrates.util.CrateSfx;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation GLOW_TEX = new ResourceLocation("fscrates", "textures/gui/glow.png");
    private static final int TOTAL = 360;
    private static final int LAND = 30;
    private static final int LID_START = 46;
    private static final int LID_END = 78;
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
    // Solo las cajas del pack "W6 - Cinematic Crates" (estilos registrados via
    // CrateStyles.regCine: cine_common/rare/epic/legendary/mythical/ultimate) necesitan
    // +180 grados extra de yaw. El resto de crates (clasicas, dedou, greek, etc.) ya
    // tienen la orientacion correcta verificada y NO se deben tocar.
    private boolean cIsCineStyle = false;
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
        this.lastTickNanos = System.nanoTime();
        this.playAtmosphere();
        this.advanceRaritySounds();
        if (this.ticks >= TOTAL && !this.finished) {
            this.finished = true;
            this.onClose();
        }
    }

    // Solo un OPERADOR (permiso nivel 2) puede saltar la escena. Para el resto la
    // cinematica es obligatoria: no se puede cerrar con ESC/SPACE/ENTER.
    private boolean canSkip() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.hasPermissions(2);
    }

    public boolean shouldCloseOnEsc() {
        return this.canSkip();
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if ((key == 32 || key == 257 || key == 256) && this.canSkip()) {
            this.onClose();
            return true;
        }
        if (key == 256) {
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void playAtmosphere() {
        // Cues ambientales independientes de la rareza, atados a los beats fisicos de
        // la escena (aparicion -> caida con peso -> creak de la tapa -> asentamiento).
        switch (this.ticks) {
            case 2: {
                // Whoosh de aparicion: el cofre "cae desde la nada".
                this.playUi(SoundEvents.WITCH_THROW, 1.0f, 1.0f);
                break;
            }
            case 30: {
                // Impacto de aterrizaje con PESO pero MAGICO (sin clang de yunque, que el
                // usuario rechazo): paso pesado grave + golpe sordo profundo en capas. Se
                // sube un pelin el volumen/gravedad de las dos capas restantes para que el
                // "thud" siga sintiendose pesado sin el metalico del anvil.
                this.playUi(SoundEvents.RAVAGER_STEP, 1.0f, 0.78f);
                this.playUi(SoundEvents.GENERIC_BIG_FALL, 0.75f, 0.72f);
                break;
            }
            case 44: {
                // La tapa empieza a ceder: creak magico + madera real debajo.
                this.playUi(SoundEvents.ENDER_CHEST_OPEN, 1.0f, 1.0f);
                this.playUi(SoundEvents.WOOD_HIT, 0.4f, 0.7f);
                break;
            }
            case 64: {
                // Destello suave a mitad de la apertura de la tapa (la luz empieza a
                // notarse mas fuerte): antes usaba un sonido de gallina fuera de tema.
                this.playUi(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.7f);
                this.playUi(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.35f, 1.9f);
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
        if (this.soundStage == 1 && t > 6 && t < 46 && t - this.lastRiseTick >= (interval = Math.max(2, Math.round(10.0f - (p = Math.min(1.0f, (float)(t - 6) / (float)Math.max(1, 40))) * 8.0f)))) {
            this.lastRiseTick = t;
            CrateSfx.spiralRise(this.sfxSink, buildupRarity, p);
        }
        if (this.soundStage == 1 && !this.peakPlayed && t >= 46) {
            this.peakPlayed = true;
            CrateSfx.spiralPeak(this.sfxSink, buildupRarity);
        }
        if (this.soundStage == 1 && t >= 46) {
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
    private long lastTickNanos = 0L;
    private long openNanos = 0L;
    private float dbgPassedPT = 0.0f;
    private float dbgRealPT = 0.0f;
    private double dbgMaxFrameMs = 0.0;
    private float dbgTickDelta = 0.0f;

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long dbgNow = System.nanoTime();
        if (this.openNanos == 0L) {
            this.openNanos = dbgNow;
        }
        if (this.dbgLastNanos != 0L) {
            double dms = (double)(dbgNow - this.dbgLastNanos) / 1000000.0;
            this.dbgFrameMs = this.dbgFrameMs <= 0.0 ? dms : this.dbgFrameMs * 0.9 + dms * 0.1;
            // "peak hold" del peor frame: sube al instante con un pico, baja despacio.
            // Si aca aparece 30-60ms mientras el FPS promedio dice 144 -> hay STUTTER real.
            this.dbgMaxFrameMs = dms > this.dbgMaxFrameMs ? dms : this.dbgMaxFrameMs * 0.96 + dms * 0.04;
        }
        this.dbgLastNanos = dbgNow;
        double dbgCrateRaw = 0.0;
        double dbgReelRaw = 0.0;
        double dbgFxRaw = 0.0;
        long dbgS;
        // RELOJ DE TIEMPO REAL PURO, desacoplado del reloj de ticks del juego.
        // El reel/escena antes iban atados a this.ticks (+partial). Si el tick loop del
        // cliente tartamudea (comun en modpacks: entidades/chunks hacen hipos aunque los
        // FPS esten altos), la animacion HEREDA ese tartamudeo -> se siente de 20-30fps.
        // Con nanoTime desde que abrio la escena, el movimiento es fluido a los FPS reales
        // sin importar lo que haga el tick loop. (50ms = 1 tick de animacion.)
        float t = (float)((double)(dbgNow - this.openNanos) / 50000000.0);
        this.dbgPassedPT = partialTick;
        this.dbgRealPT = t - (float)Math.floor(t);
        this.dbgTickDelta = t - (float)this.ticks;
        int w = this.width;
        int h = this.height;
        int cx = w / 2;
        int cy = h / 2;
        int crateCY = cy + 26;
        int rouletteY = cy - 86;
        this.ensureGeom();
        this.updateReelClicks(t);
        this.renderSceneBackground(g, w, h, cx, crateCY, t);
        int shakeX = 0;
        int shakeY = 0;
        float amp = 0.0f;
        if (t >= 30.0f && t < 40.0f) {
            float d = t - 30.0f;
            amp = (1.0f - d / 10.0f) * 8.0f;
            shakeX = (int)(Math.sin(d * 2.7f) * (double)amp);
            shakeY = (int)(Math.cos(d * 3.3f) * (double)amp * 0.5);
        } else if (t >= 40.0f && t < 66.0f) {
            float rp = (t - 40.0f) / 26.0f;
            amp = 0.5f + rp * 2.4f;
            shakeX = (int)(Math.sin(t * 1.9f) * (double)amp);
        }
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
        // El brillo de boca y las chispas se dibujan DESPUES del cofre y con z hacia
        // adelante (300 > 250 del cofre 3D) para que salgan DE LA BOCA / al frente,
        // no detras del cofre como antes.
        PoseStack fxPose = g.pose();
        fxPose.pushPose();
        fxPose.translate(0.0f, 0.0f, 300.0f);
        if (t < 254.0f) {
            this.renderMouthGlow(g, cx, crateCY, t);
            this.renderSparks(g, cx, crateCY, t);
        }
        fxPose.popPose();
        dbgFxRaw += (double)(System.nanoTime() - dbgS) / 1000000.0;
        dbgS = System.nanoTime();
        if (this.candidates != null && !this.candidates.isEmpty()) {
            if (t >= 80.0f && t < 254.0f) {
                this.renderRoulettePanel(g, cx, rouletteY, w, t);
                this.renderRoulette(g, cx, rouletteY, w, t);
            } else if (t >= 254.0f) {
                this.renderRevealBurst(g, cx, crateCY, t - 254.0f);
                this.renderShockwaveRing(g, cx, crateCY, t - 254.0f);
                this.renderReveal(g, cx, cy, t);
            }
        }
        dbgReelRaw += (double)(System.nanoTime() - dbgS) / 1000000.0;
        float barsP = Math.min(1.0f, t / 8.0f);
        if (t > (float)(TOTAL - 10)) {
            barsP = Math.max(0.0f, ((float)TOTAL - t) / 10.0f);
        }
        int barH = (int)((float)h * 0.12f * barsP);
        g.fill(0, 0, w, barH, -16777216);
        g.fill(0, h - barH, w, h, -16777216);
        if (this.canSkip()) {
            g.drawCenteredString(this.font, "\u00a78[ESC] saltar (operador)", cx, h - barH - 12, -1716868438);
        }
    }

    // 0 (COMMON) .. 1 (MYTHIC): usado para escalar sutilmente intensidad/cantidad de
    // efectos por rareza sin tocar las constantes de timing compartidas con el server.
    private float rarityIntensity() {
        int n = Rarity.values().length - 1;
        return n <= 0 ? 0.0f : (float)this.winnerRarity.ordinal() / (float)n;
    }

    private void renderMouthGlow(GuiGraphics g, int cx, int crateCY, float t) {
        if (t < 46.0f || this.cUnitPx <= 0.0f) {
            return;
        }
        float open = Math.min(1.0f, (t - 46.0f) / 16.0f);
        float fade = t >= 254.0f ? Math.max(0.0f, 1.0f - (t - 254.0f) / 10.0f) : 1.0f;
        if (fade <= 0.02f) {
            return;
        }
        int color = 0xFFFFFF & this.rarityColor;
        float rarityI = this.rarityIntensity();
        float crateScreenH = this.cScaledH * this.cPx;
        // La abertura esta en la parte alta del cofre en pantalla.
        float mouthY = (float)crateCY - crateScreenH * 0.20f;
        // Doble pulso (lento + rapido superpuesto) en vez de una sola sinusoide: se
        // siente como una fuente de luz "viva" en lugar de un parpadeo mecanico. Mas
        // rareza = pulso ligeramente mas intenso.
        float pulse = 0.90f + 0.10f * (float)Math.sin((double)t * 0.35) + (0.03f + rarityI * 0.05f) * (float)Math.sin((double)t * 1.15 + 1.7);
        float a = open * fade * pulse;
        // 1) REEMPLAZO DEL HAZ VERTICAL (el usuario odiaba esa "raya" de luz recta que
        // subia de la tapa). Ahora la abertura emana luz de forma magica en dos partes,
        // ambas construidas SOLO con blits de glow suaves:
        //
        //   (a) ONDAS DE HALO: 3 anillos suaves que crecen desde la boca y se desvanecen
        //       en bucle (easeOut), como pulsos de luz de tesoro que "respiran" hacia
        //       afuera. Se aplastan en Y (elipses) para asentarse sobre la abertura.
        //   (b) ABANICO DE RAYOS: un puñado de haces de luz que se abren en abanico hacia
        //       arriba y a los lados (NO una sola columna). Cada rayo es una cadena corta
        //       de puntos suaves que se afinan/atenuan con la distancia, con un vaiven
        //       lento. Mas rareza = rayos mas largos, mas brillantes y con leve apertura.
        int waves = 3;
        for (int wI = 0; wI < waves; ++wI) {
            float wp = CrateCinematicScreen.frac((t - 46.0f) * 0.018f + (float)wI / (float)waves);
            float ease = 1.0f - (1.0f - wp) * (1.0f - wp);
            float ringW = this.cUnitPx * (0.55f + ease * (1.6f + rarityI * 1.1f));
            float ringH = ringW * 0.60f;
            float wa = a * (1.0f - wp) * (1.0f - wp) * (0.24f + rarityI * 0.16f);
            CrateCinematicScreen.drawGlowTex(g, (float)cx, mouthY - ease * this.cUnitPx * 0.18f, ringW, ringH, color, wa);
        }
        int rays = 5;
        float rayLen = this.cUnitPx * (1.05f + rarityI * 0.7f) * (0.6f + open * 0.4f);
        float spread = 1.95f + rarityI * 0.35f;          // apertura del abanico (radianes)
        for (int rI = 0; rI < rays; ++rI) {
            float frac = rays > 1 ? (float)rI / (float)(rays - 1) - 0.5f : 0.0f;
            // angulo apuntando hacia arriba (-PI/2) abierto en abanico + vaiven suave.
            float ang = (float)(-Math.PI / 2.0) + frac * spread + (float)Math.sin((double)(t * 0.12f + (double)rI * 1.7)) * 0.06f;
            float dirX = (float)Math.cos((double)ang);
            float dirY = (float)Math.sin((double)ang);
            float len = rayLen * (0.85f + 0.15f * (float)Math.sin((double)(t * 0.18f + (double)rI)));
            int dots = 3;
            for (int j = 0; j < dots; ++j) {
                float df = (float)j / (float)(dots - 1);       // 0 en la boca .. 1 en la punta
                float dist = len * df;
                float px = (float)cx + dirX * dist;
                float py = mouthY + dirY * dist;
                float size = (1.7f - df * 1.1f) * (0.9f + rarityI * 0.35f);
                float ra = a * (0.20f + rarityI * 0.10f) * (1.0f - df * 0.85f);
                int col = j == 0 ? 0xFFFFF2 : color;         // base calida, cuerpo con color
                CrateCinematicScreen.drawSoftDot(g, px, py, size, col, ra);
            }
        }
        // 2) Resplandor calido en la abertura (la fuente de luz): color + nucleo blanco.
        // El nucleo blanco se mezcla ligeramente hacia un blanco-calido (no frio) para
        // que el brillo se lea como "luz de tesoro", no como un flash generico.
        CrateCinematicScreen.drawGlowTex(g, (float)cx, mouthY, this.cUnitPx * 0.66f, this.cUnitPx * 0.44f, color, a * (0.38f + rarityI * 0.10f));
        CrateCinematicScreen.drawGlowTex(g, (float)cx, mouthY, this.cUnitPx * 0.32f, this.cUnitPx * 0.24f, 0xFFFFF2, a * 0.55f);
        // 3) Brasas de tesoro subiendo en espiral: lentas, elegantes, con envolvente de
        // vida suave (aparecen y se apagan con smoothstep -> nada de "pops" bruscos). El
        // radio de giro se ABRE con la altura y el ascenso desacelera arriba, dando una
        // ceniza magica que flota hacia afuera. Tamaño y brillo variados por semilla; una
        // de cada tres es blanco-calido para destacar. Cantidad acotada (<=13).
        int count = 7 + Math.round(rarityI * 6.0f);
        for (int i = 0; i < count; ++i) {
            float phase;
            float seed = (float)i * 9.17f + 3.0f;
            float rx = CrateCinematicScreen.frac((float)Math.sin(seed) * 43758.547f);
            float spd = 0.45f + CrateCinematicScreen.frac((float)Math.sin(seed + 1.3f) * 22578.11f) * 0.7f;
            float life = CrateCinematicScreen.frac((t - 46.0f) * 0.02f * spd + (phase = CrateCinematicScreen.frac((float)Math.sin(seed + 3.1f) * 13795.77f)));
            // Envolvente suave (fade-in y fade-out con smoothstep en cada extremo de la
            // vida) en vez de un seno pelado: entra y sale sin cortes.
            float env = CrateCinematicScreen.smoothstep(0.0f, 0.22f, life) * (1.0f - CrateCinematicScreen.smoothstep(0.72f, 1.0f, life));
            float pa = env * open * fade;
            if (pa <= 0.04f) {
                continue;
            }
            // Ascenso con desaceleracion arriba (easeOut) para que floten al final.
            float riseEase = 1.0f - (1.0f - life) * (1.0f - life);
            float rise = riseEase * crateScreenH * 0.98f;
            // Espiral que se ABRE con la altura (radio crece con life).
            float swirl = life * 5.5f + phase * 6.2832f;
            float swirlR = (1.5f + life * 6.0f) * (0.7f + rarityI * 0.5f);
            float x = (float)cx + (rx - 0.5f) * (this.cUnitPx * 0.30f) + (float)Math.sin((double)swirl) * swirlR;
            float y = mouthY - rise;
            float size = (1.0f + rarityI * 0.5f + CrateCinematicScreen.frac((float)Math.sin(seed + 7.7f) * 5678.1f) * 0.7f) * (0.75f + 0.5f * env);
            int col = i % 3 == 0 ? 0xFFFFF2 : color;
            CrateCinematicScreen.drawSoftDot(g, x, y, size, col, pa * 0.7f);
        }
    }

    // Smoothstep clasico (Hermite): 0 debajo de e0, 1 arriba de e1, transicion suave en
    // medio. Se usa para envolventes de vida de particulas sin cortes bruscos.
    private static float smoothstep(float e0, float e1, float x) {
        if (e0 == e1) {
            return x < e0 ? 0.0f : 1.0f;
        }
        float u = (x - e0) / (e1 - e0);
        if (u < 0.0f) {
            u = 0.0f;
        }
        if (u > 1.0f) {
            u = 1.0f;
        }
        return u * u * (3.0f - 2.0f * u);
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
        int half = (int)((float)(w / 2) * in);   // se abre de lado a lado de la pantalla
        int top = cy - 30;
        int bot = cy + 30;
        int rc = 0xFF000000 | 0xFFFFFF & this.rarityColor;
        g.fill(cx - half, top, cx + half, bot, 0xC6070912);
        g.fill(cx - half, top, cx + half, top + 2, rc);
        g.fill(cx - half, bot - 2, cx + half, bot, rc);
        // ventana/aguja central donde aterriza el premio
        int mw = 24;
        g.fill(cx - mw, top - 3, cx - mw + 2, bot + 3, rc);
        g.fill(cx + mw - 2, top - 3, cx + mw, bot + 3, rc);
        g.fill(cx - 1, top - 6, cx + 1, top, -1);
        g.fill(cx - 1, bot, cx + 1, bot + 6, -1);
    }

    private void ensureGeom() {
        if (this.geomReady) {
            return;
        }
        this.geomReady = true;
        CrateStyles.Style style = this.cfg == null ? null : CrateStyles.get(this.cfg.styleId);
        this.cIsCineStyle = style != null && style.isCinematic();
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
        if (t < 30.0f) {
            // Caida mas larga (30 ticks ~1.5s) y desde mas alto: se siente con peso y no
            // termina de golpe. Cae acelerando (1-p^2) como gravedad.
            float p = t / 30.0f;
            dropUnits = 3.4f * scaledH * (1.0f - p * p);
        } else {
            float b = t - 30.0f;
            dropUnits = (float)Math.abs(Math.sin((double)b * 0.5)) * 0.1f * scaledH * (float)Math.exp(-0.18 * (double)b);
        }
        if (t < 46.0f) {
            lid = 0.0f;
        } else if (t < 78.0f) {
            // apertura mas lenta (32 ticks) para que se sienta con mas tiempo/tension
            float p = (t - 46.0f) / 32.0f;
            lid = (1.0f - (1.0f - p) * (1.0f - p)) * 22.0f;
        } else {
            lid = 22.0f;
        }
        // Cara DECORADA (frente) de frente a la camara. El frente de estos modelos es la
        // cara NORTH = -Z (convencion confirmada en crate.json: "north": "crate_front"). A
        // yaw=0 la camara ve el +Z (la parte de atras); girando 180 sobre Y, el -Z (frente
        // decorado) queda hacia la camara. Replica la orientacion del render in-world
        // (CrateRenderer), que se ve correcto (frente al jugador + tapa abre hacia el).
        // EXCEPCION: las cajas del pack "W6 - Cinematic Crates" (estilos cine_*) tienen su
        // UV/textura de frente rotada 180 grados respecto al resto de modelos del mod (se
        // confirmo visualmente: con el yaw base de 180 mostraban la cara de atras). Se les
        // suma +180 extra SOLO a ellas para corregirlo, sin afectar ninguna otra crate.
        float yaw = 180.0f + (this.cIsCineStyle ? 180.0f : 0.0f);
        float pitch = 26.0f;
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
            // La tapa debe abrir HACIA la camara (estilo cofre del tesoro) en ambos casos.
            // - Cajas NO-cine (yaw efectivo 180): la bisagra natural (+Z, z~20.1px) queda
            //   del lado LEJANO; XP(+lid) levanta el borde cercano (-Z) hacia el jugador.
            //   Verificado: borde cercano sube en pantalla 16.32 -> 25.80.
            // - Cajas CINE (yaw efectivo 360/0 por el +180 extra): con la bisagra natural
            //   XP(+lid) levantaria el borde LEJANO (abre al reves = queja del usuario). A
            //   yaw=0 la cara decorada/frente es +Z y queda CERCA de la camara; para
            //   levantar ESE borde hay que pivotar en el borde lejano, reflejando la Z de la
            //   bisagra por el centro del cofre (pivotZ = 1 - hinge[2]) y usar XP(-lid).
            //   Verificado con la matriz Rx(26): esquina cercana N=(8,19.7241,18.3103),
            //   pivote en z=-4.1px, XP(-22) => screen-up 9.70 -> 18.85 (SUBE) manteniendose
            //   cerca de la camara (Zf 25.1 -> 24.0) = abre hacia el jugador. (Mismo enfoque
            //   que 2.9.4, que el usuario confirmo que abria bien hacia el.)
            float pivotZ = this.cIsCineStyle ? 1.0f - hinge[2] : hinge[2];
            float lidRot = this.cIsCineStyle ? -lid : lid;
            pose.pushPose();
            pose.translate(hinge[0], hinge[1], pivotZ);
            pose.mulPose(Axis.XP.rotationDegrees(lidRot));
            pose.translate(-hinge[0], -hinge[1], -pivotZ);
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

    private void renderRoulette(GuiGraphics g, int cx, int cy, int w, float t) {
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
        float slotW = 48.0f;
        float centerScale = 1.7f;
        int top = cy - 30;
        int bot = cy + 30;
        // Cuantos slots hacen falta para llenar TODO el ancho de la pantalla.
        int kMax = (int)Math.ceil((double)((float)(w / 2 + 48) / slotW)) + 1;
        PoseStack pose = g.pose();
        for (int k = -kMax; k <= kMax; ++k) {
            int stripPos = Math.floorMod(baseIdx + k, stripLen);
            int idx = this.reelStrip[stripPos];
            float off = ((float)k - frac) * slotW;
            float ax = (float)cx + off;
            if (ax < -slotW || ax > (float)w + slotW) {
                continue;
            }
            float dist = Math.abs(off) / slotW;
            float scale = centerScale * Math.max(0.55f, 1.0f - dist * 0.045f);
            ItemStack st = this.candidates.get(idx);
            if (st == null || st.isEmpty()) {
                continue;
            }
            // Posicion sub-pixel (float) para movimiento fluido.
            pose.pushPose();
            pose.translate(ax, (float)cy, 0.0f);
            pose.scale(scale, scale, 1.0f);
            g.renderItem(st, -8, -8);
            pose.popPose();
        }
        // Desvanecido en los bordes (estilo case-opening): los items se funden hacia los
        // lados en vez de aparecer/cortarse de golpe. Redondo/suave, dentro del panel.
        int fadeW = Math.max(40, w / 7);
        int steps = 12;
        for (int i = 0; i < steps; ++i) {
            float f = 1.0f - (float)i / (float)steps;
            int a = (int)(198.0f * f * f) << 24;
            int x0 = i * fadeW / steps;
            int x1 = (i + 1) * fadeW / steps + 1;
            g.fill(x0, top, x1, bot, a);
            g.fill(w - x1, top, w - x0, bot, a);
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
        float halo = 1.0f + (1.0f - (1.0f - pop) * (1.0f - pop)) * 0.3f;
        CrateCinematicScreen.drawRadialGlow(g, (float)cx, (float)cy, 95.0f * halo, 95.0f * halo, rc, ringA);
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate((float)cx, (float)cy, 0.0f);
        pose.scale(scale, scale, 1.0f);
        g.renderItem(win, -8, -8);
        pose.popPose();
        String name = win.getHoverName().getString();
        g.drawCenteredString(this.font, "\u00a77Has recibido", cx, cy + 32, -1);
        g.drawCenteredString(this.font, "\u00a7l" + name, cx, cy + 44, this.winnerRarity.rgb() | 0xFF000000);
        g.drawCenteredString(this.font, this.winnerRarity.color() + this.winnerRarity.displayName(), cx, cy + 58, -1);
    }

    private static float frac(float x) {
        return x - (float)Math.floor(x);
    }

    private static int color(int rgb) {
        return 0xFFFFFF & rgb;
    }

    // Mezcla lineal de dos colores RGB (0xRRGGBB) por un factor f (0=base, 1=rgb).
    // Usado para teñir el fondo casi-negro con el color de la rareza.
    private static int mix(int base, int rgb, float f) {
        if (f < 0.0f) {
            f = 0.0f;
        }
        if (f > 1.0f) {
            f = 1.0f;
        }
        int br = base >> 16 & 0xFF;
        int bg = base >> 8 & 0xFF;
        int bb = base & 0xFF;
        int rr = rgb >> 16 & 0xFF;
        int rg = rgb >> 8 & 0xFF;
        int rb = rgb & 0xFF;
        int or = (int)((float)br + ((float)rr - (float)br) * f);
        int og = (int)((float)bg + ((float)rg - (float)bg) * f);
        int ob = (int)((float)bb + ((float)rb - (float)bb) * f);
        return (or & 0xFF) << 16 | (og & 0xFF) << 8 | ob & 0xFF;
    }

    // ---- Dibujo REDONDO (nada de gradientes/particulas cuadradas) ----

    // Elipse rellena por scanlines. Forma redonda real.
    private static void fillEllipse(GuiGraphics g, float ecx, float ecy, float rx, float ry, int argb, int step) {
        if (rx < 0.6f || ry < 0.6f) {
            return;
        }
        if (step < 1) {
            step = 1;
        }
        int y0 = (int)Math.floor(ecy - ry);
        int y1 = (int)Math.ceil(ecy + ry);
        for (int y = y0; y < y1; y += step) {
            float vy = ((float)y + 0.5f * (float)step - ecy) / ry;
            if (vy < -1.0f || vy > 1.0f) {
                continue;
            }
            float half = rx * (float)Math.sqrt(Math.max(0.0, 1.0 - (double)(vy * vy)));
            int x0 = Math.round(ecx - half);
            int x1 = Math.round(ecx + half);
            if (x1 > x0) {
                g.fill(x0, y, x1, y + step, argb);
            }
        }
    }

    // Resplandor radial SUAVE. Nº de capas proporcional al radio (step 1) para que NO
    // se vean anillos/bandas concentricas (ese era el look "mal hecho").
    // Glow radial con UNA textura suave (1 draw call). Reemplaza el metodo viejo por
    // scanlines (miles de g.fill/frame = lag y bandas feas). Ahora es rapido y liso.
    private static void drawGlowTex(GuiGraphics g, float cx, float cy, float w, float h, int rgb, float alpha) {
        if (alpha <= 0.004f || w < 1.0f || h < 1.0f) {
            return;
        }
        if (alpha > 1.0f) {
            alpha = 1.0f;
        }
        float r = (float)(rgb >> 16 & 0xFF) / 255.0f;
        float gg = (float)(rgb >> 8 & 0xFF) / 255.0f;
        float b = (float)(rgb & 0xFF) / 255.0f;
        int iw = Math.round(w);
        int ih = Math.round(h);
        int x = Math.round(cx - w * 0.5f);
        int y = Math.round(cy - h * 0.5f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(r, gg, b, alpha);
        g.blit(GLOW_TEX, x, y, iw, ih, 0.0f, 0.0f, 128, 128, 128, 128);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void drawRadialGlow(GuiGraphics g, float ecx, float ecy, float rx, float ry, int rgb, float peakAlpha) {
        CrateCinematicScreen.drawGlowTex(g, ecx, ecy, rx * 2.0f, ry * 2.0f, rgb, peakAlpha);
    }

    private static void drawSoftDot(GuiGraphics g, float px, float py, float radius, int rgb, float alpha) {
        CrateCinematicScreen.drawGlowTex(g, px, py, radius * 3.4f, radius * 3.4f, rgb, alpha);
    }

    private void renderSceneBackground(GuiGraphics g, int w, int h, int cx, int crateCY, float t) {
        int color = 0xFFFFFF & this.rarityColor;
        float rarityI = this.rarityIntensity();
        // 1) base: degradado vertical profundo pero TEÑIDO con el color de la rareza (ya
        // NO negro puro - queja del usuario "es puro negro"). Se mezcla ~14-22% del color
        // de rareza dentro del casi-negro (mas rareza = mas tinte), un poco mas arriba que
        // abajo para que el color "flote" detras del cofre y los bordes queden oscuros.
        float baseTint = 0.14f + rarityI * 0.08f;      // 0.14 .. 0.22
        int topBase = CrateCinematicScreen.mix(0x0A0D14, color, baseTint);
        int botBase = CrateCinematicScreen.mix(0x03040A, color, baseTint * 0.55f);
        g.fillGradient(0, 0, w, h, 0xFF000000 | topBase, 0xFF000000 | botBase);
        // 2) resplandor ambiental de rareza detras del cofre: AHORA claramente visible.
        // Antes el alpha era ~0.15-0.20 (no se veia color); ahora es un lavado radial rico
        // en dos capas: un halo grande y suave + un nucleo mas intenso. Mas rareza = mas
        // grande y mas brillante. Crece al abrir el cofre y hace fade en el reveal.
        float amb = Math.min(1.0f, Math.max(0.0f, (t - 26.0f) / 44.0f));
        amb *= 0.86f + 0.14f * (float)Math.sin((double)t * 0.11);
        if (t >= 254.0f) {
            amb = Math.max(amb, Math.max(0.0f, 1.0f - (t - 254.0f) / 34.0f));
        }
        float ar = this.cUnitPx > 0.0f ? this.cUnitPx * 2.0f : (float)w * 0.26f;
        float ambScale = 1.0f + rarityI * 0.35f;
        // capa A: halo amplio y difuso que baña toda la zona central de color.
        CrateCinematicScreen.drawGlowTex(g, (float)cx, (float)crateCY - 6.0f, ar * 3.4f * ambScale, ar * 2.7f * ambScale, color, amb * (0.22f + rarityI * 0.13f));
        // capa B: nucleo de color mas concentrado e intenso justo detras del cofre, para
        // que el tono se LEA fuerte (pico ~0.38..0.60 segun rareza).
        CrateCinematicScreen.drawGlowTex(g, (float)cx, (float)crateCY - 4.0f, ar * 1.9f * ambScale, ar * 1.55f * ambScale, color, amb * (0.38f + rarityI * 0.22f));
        // 3) motas de polvo lentas y tenues (profundidad). Cantidad y brillo escalan
        // levemente con la rareza (mas "atmosfera magica" para premios grandes) pero
        // siempre dentro de un puñado de draw calls (<=20).
        int motes = 14 + Math.round(rarityI * 6.0f);
        for (int i = 0; i < motes; ++i) {
            float seed = (float)i * 3.71f + 1.3f;
            float mx = CrateCinematicScreen.frac((float)Math.sin(seed) * 43758.547f);
            float my0 = CrateCinematicScreen.frac((float)Math.sin(seed + 2.1f) * 22578.11f);
            float spd = 0.15f + CrateCinematicScreen.frac((float)Math.sin(seed + 4.7f) * 9124.3f) * 0.45f;
            float my = CrateCinematicScreen.frac(my0 - t * 0.0014f * spd);
            float twk = 0.35f + 0.65f * (float)Math.abs(Math.sin((double)t * 0.05 + (double)seed));
            float x = (mx + (float)Math.sin((double)t * 0.02 + (double)seed) * 0.02f) * (float)w;
            float y = my * (float)h;
            // Motas ocasionales con tinte de rareza (1 de cada 4) para dar profundidad de
            // color sin saturar; el resto es blanco tenue (polvo neutro).
            int mcol = i % 4 == 0 ? color : 0xFFFFFF;
            CrateCinematicScreen.drawSoftDot(g, x, y, 0.9f + twk * 0.7f, mcol, (i % 4 == 0 ? 0.09f : 0.06f) * twk);
        }
        // 4) vineta cinematografica: bordes oscuros para enmarcar (arriba/abajo + laterales)
        g.fillGradient(0, 0, w, (int)((float)h * 0.30f), 0x99000000, 0);
        g.fillGradient(0, (int)((float)h * 0.70f), w, h, 0, 0x99000000);
        int vw = Math.max(30, w / 8);
        int steps = 8;
        for (int i = 0; i < steps; ++i) {
            float f = 1.0f - (float)i / (float)steps;
            int al = (int)(102.0f * f * f) << 24;
            int x0 = i * vw / steps;
            int x1 = (i + 1) * vw / steps + 1;
            g.fill(x0, 0, x1, h, al);
            g.fill(w - x1, 0, w - x0, h, al);
        }
    }

    private void renderSparks(GuiGraphics g, int cx, int cy, float t) {
        int color = 0xFFFFFF & this.rarityColor;
        // Limite se mantiene bajo (baseline previo: 6 + rareza*2, max ~14 en MYTHIC) para
        // no reintroducir el lag de las cientos-de-fills anteriores; solo se mejora la
        // TRAYECTORIA (arco con deriva lateral en vez de subida recta) y el tamaño/color.
        int count = 6 + this.cfg.rarity.ordinal() * 2;
        for (int i = 0; i < count; ++i) {
            float phase;
            float seed = (float)i * 12.9898f + 4.233f;
            float rx = CrateCinematicScreen.frac((float)Math.sin(seed) * 43758.547f);
            float spd = 0.5f + CrateCinematicScreen.frac((float)Math.sin(seed + 1.7f) * 22578.11f) * 0.9f;
            float life = CrateCinematicScreen.frac(t * 0.009f * spd + (phase = CrateCinematicScreen.frac((float)Math.sin(seed + 5.3f) * 13795.77f)));
            // Envolvente de vida SUAVE (fade-in largo + fade-out) con smoothstep: las
            // chispas aparecen y desaparecen sin destellos secos. Antes era un seno pelado
            // que hacia que salieran/cortaran de golpe (parte de "las particulas horribles").
            float a = CrateCinematicScreen.smoothstep(0.0f, 0.18f, life) * (1.0f - CrateCinematicScreen.smoothstep(0.7f, 1.0f, life));
            if (a <= 0.02f) {
                continue;
            }
            // Trayectoria en espiral ascendente que se ABRE con la altura (el radio de giro
            // crece con la vida) + una leve gravedad al final -> arco gracioso en vez de
            // subida recta uniforme. Cada chispa gira a distinta fase/sentido.
            float dir = (i % 2 == 0) ? 1.0f : -1.0f;
            float swirl = life * (4.0f + spd * 2.0f) + phase * 6.2832f;
            float swirlR = (6.0f + life * 26.0f) * (0.6f + rx * 0.8f);
            float drift = (float)Math.cos((double)swirl) * swirlR * dir;
            float arcFall = life > 0.62f ? (life - 0.62f) * (life - 0.62f) * 70.0f : 0.0f;
            float x = (float)cx + (rx - 0.5f) * 150.0f + drift;
            float y = (float)cy + 120.0f - life * 300.0f + arcFall;
            // Tamaño con variacion por semilla + leve pulso: no todas iguales.
            float twk = 0.85f + 0.15f * (float)Math.sin((double)(t * 0.4f + (double)seed));
            float rad = (0.8f + (1.0f - life) * 1.0f) * (0.8f + CrateCinematicScreen.frac((float)Math.sin(seed + 9.1f) * 3456.7f) * 0.5f) * twk;
            // 1 de cada 4 blanco-calido brillante (highlight), resto color de rareza.
            int col = i % 4 == 0 ? 0xFFFFF2 : color;
            CrateCinematicScreen.drawSoftDot(g, x, y, rad, col, a * 0.5f);
        }
    }

    private void renderShockwaveRing(GuiGraphics g, int cx, int cy, float since) {
        if (since > 16.0f) {
            return;
        }
        // Dos ondas de choque limpias en capas: una FRONTAL blanco-calida rapida que se
        // expande con easeOutCubic (sale disparada y frena) y se afina/atenua al crecer, y
        // una SECUNDARIA con el color de la rareza que la persigue un poco mas lenta. El
        // grosor del punto disminuye con el radio para que el anillo se "estire" y afine
        // como una onda real en vez de un circulo de puntos gordos parejos.
        int bc = 0xFFFFFF & this.winnerRarity.rgb();
        int dots = 40;
        // --- onda frontal (blanco-calido) ---
        float p1 = Math.min(1.0f, since / 15.0f);
        float ease1 = 1.0f - (1.0f - p1) * (1.0f - p1) * (1.0f - p1);   // easeOutCubic
        float radius = ease1 * 200.0f;
        float ba = Math.max(0.0f, 1.0f - p1) * Math.min(1.0f, since / 1.2f);  // fade-in muy corto + fade-out
        if (ba > 0.02f) {
            float sz = 2.0f - ease1 * 1.1f;                              // se afina al expandir
            for (int i = 0; i < dots; ++i) {
                float ang = (float)((double)i * (Math.PI * 2 / (double)dots));
                float x = (float)cx + (float)Math.cos(ang) * radius;
                float y = (float)cy + (float)Math.sin(ang) * radius * 0.7f;
                CrateCinematicScreen.drawSoftDot(g, x, y, sz, 0xFFFFF2, ba * 0.85f);
            }
        }
        // --- onda secundaria (color de rareza), persigue con retraso ---
        float d2 = since - 2.5f;
        if (d2 > 0.0f) {
            float p2 = Math.min(1.0f, d2 / 13.0f);
            float ease2 = 1.0f - (1.0f - p2) * (1.0f - p2) * (1.0f - p2);
            float radius2 = ease2 * 168.0f;
            float ba2 = Math.max(0.0f, 1.0f - p2);
            float sz2 = 2.1f - ease2 * 1.0f;
            for (int i = 0; i < dots; ++i) {
                float ang = (float)((double)i * (Math.PI * 2 / (double)dots)) + 0.08f;
                float x = (float)cx + (float)Math.cos(ang) * radius2;
                float y = (float)cy + (float)Math.sin(ang) * radius2 * 0.7f;
                CrateCinematicScreen.drawSoftDot(g, x, y, sz2, bc, ba2 * 0.7f);
            }
        }
    }

    private void renderRevealBurst(GuiGraphics g, int cx, int cy, float since) {
        int bc = 0xFFFFFF & this.winnerRarity.rgb();
        float ba = Math.max(0.0f, 1.0f - since / 12.0f);
        if (ba <= 0.02f) {
            return;
        }
        // 0) DESTELLO central brillante que revienta y se apaga MUY rapido (~3 ticks): da
        // el "punch" del momento sin tapar el item. Nucleo blanco-calido sobre un halo de
        // color de rareza, ambos con blits suaves (nada de g.fill cuadrado).
        float flash = Math.max(0.0f, 1.0f - since / 3.0f);
        if (flash > 0.02f) {
            float fe = flash * flash;
            CrateCinematicScreen.drawGlowTex(g, (float)cx, (float)cy, 260.0f * (0.7f + since * 0.12f), 260.0f * (0.7f + since * 0.12f), bc, fe * 0.55f);
            CrateCinematicScreen.drawGlowTex(g, (float)cx, (float)cy, 130.0f, 130.0f, 0xFFFFF2, fe * 0.8f);
        }
        // Rayos principales del burst: easeOutCubic en el radio (salen rapido, frenan) para
        // que se sienta como una explosion de luz real y no un circulo que crece parejo. Se
        // alternan largos/cortos (rayos gruesos + destellos finos) para un borde estrellado.
        float p = Math.min(1.0f, since / 9.0f);
        float easedR = 1.0f - (1.0f - p) * (1.0f - p) * (1.0f - p);
        float br = easedR * 100.0f;
        int burst = 28;
        for (int i = 0; i < burst; ++i) {
            float ang = (float)((double)i * (Math.PI * 2 / (double)burst));
            float lenVar = 0.72f + CrateCinematicScreen.frac((float)Math.sin((double)i * 3.3) * 43758.5f) * 0.6f;
            // rayos pares un poco mas largos = borde con "picos" en vez de circulo parejo.
            if (i % 2 == 0) {
                lenVar += 0.18f;
            }
            float rr = br * lenVar;
            float x = (float)cx + (float)Math.cos(ang) * rr;
            float y = (float)cy + (float)Math.sin(ang) * rr;
            float sz = (1.5f + ba * 1.5f) * (i % 2 == 0 ? 1.15f : 0.8f);
            CrateCinematicScreen.drawSoftDot(g, x, y, sz, i % 4 == 0 ? 0xFFFFF2 : bc, ba * 0.9f);
        }
        // Capa de "polvo de estrellas" mas lenta y dispersa por detras, solo para
        // rarezas altas (EPIC+): un puñado de destellos pequeños que titilan en vez de
        // solo expandirse, dando profundidad al momento del reveal sin saturar de draws.
        if (this.winnerRarity.ordinal() >= Rarity.EPIC.ordinal()) {
            int sparkle = 10;
            for (int i = 0; i < sparkle; ++i) {
                float seed = (float)i * 7.13f + 2.0f;
                float ang = CrateCinematicScreen.frac((float)Math.sin(seed) * 43758.547f) * 6.2832f;
                float rr = (40.0f + CrateCinematicScreen.frac((float)Math.sin(seed + 1.9f) * 12345.6f) * 70.0f) * Math.min(1.0f, since / 6.0f);
                float twinkle = 0.5f + 0.5f * (float)Math.sin((double)(since * 9.0f + seed * 3.0f));
                float x = (float)cx + (float)Math.cos(ang) * rr;
                float y = (float)cy + (float)Math.sin(ang) * rr;
                CrateCinematicScreen.drawSoftDot(g, x, y, 1.1f + twinkle * 0.6f, 0xFFFFF2, ba * twinkle * 0.6f);
            }
        }
    }

    private static String stripAmp(String s) {
        return s == null ? "" : s.replace('&', '\u00a7');
    }
}

