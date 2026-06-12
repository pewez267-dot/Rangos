// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.block;

import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import com.fscrates.config.ParticleLayer;
import java.util.Iterator;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import com.fscrates.config.Rarity;
import java.util.ArrayList;
import com.fscrates.animation.AnimationRegistry;
import com.fscrates.registry.ModRegistry;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import com.fscrates.animation.CrateAnimation;
import com.fscrates.config.CrateConfig;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CrateBlockEntity extends BlockEntity
{
    private CrateConfig config;
    public static final float P_ANTICIPATION_END = 0.1f;
    public static final float P_OPEN_END = 0.22f;
    public static final float P_REVEAL_END = 0.88f;
    /**
     * Numero de vueltas completas que da la ruleta antes de parar. Mas vueltas en
     * la misma ventana de tiempo = ruleta MAS RAPIDA (estilo CS:GO) y con mas
     * suspenso al frenar. Se usa TANTO en el render como en el sonido para que
     * clicks y giro esten perfectamente sincronizados.
     */
    public static final int REEL_LOOPS = 10;
    public boolean animating;
    public int animTick;
    public int animTotal;
    private CrateAnimation animation;
    private int animColor;
    private ItemStack rewardIcon;
    private final List<ItemStack> candidates;
    private int winnerIndex;
    private Rarity effectRarity;
    private int soundStage;
    private int winTick;
    private int noteIndex;
    private int lastReelIndex;
    public float ambientTime;
    
    public CrateBlockEntity(final BlockPos pos, final BlockState state) {
        super((BlockEntityType)ModRegistry.CRATE_BE.get(), pos, state);
        this.config = new CrateConfig();
        this.animating = false;
        this.animTick = 0;
        this.animTotal = 150;
        this.animation = AnimationRegistry.get(AnimationRegistry.defaultId());
        this.animColor = 16777215;
        this.rewardIcon = ItemStack.EMPTY;
        this.candidates = new ArrayList<ItemStack>();
        this.winnerIndex = 0;
        this.effectRarity = Rarity.COMMON;
        this.soundStage = 0;
        this.winTick = -1;
        this.noteIndex = 0;
        this.lastReelIndex = -1;
        this.ambientTime = 0.0f;
    }
    
    public CrateConfig getConfig() {
        return this.config;
    }
    
    public void setConfig(final CrateConfig config) {
        this.config = ((config == null) ? new CrateConfig() : config);
        this.animColor = this.config.rarity.rgb();
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    public Rarity getRarity() {
        return this.config.rarity;
    }
    
    public CrateAnimation getAnimation() {
        return this.animation;
    }
    
    public int getAnimColor() {
        return this.animColor;
    }
    
    public ItemStack getRewardIcon() {
        return this.rewardIcon;
    }
    
    public List<ItemStack> getCandidates() {
        return this.candidates;
    }
    
    public int getWinnerIndex() {
        return this.winnerIndex;
    }

    /** Rareza EFECTIVA del premio ganador (define luz, sonido y particulas). */
    public Rarity getEffectRarity() {
        return this.effectRarity;
    }
    
    public void startAnimation(final String animationId, final int rarityColor, final int winnerIndex, final int winnerRarity, final List<ItemStack> cands) {
        this.animation = AnimationRegistry.get(animationId);
        this.animTotal = Math.max(6, this.animation.durationTicks());
        this.animColor = rarityColor;
        final Rarity[] rv = Rarity.values();
        this.effectRarity = rv[Math.max(0, Math.min(rv.length - 1, winnerRarity))];
        this.candidates.clear();
        if (cands != null) {
            for (final ItemStack s : cands) {
                if (s != null && !s.isEmpty()) {
                    this.candidates.add(s);
                }
            }
        }
        this.winnerIndex = (this.candidates.isEmpty() ? 0 : Math.max(0, Math.min(this.candidates.size() - 1, winnerIndex)));
        this.rewardIcon = (this.candidates.isEmpty() ? ItemStack.EMPTY : this.candidates.get(this.winnerIndex));
        this.animTick = 0;
        this.soundStage = 0;
        this.winTick = -1;
        this.noteIndex = 0;
        this.lastReelIndex = -1;
        this.animating = true;
        if (this.level != null) {
            // Sonido de "desbloqueo" al usar la llave: clink metalico de la cerradura
            // + click de la llave al girar. (Antes era una nota de bajo grave fea.)
            this.play(SoundEvents.CHAIN_PLACE, 0.6f, 1.45f);
            this.play(SoundEvents.UI_BUTTON_CLICK, 0.5f, 1.1f);
        }
    }
    
    public float progress() {
        return this.animating ? Math.min(1.0f, this.animTick / (float)Math.max(1, this.animTotal)) : 0.0f;
    }
    
    public ParticleLayer.Phase currentPhase() {
        if (!this.animating) {
            return ParticleLayer.Phase.IDLE;
        }
        final float p = this.progress();
        if (p < 0.1f) {
            return ParticleLayer.Phase.ANTICIPATION;
        }
        if (p < 0.22f) {
            return ParticleLayer.Phase.OPEN;
        }
        if (p < 0.88f) {
            return ParticleLayer.Phase.REVEAL;
        }
        return ParticleLayer.Phase.FINALE;
    }
    
    public float lidOpen(final float partial) {
        if (!this.animating) {
            return 0.0f;
        }
        final float p = (this.animTick + partial) / Math.max(1, this.animTotal);
        if (p < 0.1f) {
            return 0.0f;
        }
        if (p < 0.22f) {
            return easeOutBack((p - 0.1f) / 0.12f);
        }
        if (p < 0.94f) {
            return 1.0f;
        }
        return 1.0f - easeInOut(Math.min(1.0f, (p - 0.94f) / 0.06f));
    }
    
    public float shake(final float partial) {
        if (!this.animating) {
            return 0.0f;
        }
        final float p = (this.animTick + partial) / Math.max(1, this.animTotal);
        if (p >= 0.1f) {
            return 0.0f;
        }
        final float intensity = (0.1f - p) / 0.1f;
        return (float)Math.sin((this.animTick + partial) * 2.4f) * 0.06f * intensity;
    }
    
    public float revealProgress(final float partial) {
        final float p = (this.animTick + partial) / Math.max(1, this.animTotal);
        if (p <= 0.22f) {
            return 0.0f;
        }
        if (p >= 0.88f) {
            return 1.0f;
        }
        return (p - 0.22f) / 0.65999997f;
    }
    
    public float finaleProgress(final float partial) {
        final float p = (this.animTick + partial) / Math.max(1, this.animTotal);
        if (p <= 0.88f) {
            return 0.0f;
        }
        return Math.min(1.0f, (p - 0.88f) / 0.120000005f);
    }
    
    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final CrateBlockEntity be) {
        ++be.ambientTime;
        if (be.animating) {
            ++be.animTick;
            be.emitLayers(level, pos, be.currentPhase());
            be.emitAccent(level, pos);
            be.advanceSounds();
            if (be.animTick >= be.animTotal) {
                be.animating = false;
                be.animTick = 0;
                be.rewardIcon = ItemStack.EMPTY;
                be.candidates.clear();
                // Restaura el color de la crate para las particulas de reposo.
                be.animColor = be.config.rarity.rgb();
                be.effectRarity = be.config.rarity;
            }
        }
        else if (be.config.particles) {
            be.emitLayers(level, pos, ParticleLayer.Phase.IDLE);
        }
    }
    
    private void emitLayers(final Level level, final BlockPos pos, final ParticleLayer.Phase phase) {
        for (final ParticleLayer layer : this.config.particleLayers) {
            if (layer.phase != phase) {
                continue;
            }
            if (phase == ParticleLayer.Phase.IDLE && level.getGameTime() % Math.max(1, layer.interval) != 0L) {
                continue;
            }
            final ParticleOptions opt = this.resolve(layer);
            if (opt == null) {
                continue;
            }
            this.emitShape(level, pos, layer, opt);
        }
    }
    
    private ParticleOptions resolve(final ParticleLayer layer) {
        final String id = (layer.particleId == null) ? "" : layer.particleId.trim();
        if (id.equals("minecraft:dust") || id.equals("dust")) {
            final int color = layer.useRarityColor ? this.animColor : parseHex(layer.colorHex, this.animColor);
            return (ParticleOptions)this.dust(color, 1.4f);
        }
        final ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            return null;
        }
        final ParticleType<?> type = (ParticleType<?>)ForgeRegistries.PARTICLE_TYPES.getValue(rl);
        SimpleParticleType simpleParticleType;
        if (type instanceof final SimpleParticleType simpleParticleType2) {
            final SimpleParticleType simple = simpleParticleType = simpleParticleType2;
        }
        else {
            simpleParticleType = null;
        }
        return (ParticleOptions)simpleParticleType;
    }
    
    private DustParticleOptions dust(final int color, final float scale) {
        return new DustParticleOptions(new Vector3f((color >> 16 & 0xFF) / 255.0f, (color >> 8 & 0xFF) / 255.0f, (color & 0xFF) / 255.0f), scale);
    }
    
    private void emitShape(final Level level, final BlockPos pos, final ParticleLayer layer, final ParticleOptions opt) {
        final double cx = pos.getX() + 0.5;
        final double cy = pos.getY() + Math.max(0.0, layer.yOffset);
        final double cz = pos.getZ() + 0.5;
        final RandomSource rng = level.random;
        final int n = Math.max(1, layer.count);
        final double r = layer.radius;
        final double sp = layer.speed;
        final double spread = layer.spread;
        final double t = this.ambientTime * 0.1;
        for (int i = 0; i < n; ++i) {
            switch (layer.shape) {
                case HALO: {
                    final double angle = t + i * (6.283185307179586 / n);
                    level.addParticle(opt, cx + Math.cos(angle) * r, cy + 0.05 * Math.sin(t * 1.7 + i), cz + Math.sin(angle) * r, -Math.sin(angle) * sp, sp * 0.4, Math.cos(angle) * sp);
                    break;
                }
                case RING: {
                    final double angle = i * (6.283185307179586 / n);
                    level.addParticle(opt, cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r, Math.cos(angle) * sp, 0.0, Math.sin(angle) * sp);
                    break;
                }
                case BURST: {
                    final double ax = (rng.nextDouble() - 0.5) * 2.0;
                    final double az = (rng.nextDouble() - 0.5) * 2.0;
                    final double ay = 0.4 + rng.nextDouble() * 0.6;
                    final double mag = Math.max(0.001, Math.sqrt(ax * ax + ay * ay + az * az));
                    level.addParticle(opt, cx, cy, cz, ax / mag * (sp + spread), ay / mag * (sp + spread), az / mag * (sp + spread));
                    break;
                }
                case COLUMN: {
                    level.addParticle(opt, cx + (rng.nextDouble() - 0.5) * spread, cy + rng.nextDouble() * (0.4 + r), cz + (rng.nextDouble() - 0.5) * spread, 0.0, sp, 0.0);
                    break;
                }
                case SPIRAL: {
                    final double angle = t * 3.0 + i * 0.7;
                    final double rr = r * (0.3 + i / (double)n * 0.7);
                    level.addParticle(opt, cx + Math.cos(angle) * rr, cy + i / (double)n * 1.0, cz + Math.sin(angle) * rr, 0.0, sp, 0.0);
                    break;
                }
                case FOUNTAIN: {
                    final double angle = rng.nextDouble() * 3.141592653589793 * 2.0;
                    level.addParticle(opt, cx, cy, cz, Math.cos(angle) * spread, sp + rng.nextDouble() * 0.15, Math.sin(angle) * spread);
                    break;
                }
                case VORTEX: {
                    final double angle = t * 4.0 + i * (6.283185307179586 / n);
                    final double rr2 = r * (0.6 + 0.4 * Math.sin(t * 2.0 + i));
                    level.addParticle(opt, cx + Math.cos(angle) * rr2, cy + rng.nextDouble() * 0.5, cz + Math.sin(angle) * rr2, -Math.cos(angle) * sp * 2.0, sp, -Math.sin(angle) * sp * 2.0);
                    break;
                }
                case RAIN: {
                    level.addParticle(opt, cx + (rng.nextDouble() - 0.5) * (spread + r * 2.0), cy + rng.nextDouble() * 0.5, cz + (rng.nextDouble() - 0.5) * (spread + r * 2.0), 0.0, -sp, 0.0);
                    break;
                }
                case POINT: {
                    level.addParticle(opt, cx, cy, cz, (rng.nextDouble() - 0.5) * sp, rng.nextDouble() * sp, (rng.nextDouble() - 0.5) * sp);
                    break;
                }
            }
        }
    }
    
    private void emitAccent(final Level level, final BlockPos pos) {
        final float p = this.progress();
        final double cx = pos.getX() + 0.5;
        final double cz = pos.getZ() + 0.5;
        final double cyTop = pos.getY() + 1.5;
        final RandomSource rng = level.random;
        // Aura ambiental segun el TEMA de la animacion: BAJA y alrededor del cofre
        // (no sobre la ruleta, que esta a y~1.5) para que cada animacion se vea
        // distinta sin tapar el carrusel. Solo durante apertura/revelacion.
        if (p >= 0.1f && p < 0.88f && this.animTick % 3 == 0) {
            final ParticleOptions amb = this.themeParticle(this.animation.theme());
            final double ang = rng.nextDouble() * 6.283185307179586;
            final double rad = 0.5 + rng.nextDouble() * 0.2;
            level.addParticle(amb, cx + Math.cos(ang) * rad, pos.getY() + 0.2 + rng.nextDouble() * 0.5, cz + Math.sin(ang) * rad, 0.0, 0.02 + rng.nextDouble() * 0.03, 0.0);
        }
        // Particulas de acento SOLO en FINALE (p >= 0.88) para no tapar la ruleta.
        // AHORA dependen de la RAREZA del item ganado: tipo de particula + chispas
        // con el color de la rareza.
        if (p >= 0.88f && this.animTick % 2 == 0) {
            final ParticleOptions fin = this.finaleParticle(this.effectRarity);
            for (int i = 0; i < 5; ++i) {
                final double a2 = rng.nextDouble() * 6.283185307179586;
                final double s = 0.2 + rng.nextDouble() * 0.45;
                level.addParticle(fin, cx, cyTop, cz, Math.cos(a2) * s, 0.15 + rng.nextDouble() * 0.3, Math.sin(a2) * s);
            }
            for (int i = 0; i < 3; ++i) {
                final double a3 = rng.nextDouble() * 6.283185307179586;
                final double s2 = 0.15 + rng.nextDouble() * 0.35;
                level.addParticle((ParticleOptions)this.dust(this.animColor, 1.5f), cx, cyTop, cz, Math.cos(a3) * s2, 0.1 + rng.nextDouble() * 0.2, Math.sin(a3) * s2);
            }
        }
    }

    /** Particula de FINALE segun la rareza del premio (escala de "epicidad"). */
    private ParticleOptions finaleParticle(final Rarity r) {
        return switch (r) {
            case COMMON -> (ParticleOptions)ParticleTypes.END_ROD;
            case RARE -> (ParticleOptions)ParticleTypes.GLOW;
            case EPIC -> (ParticleOptions)ParticleTypes.WITCH;
            case LEGENDARY -> (ParticleOptions)ParticleTypes.FIREWORK;
            case MYTHIC -> (ParticleOptions)ParticleTypes.FLAME;
            default -> (ParticleOptions)ParticleTypes.FIREWORK;
        };
    }

    /** Particula ambiental segun el TEMA de la animacion (da identidad a cada una). */
    private ParticleOptions themeParticle(final CrateAnimation.Theme t) {
        return switch (t) {
            case INFERNAL -> (ParticleOptions)ParticleTypes.FLAME;
            case CELESTIAL -> (ParticleOptions)ParticleTypes.END_ROD;
            case NEON -> (ParticleOptions)ParticleTypes.GLOW;
            case MAGIC -> (ParticleOptions)ParticleTypes.WITCH;
            case ANCIENT -> (ParticleOptions)ParticleTypes.ENCHANT;
            case NATURE -> (ParticleOptions)ParticleTypes.HAPPY_VILLAGER;
            case CASINO -> (ParticleOptions)ParticleTypes.FIREWORK;
            default -> (ParticleOptions)this.dust(this.animColor, 1.0f);
        };
    }
    
    private static int parseHex(final String hex, final int fallback) {
        if (hex == null) {
            return fallback;
        }
        try {
            return (int)Long.parseLong(hex.replace("#", "").trim(), 16);
        }
        catch (final NumberFormatException e) {
            return fallback;
        }
    }
    
    private void advanceSounds() {
        final float p = this.progress();
        // --- Fase anticipacion: pequeño "thump" y nota de arranque ---
        if (this.soundStage == 0 && p >= 0.1f) {
            this.play(SoundEvents.CHEST_OPEN, 0.55f, 1.05f);
            this.play((SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value(), 0.45f, 0.75f);
            if (this.config.rarity.ordinal() >= Rarity.EPIC.ordinal()) {
                this.play(SoundEvents.BEACON_ACTIVATE, 0.35f, 1.4f);
            }
            this.soundStage = 1;
        }
        // --- Fase REVEAL: tick LIMPIO sincronizado con la ruleta (estilo CS:GO) ---
        // Un "tick" cada vez que un item cruza el centro, con la MISMA formula que
        // el render (easeOutReel * maxTravel, REEL_LOOPS): rapidisimo al inicio y
        // cada vez mas espaciado al desacelerar. Sonido: click de UI limpio y
        // profesional (NADA de amatista de picar ni bloques musicales).
        if (this.soundStage >= 1 && p >= 0.22f && p < 0.88f && !this.candidates.isEmpty()) {
            final float rp = this.revealProgress(0.0f);
            final int n = this.candidates.size();
            final int winner = Math.max(0, Math.min(n - 1, this.winnerIndex));
            final float maxTravel = n * REEL_LOOPS + winner; // identico a renderReel
            final int idx = (int)Math.floor(easeOutReel(Math.min(1.0f, rp)) * maxTravel);
            if (idx != this.lastReelIndex) {
                this.lastReelIndex = idx;
                final float pitch = 0.9f + rp * 0.7f; // sube de tono al acercarse al premio
                this.play(SoundEvents.UI_BUTTON_CLICK, 0.45f, pitch);
            }
        }
        // --- Fase FINALE: golpe de victoria SEGUN LA RAREZA del item ganado ---
        // En el instante en que la ruleta para (p>=0.88) suena el golpe, justo
        // cuando se entrega la recompensa (todo sincronizado). Una breve cola 4
        // ticks despues remata sin arrastrarse.
        if (p >= 0.88f) {
            if (this.soundStage < 60) {
                this.playWin(this.effectRarity);
                this.soundStage = 60;
                this.winTick = this.animTick;
                this.noteIndex = 0;
            } else if (this.noteIndex == 0 && this.animTick - this.winTick >= 4) {
                this.playWinTail(this.effectRarity);
                this.noteIndex = 1;
            }
        }
    }

    /**
     * Golpe de victoria segun la RAREZA del item ganado. Paleta rica (campanas,
     * fuegos, faro, trueno, rugido) — sin totems ni jingle de logro ni amatista.
     * Suena en el instante exacto en que para la ruleta y se entrega el premio.
     */
    private void playWin(final Rarity r) {
        switch (r) {
            case COMMON: {
                this.play(SoundEvents.NOTE_BLOCK_BELL, 0.7f, 1.0f);
                this.play(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
                break;
            }
            case RARE: {
                this.play(SoundEvents.NOTE_BLOCK_BELL, 0.8f, 1.0f);
                this.play(SoundEvents.NOTE_BLOCK_BELL, 0.55f, 1.5f);
                this.play(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.3f);
                break;
            }
            case EPIC: {
                this.play(SoundEvents.NOTE_BLOCK_BELL, 0.85f, 1.0f);
                this.play(SoundEvents.PLAYER_LEVELUP, 0.6f, 1.15f);
                this.play(SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, 1.3f);
                this.play(SoundEvents.BEACON_POWER_SELECT, 0.55f, 1.2f);
                break;
            }
            case LEGENDARY: {
                this.play(SoundEvents.NOTE_BLOCK_BELL, 0.9f, 0.7f);   // gong grave
                this.play(SoundEvents.NOTE_BLOCK_BELL, 0.7f, 1.5f);   // campana alta brillante
                this.play(SoundEvents.PLAYER_LEVELUP, 0.6f, 1.0f);
                this.play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.6f, 1.0f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.65f, 1.5f);  // fanfarria resonante
                break;
            }
            case MYTHIC: {
                this.play(SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.3f);     // rugido epico
                this.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.4f, 1.4f); // trueno
                this.play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.65f, 0.9f);
                this.play(SoundEvents.NOTE_BLOCK_BELL, 0.95f, 0.5f);       // gong profundo
                this.play(SoundEvents.NOTE_BLOCK_BELL, 0.7f, 1.5f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.7f, 1.2f);
                break;
            }
        }
    }

    /** Cola breve (4 ticks despues del golpe) para rematar sin arrastrarse. */
    private void playWinTail(final Rarity r) {
        this.play(SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.45f, 1.0f);
        if (r.ordinal() >= Rarity.LEGENDARY.ordinal()) {
            this.play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.5f, 1.3f);
            this.play(SoundEvents.NOTE_BLOCK_BELL, 0.55f, 2.0f);
        }
        if (r == Rarity.MYTHIC) {
            this.play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.5f, 0.8f);
        }
    }
    
    private void play(final SoundEvent sound, final float vol, final float pitch) {
        if (this.level == null || sound == null) {
            return;
        }
        this.level.playLocalSound(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, sound, SoundSource.BLOCKS, vol, pitch, false);
    }

    /**
     * Overload que acepta sonidos envueltos en Holder (p.ej. los NOTE_BLOCK_* y
     * UI_BUTTON_CLICK lo son en 1.20.1). Permite referirlos sin .value() y hace
     * que el codigo compile sea cual sea su tipo exacto en el mapping.
     */
    private void play(final net.minecraft.core.Holder<SoundEvent> sound, final float vol, final float pitch) {
        if (sound != null) {
            this.play((SoundEvent)sound.value(), vol, pitch);
        }
    }
    
    private static float easeOutBack(final float t) {
        final float c1 = 1.70158f;
        final float c2 = c1 + 1.0f;
        final float x = t - 1.0f;
        return 1.0f + c2 * x * x * x + c1 * x * x;
    }
    
    private static float easeInOut(final float t) {
        return (t < 0.5f) ? (2.0f * t * t) : (1.0f - (float)Math.pow(-2.0f * t + 2.0f, 2.0) / 2.0f);
    }

    /** Curva de desaceleracion COMPARTIDA por la ruleta (render del carrusel + sonido).
     *  easeOutCubic: mucha velocidad al inicio y desaceleracion GRADUAL y suave
     *  hasta parar en el premio (estilo CS:GO), evitando el "arrastre" final que
     *  daba la quartica. Combinado con REEL_LOOPS=10 da una ruleta rapida. */
    public static float easeOutReel(final float t) {
        final float x = 1.0f - t;
        return 1.0f - x * x * x;
    }
    
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("config", (Tag)this.config.save());
    }
    
    public void load(final CompoundTag tag) {
        super.load(tag);
        if (tag.contains("config")) {
            this.config = CrateConfig.load(tag.getCompound("config"));
            this.animColor = this.config.rarity.rgb();
        }
    }
    
    public CompoundTag getUpdateTag() {
        final CompoundTag tag = super.getUpdateTag();
        tag.put("config", (Tag)this.config.save());
        return tag;
    }
    
    public void handleUpdateTag(final CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains("config")) {
            this.config = CrateConfig.load(tag.getCompound("config"));
            this.animColor = this.config.rarity.rgb();
        }
    }
    
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return (Packet<ClientGamePacketListener>)ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }
    
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket pkt) {
        final CompoundTag tag = pkt.getTag();
        if (tag != null && tag.contains("config")) {
            this.config = CrateConfig.load(tag.getCompound("config"));
            this.animColor = this.config.rarity.rgb();
        }
    }
    
    public static List<ItemStack> decodeItems(final CompoundTag wrap) {
        final List<ItemStack> out = new ArrayList<ItemStack>();
        if (wrap == null) {
            return out;
        }
        final ListTag list = wrap.getList("items", 10);
        for (int i = 0; i < list.size(); ++i) {
            out.add(ItemStack.of(list.getCompound(i)));
        }
        return out;
    }
}
