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
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;
import com.fscrates.animation.AnimationRegistry;
import com.fscrates.registry.ModRegistry;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import com.fscrates.config.Rarity;
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
    public static final float P_REVEAL_END = 0.9f;
    public static final int REEL_STEPS = 180;
    private static final int OPEN_TICKS = 16;
    private static final int HOLD_TICKS = 70;
    private static final int CLOSE_TICKS = 26;
    private static final float SPIRAL_FRAC = 0.39f;
    private static final int SPIRAL_MIN_TICKS = 130;
    private static final int PEAK_HOLD_TICKS = 18;
    private static final int SPIRAL_BONUS_MYTHIC = 24;
    private static final int BUILDUP_TICKS = 196;
    public boolean animating;
    public int animTick;
    public int animTotal;
    private int tSpiralEnd;
    private int tOpenEnd;
    private int tSpinStop;
    private int tHoldEnd;
    private int tRiseEnd;
    private boolean peakPlayed;
    private boolean instant;
    private CrateAnimation animation;
    private int animColor;
    private ItemStack rewardIcon;
    private final List<ItemStack> candidates;
    private int winnerIndex;
    private Rarity effectRarity;
    private int[] candidateRarities;
    private int soundStage;
    private int winTick;
    private int noteIndex;
    private int lastReelIndex;
    private int lastRiseTick;
    public float ambientTime;
    
    public CrateBlockEntity(final BlockPos pos, final BlockState state) {
        super((BlockEntityType)ModRegistry.CRATE_BE.get(), pos, state);
        this.config = new CrateConfig();
        this.animating = false;
        this.animTick = 0;
        this.animTotal = 150;
        this.tSpiralEnd = 0;
        this.tOpenEnd = 0;
        this.tSpinStop = 0;
        this.tHoldEnd = 0;
        this.tRiseEnd = 0;
        this.peakPlayed = false;
        this.instant = false;
        this.animation = AnimationRegistry.get(AnimationRegistry.defaultId());
        this.animColor = 16777215;
        this.rewardIcon = ItemStack.EMPTY;
        this.candidates = new ArrayList<ItemStack>();
        this.winnerIndex = 0;
        this.effectRarity = Rarity.COMMON;
        this.candidateRarities = new int[0];
        this.soundStage = 0;
        this.winTick = -1;
        this.noteIndex = 0;
        this.lastReelIndex = -1;
        this.lastRiseTick = -100;
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
    
    public Rarity getEffectRarity() {
        return this.effectRarity;
    }
    
    public int[] getCandidateRarities() {
        return this.candidateRarities;
    }
    
    public static int[] decodeRarities(final CompoundTag wrap) {
        if (wrap == null || !wrap.contains("rar")) {
            return new int[0];
        }
        return wrap.getIntArray("rar");
    }
    
    public void startAnimation(final String animationId, final int rarityColor, final int winnerIndex, final int winnerRarity, final int[] candRarities, final List<ItemStack> cands) {
        this.animation = AnimationRegistry.get(animationId);
        final int base = Math.max(6, this.animation.durationTicks());
        this.instant = (this.animation.style() == CrateAnimation.Style.INSTANT);
        if (this.instant) {
            this.tSpiralEnd = 0;
            this.tOpenEnd = 0;
            this.tSpinStop = 0;
            this.tHoldEnd = 0;
            this.tRiseEnd = 0;
            this.animTotal = base;
        }
        else {
            this.tSpinStop = Math.round(base * 0.9f);
            final Rarity cr = this.config.rarity;
            this.tSpiralEnd = 196;
            this.tOpenEnd = this.tSpiralEnd + 16;
            if (this.tOpenEnd >= this.tSpinStop - 4) {
                this.tOpenEnd = Math.max(this.tSpiralEnd + 2, this.tSpinStop - 6);
            }
            this.tRiseEnd = Math.max(4, this.tSpiralEnd - peakHoldTicks(cr));
            this.tHoldEnd = this.tSpinStop + 70;
            this.animTotal = this.tHoldEnd + 26;
        }
        this.animColor = rarityColor;
        final Rarity[] rv = Rarity.values();
        this.effectRarity = rv[Math.max(0, Math.min(rv.length - 1, winnerRarity))];
        this.candidateRarities = ((candRarities == null) ? new int[0] : candRarities);
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
        this.lastRiseTick = -100;
        this.peakPlayed = false;
        this.animating = true;
        if (this.level != null) {
            this.playUnlock(this.config.rarity);
        }
    }
    
    public float progress() {
        return this.animating ? Math.min(1.0f, this.animTick / (float)Math.max(1, this.animTotal)) : 0.0f;
    }
    
    public int getSpiralEndTick() {
        return this.tSpiralEnd;
    }
    
    public int getOpenEndTick() {
        return this.tOpenEnd;
    }
    
    public int getSpinStopTick() {
        return this.tSpinStop;
    }
    
    public int getHoldEndTick() {
        return this.tHoldEnd;
    }
    
    public boolean isInstant() {
        return this.instant;
    }
    
    public ParticleLayer.Phase currentPhase() {
        if (!this.animating) {
            return ParticleLayer.Phase.IDLE;
        }
        if (this.instant) {
            return ParticleLayer.Phase.REVEAL;
        }
        final int t = this.animTick;
        if (t < this.tSpiralEnd) {
            return ParticleLayer.Phase.ANTICIPATION;
        }
        if (t < this.tOpenEnd) {
            return ParticleLayer.Phase.OPEN;
        }
        if (t < this.tSpinStop) {
            return ParticleLayer.Phase.REVEAL;
        }
        return ParticleLayer.Phase.FINALE;
    }
    
    public float lidOpen(final float partial) {
        if (!this.animating) {
            return 0.0f;
        }
        if (this.instant) {
            return 1.0f;
        }
        final float t = this.animTick + partial;
        if (t <= this.tSpiralEnd) {
            return 0.0f;
        }
        if (t < this.tOpenEnd) {
            return easeOutBack((t - this.tSpiralEnd) / Math.max(1.0f, (float)(this.tOpenEnd - this.tSpiralEnd)));
        }
        if (t < this.tHoldEnd) {
            return 1.0f;
        }
        if (t < this.animTotal) {
            return 1.0f - easeInOut(Math.min(1.0f, (t - this.tHoldEnd) / Math.max(1.0f, (float)(this.animTotal - this.tHoldEnd))));
        }
        return 0.0f;
    }
    
    public float shake(final float partial) {
        if (!this.animating || this.instant) {
            return 0.0f;
        }
        final float t = this.animTick + partial;
        if (t >= this.tSpiralEnd) {
            return 0.0f;
        }
        final float intensity = (this.tSpiralEnd - t) / Math.max(1.0f, (float)this.tSpiralEnd);
        return (float)Math.sin(t * 2.4f) * 0.06f * intensity;
    }
    
    public float revealProgress(final float partial) {
        if (this.instant) {
            return 1.0f;
        }
        final float t = this.animTick + partial;
        if (t <= this.tOpenEnd) {
            return 0.0f;
        }
        if (t >= this.tSpinStop) {
            return 1.0f;
        }
        return (t - this.tOpenEnd) / Math.max(1.0f, (float)(this.tSpinStop - this.tOpenEnd));
    }
    
    public float finaleProgress(final float partial) {
        if (this.instant) {
            return 1.0f;
        }
        final float t = this.animTick + partial;
        if (t <= this.tSpinStop) {
            return 0.0f;
        }
        return Math.min(1.0f, (t - this.tSpinStop) / 14.0f);
    }
    
    public float closeProgress(final float partial) {
        if (!this.animating || this.instant) {
            return 0.0f;
        }
        final float t = this.animTick + partial;
        if (t <= this.tHoldEnd) {
            return 0.0f;
        }
        if (t >= this.animTotal) {
            return 1.0f;
        }
        return (t - this.tHoldEnd) / Math.max(1.0f, (float)(this.animTotal - this.tHoldEnd));
    }
    
    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final CrateBlockEntity be) {
        ++be.ambientTime;
        if (be.animating) {
            ++be.animTick;
            be.emitLayers(level, pos, be.currentPhase());
            be.emitAccent(level, pos);
            be.emitBuildupSpiral(level, pos);
            be.emitSpiralBurst(level, pos);
            be.advanceSounds();
            if (be.animTick >= be.animTotal) {
                be.animating = false;
                be.animTick = 0;
                be.rewardIcon = ItemStack.EMPTY;
                be.candidates.clear();
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
        SimpleParticleType simpleParticleType3;
        if (type instanceof final SimpleParticleType simpleParticleType4) {
            final SimpleParticleType simpleParticleType2 = simpleParticleType3 = simpleParticleType4;
        }
        else {
            simpleParticleType3 = null;
        }
        return (ParticleOptions)simpleParticleType3;
    }
    
    private DustParticleOptions dust(final int color, final float scale) {
        return new DustParticleOptions(new Vector3f((color >> 16 & 0xFF) / 255.0f, (color >> 8 & 0xFF) / 255.0f, (color & 0xFF) / 255.0f), scale);
    }
    
    private double particleDensity() {
        return (this.config.rarity == Rarity.COMMON) ? 0.55 : 1.0;
    }
    
    private void emitShape(final Level level, final BlockPos pos, final ParticleLayer layer, final ParticleOptions opt) {
        final double crateScale = this.config.rarity.sizeScale();
        final double scale = 1.0 + (crateScale - 1.0) * 0.22;
        final double cx = pos.getX() + 0.5;
        final double cy = pos.getY() + Math.max(0.0, layer.yOffset) * scale;
        final double cz = pos.getZ() + 0.5;
        final RandomSource rng = level.random;
        final int n = Math.max(1, (int)Math.round(Math.max(1, layer.count) * this.particleDensity()));
        final double r = layer.radius * scale;
        final double sp = layer.speed;
        final double spread = layer.spread * scale;
        final double t = this.ambientTime * 0.1;
        for (int i = 0; i < n; ++i) {
            switch (layer.shape) {
                case HALO: {
                    final double angle = t + i * (6.283185307179586 / n);
                    level.addParticle(opt, cx + Math.cos(angle) * r, cy + 0.05 * scale * Math.sin(t * 1.7 + i), cz + Math.sin(angle) * r, -Math.sin(angle) * sp, sp * 0.4, Math.cos(angle) * sp);
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
                    level.addParticle(opt, cx + (rng.nextDouble() - 0.5) * spread, cy + rng.nextDouble() * (0.4 * scale + r), cz + (rng.nextDouble() - 0.5) * spread, 0.0, sp, 0.0);
                    break;
                }
                case SPIRAL: {
                    final double tt = this.ambientTime * 0.18;
                    final double frac = (i + (int)(this.ambientTime % 3.0f)) % n / (double)n;
                    final double ang = tt + frac * 12.566370614359172;
                    final double rr = r * (1.05 - frac * 0.4);
                    level.addParticle(opt, cx + Math.cos(ang) * rr, cy + frac * 1.5 * scale, cz + Math.sin(ang) * rr, -Math.sin(ang) * sp, sp + 0.02, Math.cos(ang) * sp);
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
                    level.addParticle(opt, cx + Math.cos(angle) * rr2, cy + rng.nextDouble() * 0.5 * scale, cz + Math.sin(angle) * rr2, -Math.cos(angle) * sp * 2.0, sp, -Math.sin(angle) * sp * 2.0);
                    break;
                }
                case RAIN: {
                    level.addParticle(opt, cx + (rng.nextDouble() - 0.5) * (spread + r * 2.0), cy + rng.nextDouble() * 0.5 * scale, cz + (rng.nextDouble() - 0.5) * (spread + r * 2.0), 0.0, -sp, 0.0);
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
        if (this.instant) {
            return;
        }
        final int t = this.animTick;
        final double cx = pos.getX() + 0.5;
        final double cz = pos.getZ() + 0.5;
        final double cyTop = pos.getY() + 1.5;
        final RandomSource rng = level.random;
        if (t >= this.tOpenEnd && t < this.tSpinStop && t % 3 == 0) {
            final ParticleOptions amb = this.themeParticle(this.animation.theme());
            final double ang = rng.nextDouble() * 6.283185307179586;
            final double rad = 0.5 + rng.nextDouble() * 0.2;
            level.addParticle(amb, cx + Math.cos(ang) * rad, pos.getY() + 0.2 + rng.nextDouble() * 0.5, cz + Math.sin(ang) * rad, 0.0, 0.02 + rng.nextDouble() * 0.03, 0.0);
        }
        if (t >= this.tSpinStop && t < this.tHoldEnd && t % 2 == 0) {
            final ParticleOptions fin = this.finaleParticle(this.effectRarity);
            for (int burst = (t < this.tSpinStop + 12) ? 6 : 2, i = 0; i < burst; ++i) {
                final double a2 = rng.nextDouble() * 6.283185307179586;
                final double s = 0.2 + rng.nextDouble() * 0.5;
                level.addParticle(fin, cx, cyTop, cz, Math.cos(a2) * s, 0.15 + rng.nextDouble() * 0.3, Math.sin(a2) * s);
            }
            for (int i = 0; i < 3; ++i) {
                final double a3 = rng.nextDouble() * 6.283185307179586;
                final double s2 = 0.15 + rng.nextDouble() * 0.35;
                level.addParticle((ParticleOptions)this.dust(this.animColor, 1.5f), cx, cyTop, cz, Math.cos(a3) * s2, 0.1 + rng.nextDouble() * 0.2, Math.sin(a3) * s2);
            }
        }
        if (t >= this.tHoldEnd && t < this.animTotal && t % 2 == 0) {
            final double a4 = rng.nextDouble() * 6.283185307179586;
            final double rad2 = 0.2 + rng.nextDouble() * 0.25;
            level.addParticle((ParticleOptions)this.dust(this.config.rarity.rgb(), 1.2f), cx + Math.cos(a4) * rad2, cyTop - 0.2, cz + Math.sin(a4) * rad2, 0.0, -0.06, 0.0);
        }
    }
    
    private ParticleOptions finaleParticle(final Rarity r) {
        return switch (r) {
            case COMMON -> ParticleTypes.END_ROD;
            case RARE -> ParticleTypes.GLOW;
            case EPIC -> ParticleTypes.WITCH;
            case LEGENDARY -> ParticleTypes.FIREWORK;
            case MYTHIC -> ParticleTypes.FLAME;
            default -> ParticleTypes.FIREWORK;
        };
    }
    
    private void emitBuildupSpiral(final Level level, final BlockPos pos) {
        if (this.instant || this.tSpiralEnd <= 1) {
            return;
        }
        final int t = this.animTick;
        if (t <= 0 || t >= this.tSpiralEnd) {
            return;
        }
        final Rarity r = this.config.rarity;
        final boolean common = r == Rarity.COMMON;
        if (common && ((int)this.ambientTime & 0x1) == 0x1) {
            return;
        }
        final double pscale = 1.0 + (r.sizeScale() - 1.0) * 0.22;
        final float p = Math.min(1.0f, t / (float)Math.max(1, this.tSpiralEnd));
        final double cx = pos.getX() + 0.5;
        final double cz = pos.getZ() + 0.5;
        final double baseY = pos.getY() + 0.1;
        final DustParticleOptions dust = this.dust(r.rgb(), 1.2f);
        final ParticleOptions spark = this.openingSparkle(r);
        final int arms = common ? (1 + Math.round(p * 1.0f)) : (2 + Math.round(p * 4.0f));
        final double turns = 2.0 + p * 1.5;
        final double height = (1.25 + p * 0.45) * pscale;
        final double baseR = 0.55 * pscale;
        final double spin = this.ambientTime * 0.3;
        final int steps = common ? 2 : 3;
        for (int a = 0; a < arms; ++a) {
            final double armOff = a * (6.283185307179586 / arms);
            for (int s = 0; s < steps; ++s) {
                final double frac = (s + this.ambientTime % 4.0f * 0.25) / steps;
                final double ang = spin + armOff + frac * turns * 6.283185307179586;
                final double rr = baseR * (1.05 - frac * 0.45);
                final double px = cx + Math.cos(ang) * rr;
                final double pz = cz + Math.sin(ang) * rr;
                final double py = baseY + frac * height;
                final double vTan = 0.04 + p * 0.05;
                level.addParticle((ParticleOptions)dust, px, py, pz, -Math.sin(ang) * vTan, 0.02 + p * 0.03, Math.cos(ang) * vTan);
                final boolean addSpark = common ? (s == steps - 1) : (s == steps - 1 || p > 0.6f);
                if (addSpark) {
                    level.addParticle(spark, px, py, pz, -Math.sin(ang) * vTan * 0.6, 0.03, Math.cos(ang) * vTan * 0.6);
                }
            }
        }
    }
    
    private void emitSpiralBurst(final Level level, final BlockPos pos) {
        if (this.instant || this.tSpiralEnd <= 1 || this.animTick != this.tSpiralEnd - 1) {
            return;
        }
        final Rarity r = this.config.rarity;
        final int tier = r.ordinal();
        final double scale = 1.0 + (r.sizeScale() - 1.0) * 0.22;
        final RandomSource rng = level.random;
        final double cx = pos.getX() + 0.5;
        final double cz = pos.getZ() + 0.5;
        final double y = pos.getY() + 0.6 * scale;
        final double rad = 0.5 * scale;
        final DustParticleOptions dust = this.dust(r.rgb(), 1.4f);
        final ParticleOptions spark = this.openingSparkle(r);
        final float power = 1.0f + tier * 0.35f;
        level.addParticle((ParticleOptions)ParticleTypes.FLASH, cx, y, cz, 0.0, 0.0, 0.0);
        for (int puffs = 2 + tier, i = 0; i < puffs; ++i) {
            level.addParticle((ParticleOptions)ParticleTypes.EXPLOSION, cx + (rng.nextDouble() - 0.5) * rad, y + (rng.nextDouble() - 0.5) * 0.3 * scale, cz + (rng.nextDouble() - 0.5) * rad, 0.0, 0.0, 0.0);
        }
        for (int ringN = 20 + tier * 6, j = 0; j < ringN; ++j) {
            final double ang = j * (6.283185307179586 / ringN);
            final double px = cx + Math.cos(ang) * rad;
            final double pz = cz + Math.sin(ang) * rad;
            final double v = 0.18 * power;
            level.addParticle((ParticleOptions)dust, px, y, pz, Math.cos(ang) * v, 0.06, Math.sin(ang) * v);
            level.addParticle(spark, px, y, pz, Math.cos(ang) * v * 0.7, 0.08, Math.sin(ang) * v * 0.7);
        }
        for (int sphereN = 24 + tier * 10, k = 0; k < sphereN; ++k) {
            final double ax = rng.nextDouble() - 0.5;
            final double ay = rng.nextDouble() * 0.9 + 0.1;
            final double az = rng.nextDouble() - 0.5;
            final double mag = Math.max(0.001, Math.sqrt(ax * ax + ay * ay + az * az));
            final double sp = (0.25 + rng.nextDouble() * 0.35) * power;
            final ParticleOptions p = (ParticleOptions)((k % 3 == 0) ? ParticleTypes.FIREWORK : ((k % 3 == 1) ? spark : dust));
            level.addParticle(p, cx, y, cz, ax / mag * sp, ay / mag * sp, az / mag * sp);
        }
        for (int upN = 8 + tier * 4, l = 0; l < upN; ++l) {
            final double a = rng.nextDouble() * 6.283185307179586;
            final double rr = rng.nextDouble() * rad * 0.6;
            level.addParticle(spark, cx + Math.cos(a) * rr, y, cz + Math.sin(a) * rr, (rng.nextDouble() - 0.5) * 0.06, (0.25 + rng.nextDouble() * 0.4) * power, (rng.nextDouble() - 0.5) * 0.06);
        }
    }
    
    private ParticleOptions openingSparkle(final Rarity r) {
        return switch (r) {
            case COMMON -> ParticleTypes.END_ROD;
            case RARE -> ParticleTypes.GLOW;
            case EPIC -> ParticleTypes.WITCH;
            case LEGENDARY -> ParticleTypes.ENCHANT;
            case MYTHIC -> ParticleTypes.FLAME;
            default -> ParticleTypes.END_ROD;
        };
    }
    
    private ParticleOptions themeParticle(final CrateAnimation.Theme t) {
        return switch (t) {
            case INFERNAL -> ParticleTypes.FLAME;
            case CELESTIAL -> ParticleTypes.END_ROD;
            case NEON -> ParticleTypes.GLOW;
            case MAGIC -> ParticleTypes.WITCH;
            case ANCIENT -> ParticleTypes.ENCHANT;
            case NATURE -> ParticleTypes.HAPPY_VILLAGER;
            case CASINO -> ParticleTypes.FIREWORK;
            default -> this.dust(this.animColor, 1.0f);
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
        if (this.instant) {
            if (this.soundStage < 60) {
                this.playWin(this.effectRarity);
                this.soundStage = 60;
            }
            return;
        }
        final int t = this.animTick;
        final Rarity cr = this.config.rarity;
        if (this.soundStage == 0 && t >= 2) {
            this.playSpiralCharge(cr);
            this.soundStage = 1;
        }
        if (this.soundStage == 1 && t > 2 && t < this.tRiseEnd) {
            final float p = Math.min(1.0f, (t - 2) / (float)Math.max(1, this.tRiseEnd - 2));
            final int interval = Math.max(2, Math.round(10.0f - p * 8.0f));
            if (t - this.lastRiseTick >= interval) {
                this.lastRiseTick = t;
                this.playSpiralRise(cr, p);
            }
        }
        if (this.soundStage == 1 && !this.peakPlayed && t >= this.tRiseEnd) {
            this.peakPlayed = true;
            this.playSpiralPeak(cr);
        }
        if (this.soundStage == 1 && t >= this.tSpiralEnd) {
            this.playOpenAccent(cr);
            this.soundStage = 2;
        }
        if (this.soundStage >= 2 && t >= this.tOpenEnd && t < this.tSpinStop && !this.candidates.isEmpty()) {
            final float rp = this.revealProgress(0.0f);
            final int n = this.candidates.size();
            final int winner = Math.max(0, Math.min(n - 1, this.winnerIndex));
            final float maxTravel = reelTravel(n, winner);
            final int idx = (int)Math.floor(easeOutReel(Math.min(1.0f, rp)) * maxTravel);
            if (idx != this.lastReelIndex) {
                this.lastReelIndex = idx;
                final float pitch = 0.9f + rp * 0.7f;
                this.play((Holder<SoundEvent>)SoundEvents.UI_BUTTON_CLICK, 0.4f, pitch);
            }
        }
        if (t >= this.tSpinStop && this.soundStage >= 2 && this.soundStage < 60) {
            this.playWin(this.effectRarity);
            this.soundStage = 60;
            this.winTick = t;
            this.noteIndex = 0;
        }
        else if (this.soundStage == 60 && this.noteIndex == 0 && t - this.winTick >= 4) {
            this.playWinTail(this.effectRarity);
            this.noteIndex = 1;
        }
        if (this.soundStage >= 60 && this.soundStage < 70 && t >= this.tHoldEnd) {
            this.playClose(cr);
            this.soundStage = 70;
        }
    }
    
    private void playUnlock(final Rarity r) {
        switch (r) {
            case COMMON: {
                this.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.5f, 1.4f);
                this.play(SoundEvents.BEACON_POWER_SELECT, 0.45f, 1.25f);
                break;
            }
            case RARE: {
                this.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.55f, 1.2f);
                this.play(SoundEvents.CONDUIT_AMBIENT, 0.5f, 1.15f);
                this.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.4f, 1.1f);
                break;
            }
            case EPIC: {
                this.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 1.1f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.5f, 1.0f);
                this.play(SoundEvents.EVOKER_PREPARE_ATTACK, 0.5f, 1.0f);
                break;
            }
            case LEGENDARY: {
                this.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 0.9f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.55f, 0.9f);
                this.play((Holder<SoundEvent>)SoundEvents.RAID_HORN, 0.45f, 1.1f);
                this.play(SoundEvents.TRIDENT_THUNDER, 0.35f, 1.4f);
                break;
            }
            case MYTHIC: {
                this.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 0.7f);
                this.play(SoundEvents.CONDUIT_ACTIVATE, 0.55f, 0.8f);
                this.play(SoundEvents.WARDEN_HEARTBEAT, 0.55f, 0.85f);
                this.play(SoundEvents.ENDER_DRAGON_GROWL, 0.4f, 0.9f);
                break;
            }
        }
    }
    
    private void playSpiralCharge(final Rarity r) {
        switch (r) {
            case COMMON: {
                this.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.45f, 1.5f);
                break;
            }
            case RARE: {
                this.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.5f, 1.35f);
                this.play(SoundEvents.BEACON_POWER_SELECT, 0.4f, 1.3f);
                break;
            }
            case EPIC: {
                this.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.55f, 1.2f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.5f, 1.1f);
                this.play(SoundEvents.EVOKER_CAST_SPELL, 0.4f, 1.1f);
                break;
            }
            case LEGENDARY: {
                this.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 1.0f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.55f, 1.0f);
                this.play(SoundEvents.EVOKER_CAST_SPELL, 0.45f, 0.9f);
                break;
            }
            case MYTHIC: {
                this.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 0.85f);
                this.play(SoundEvents.CONDUIT_ACTIVATE, 0.6f, 1.0f);
                this.play(SoundEvents.ENDER_DRAGON_GROWL, 0.4f, 1.2f);
                this.play(SoundEvents.WARDEN_HEARTBEAT, 0.45f, 0.9f);
                break;
            }
        }
    }
    
    private void playSpiralRise(final Rarity r, final float p) {
        final float vol = Math.min(1.0f, 0.45f + p * 0.55f);
        switch (r) {
            case COMMON: {
                final float pitch = Math.min(1.55f, 0.55f + p * 1.0f);
                this.play(SoundEvents.BEACON_POWER_SELECT, vol * 0.85f, pitch);
                break;
            }
            case RARE: {
                final float pitch = Math.min(1.5f, 0.55f + p * 0.95f);
                this.play(SoundEvents.ENCHANTMENT_TABLE_USE, vol, pitch);
                if (p > 0.55f) {
                    this.play(SoundEvents.CONDUIT_AMBIENT, vol * 0.3f, 0.85f + p * 0.4f);
                    break;
                }
                break;
            }
            case EPIC: {
                final float pitch = Math.min(1.5f, 0.5f + p * 1.0f);
                this.play(SoundEvents.ENCHANTMENT_TABLE_USE, vol, pitch);
                if (p > 0.3f) {
                    this.play(SoundEvents.EVOKER_CAST_SPELL, vol * (0.1f + p * 0.35f), 0.6f + p * 0.6f);
                }
                if (p > 0.8f) {
                    this.play(SoundEvents.CONDUIT_ACTIVATE, vol * 0.4f, 0.85f + p * 0.3f);
                    break;
                }
                break;
            }
            case LEGENDARY: {
                final float pitch = Math.min(1.25f, 0.4f + p * 0.85f);
                this.play(SoundEvents.BEACON_POWER_SELECT, vol, pitch);
                if (p > 0.35f) {
                    this.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.15f + p * 0.25f, 0.7f + p * 0.45f);
                }
                if (p > 0.75f) {
                    this.play((Holder<SoundEvent>)SoundEvents.RAID_HORN, vol * 0.28f, 0.72f + p * 0.32f);
                    break;
                }
                break;
            }
            case MYTHIC: {
                final float pitch = Math.min(1.05f, 0.3f + p * 0.75f);
                this.play(SoundEvents.BEACON_POWER_SELECT, vol, pitch);
                if (p > 0.2f) {
                    this.play(SoundEvents.WARDEN_HEARTBEAT, vol * (0.2f + p * 0.4f), 0.55f + p * 0.5f);
                }
                if (p > 0.5f) {
                    this.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.2f + p * 0.25f, 0.65f + p * 0.45f);
                }
                if (p > 0.88f) {
                    this.play(SoundEvents.ENDER_DRAGON_GROWL, 0.18f, 1.2f);
                    break;
                }
                break;
            }
        }
    }
    
    private static int peakHoldTicks(final Rarity r) {
        return 44;
    }
    
    private static int spiralBonusTicks(final Rarity r) {
        return 24;
    }
    
    private void playSpiralPeak(final Rarity r) {
        switch (r) {
            case COMMON: {
                this.play(SoundEvents.BELL_BLOCK, 0.85f, 1.5f);
                this.play(SoundEvents.BELL_BLOCK, 0.75f, 1.78f);
                this.play(SoundEvents.BELL_BLOCK, 0.62f, 2.0f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.6f, 1.6f);
                this.play(SoundEvents.BEACON_POWER_SELECT, 0.55f, 1.85f);
                this.play(SoundEvents.CONDUIT_AMBIENT, 0.4f, 1.7f);
                break;
            }
            case RARE: {
                this.play(SoundEvents.BELL_BLOCK, 0.88f, 1.33f);
                this.play(SoundEvents.BELL_BLOCK, 0.76f, 1.68f);
                this.play(SoundEvents.BELL_BLOCK, 0.62f, 2.0f);
                this.play(SoundEvents.CONDUIT_ACTIVATE, 0.65f, 1.45f);
                this.play(SoundEvents.CONDUIT_AMBIENT, 0.5f, 1.3f);
                this.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.55f, 1.5f);
                break;
            }
            case EPIC: {
                this.play(SoundEvents.BELL_BLOCK, 0.9f, 1.18f);
                this.play(SoundEvents.BELL_BLOCK, 0.78f, 1.5f);
                this.play(SoundEvents.BELL_BLOCK, 0.64f, 1.78f);
                this.play(SoundEvents.EVOKER_CAST_SPELL, 0.62f, 1.2f);
                this.play(SoundEvents.EVOKER_PREPARE_ATTACK, 0.5f, 1.0f);
                this.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.4f, 1.45f);
                break;
            }
            case LEGENDARY: {
                this.play(SoundEvents.BELL_BLOCK, 0.95f, 1.5f);
                this.play(SoundEvents.BELL_BLOCK, 0.88f, 1.15f);
                this.play(SoundEvents.BELL_BLOCK, 0.8f, 0.85f);
                this.play((Holder<SoundEvent>)SoundEvents.RAID_HORN, 0.8f, 1.1f);
                this.play((Holder<SoundEvent>)SoundEvents.RAID_HORN, 0.62f, 0.82f);
                this.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.68f, 1.2f);
                this.play(SoundEvents.CONDUIT_ACTIVATE, 0.6f, 1.25f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.6f, 1.4f);
                break;
            }
            case MYTHIC: {
                this.play(SoundEvents.BELL_BLOCK, 0.95f, 1.4f);
                this.play(SoundEvents.BELL_BLOCK, 0.9f, 1.0f);
                this.play(SoundEvents.BELL_BLOCK, 0.75f, 0.75f);
                this.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.7f, 1.1f);
                this.play(SoundEvents.WARDEN_HEARTBEAT, 0.6f, 1.2f);
                this.play(SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.2f);
                break;
            }
        }
    }
    
    private void playOpenAccent(final Rarity r) {
        switch (r) {
            case COMMON: {
                this.play(SoundEvents.GENERIC_EXPLODE, 0.8f, 1.5f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.8f, 1.2f);
                this.play(SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.6f);
                this.play(SoundEvents.BELL_BLOCK, 0.5f, 1.7f);
                break;
            }
            case RARE: {
                this.play(SoundEvents.CONDUIT_ACTIVATE, 0.9f, 1.1f);
                this.play(SoundEvents.TRIDENT_THUNDER, 0.75f, 1.3f);
                this.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.45f, 1.3f);
                this.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f, 1.2f);
                this.play(SoundEvents.GENERIC_EXPLODE, 0.75f, 1.35f);
                break;
            }
            case EPIC: {
                this.play(SoundEvents.GENERIC_EXPLODE, 1.0f, 1.0f);
                this.play(SoundEvents.GENERIC_EXPLODE, 0.75f, 1.18f);
                this.play(SoundEvents.GENERIC_EXPLODE, 0.55f, 0.85f);
                this.play(SoundEvents.WARDEN_SONIC_BOOM, 0.8f, 1.2f);
                this.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.7f, 1.1f);
                this.play(SoundEvents.EVOKER_CAST_SPELL, 0.55f, 0.95f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.6f, 1.25f);
                break;
            }
            case LEGENDARY: {
                this.play(SoundEvents.GENERIC_EXPLODE, 1.0f, 0.85f);
                this.play(SoundEvents.GENERIC_EXPLODE, 0.95f, 1.0f);
                this.play(SoundEvents.GENERIC_EXPLODE, 0.8f, 1.18f);
                this.play(SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 0.95f);
                this.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.95f, 1.0f);
                this.play(SoundEvents.TRIDENT_THUNDER, 0.8f, 1.15f);
                this.play((Holder<SoundEvent>)SoundEvents.RAID_HORN, 0.9f, 1.0f);
                this.play((Holder<SoundEvent>)SoundEvents.RAID_HORN, 0.7f, 0.8f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.7f, 1.2f);
                break;
            }
            case MYTHIC: {
                this.play(SoundEvents.GENERIC_EXPLODE, 1.0f, 0.7f);
                this.play(SoundEvents.GENERIC_EXPLODE, 1.0f, 0.92f);
                this.play(SoundEvents.GENERIC_EXPLODE, 0.85f, 1.15f);
                this.play(SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 0.85f);
                this.play(SoundEvents.WARDEN_ROAR, 0.8f, 1.0f);
                this.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 0.95f);
                this.play(SoundEvents.ENDER_DRAGON_GROWL, 0.85f, 0.9f);
                this.play((Holder<SoundEvent>)SoundEvents.RAID_HORN, 0.8f, 0.82f);
                break;
            }
        }
    }
    
    private void playWin(final Rarity r) {
        switch (r) {
            case COMMON: {
                this.play(SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.5f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.5f, 1.3f);
                break;
            }
            case RARE: {
                this.play(SoundEvents.BEACON_ACTIVATE, 0.65f, 1.2f);
                this.play(SoundEvents.CONDUIT_ACTIVATE, 0.55f, 1.3f);
                this.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.5f, 1.2f);
                break;
            }
            case EPIC: {
                this.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.7f, 1.0f);
                this.play(SoundEvents.CONDUIT_ACTIVATE, 0.6f, 1.2f);
                this.play(SoundEvents.EVOKER_CAST_SPELL, 0.55f, 1.1f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.55f, 1.3f);
                break;
            }
            case LEGENDARY: {
                this.play((Holder<SoundEvent>)SoundEvents.RAID_HORN, 0.85f, 0.95f);
                this.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f);
                this.play(SoundEvents.TRIDENT_THUNDER, 0.7f, 1.2f);
                this.play(SoundEvents.WARDEN_SONIC_BOOM, 0.55f, 1.1f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.75f, 1.3f);
                this.play(SoundEvents.CONDUIT_ACTIVATE, 0.6f, 1.1f);
                break;
            }
            case MYTHIC: {
                this.play(SoundEvents.ENDER_DRAGON_GROWL, 0.95f, 1.0f);
                this.play(SoundEvents.WARDEN_SONIC_BOOM, 0.85f, 0.95f);
                this.play(SoundEvents.WARDEN_ROAR, 0.7f, 1.0f);
                this.play((Holder<SoundEvent>)SoundEvents.RAID_HORN, 0.8f, 0.85f);
                this.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 1.1f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.8f, 1.1f);
                break;
            }
        }
    }
    
    private void playWinTail(final Rarity r) {
        switch (r) {
            case COMMON: {
                this.play(SoundEvents.BEACON_POWER_SELECT, 0.45f, 1.8f);
                break;
            }
            case RARE: {
                this.play(SoundEvents.BEACON_POWER_SELECT, 0.5f, 1.7f);
                this.play(SoundEvents.CONDUIT_AMBIENT, 0.4f, 1.3f);
                break;
            }
            case EPIC: {
                this.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.5f, 1.3f);
                this.play(SoundEvents.CONDUIT_AMBIENT, 0.45f, 1.2f);
                break;
            }
            case LEGENDARY: {
                this.play(SoundEvents.TRIDENT_THUNDER, 0.6f, 1.2f);
                this.play((Holder<SoundEvent>)SoundEvents.RAID_HORN, 0.5f, 1.05f);
                this.play(SoundEvents.BEACON_ACTIVATE, 0.5f, 1.5f);
                break;
            }
            case MYTHIC: {
                this.play(SoundEvents.ENDER_DRAGON_GROWL, 0.65f, 1.3f);
                this.play(SoundEvents.WARDEN_ROAR, 0.5f, 1.1f);
                this.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.55f, 1.25f);
                break;
            }
        }
    }
    
    private void playClose(final Rarity r) {
        switch (r) {
            case COMMON: {
                this.play(SoundEvents.BEACON_DEACTIVATE, 0.5f, 1.1f);
                break;
            }
            case RARE: {
                this.play(SoundEvents.BEACON_DEACTIVATE, 0.5f, 1.0f);
                this.play(SoundEvents.CONDUIT_DEACTIVATE, 0.4f, 1.2f);
                break;
            }
            case EPIC: {
                this.play(SoundEvents.BEACON_DEACTIVATE, 0.55f, 1.0f);
                this.play(SoundEvents.CONDUIT_DEACTIVATE, 0.45f, 0.9f);
                break;
            }
            case LEGENDARY: {
                this.play(SoundEvents.CONDUIT_DEACTIVATE, 0.55f, 0.95f);
                this.play(SoundEvents.BEACON_DEACTIVATE, 0.5f, 0.85f);
                break;
            }
            case MYTHIC: {
                this.play(SoundEvents.CONDUIT_DEACTIVATE, 0.55f, 0.9f);
                this.play(SoundEvents.WARDEN_HEARTBEAT, 0.45f, 0.8f);
                this.play(SoundEvents.ENDER_DRAGON_GROWL, 0.4f, 0.85f);
                break;
            }
        }
    }
    
    private void play(final SoundEvent sound, final float vol, final float pitch) {
        if (this.level == null || sound == null) {
            return;
        }
        this.level.playLocalSound(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, sound, SoundSource.BLOCKS, vol, pitch, false);
    }
    
    private void play(final Holder<SoundEvent> sound, final float vol, final float pitch) {
        if (sound != null) {
            this.play((SoundEvent)sound.value(), vol, pitch);
        }
    }
    
    private static float easeOutBack(final float t) {
        final float c1 = 1.70158f;
        final float c2 = 2.70158f;
        final float x = t - 1.0f;
        return 1.0f + 2.70158f * x * x * x + 1.70158f * x * x;
    }
    
    private static float easeInOut(final float t) {
        return (t < 0.5f) ? (2.0f * t * t) : (1.0f - (float)Math.pow(-2.0f * t + 2.0f, 2.0) / 2.0f);
    }
    
    public static float easeOutReel(final float t) {
        final float x = 1.0f - t;
        return 1.0f - x * x * x * x * x;
    }
    
    public static float reelTravel(final int n, final int winner) {
        if (n <= 0) {
            return 180.0f;
        }
        return (float)(180 + Math.floorMod(winner - 180, n));
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
