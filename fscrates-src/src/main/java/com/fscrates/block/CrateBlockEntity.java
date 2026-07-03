package com.fscrates.block;

import com.fscrates.animation.AnimationRegistry;
import com.fscrates.animation.CrateAnimation;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.ParticleLayer;
import com.fscrates.config.Rarity;
import com.fscrates.registry.ModRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

public class CrateBlockEntity
extends BlockEntity {
    private CrateConfig config = new CrateConfig();
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
    public boolean animating = false;
    public int animTick = 0;
    public int animTotal = 150;
    private int tSpiralEnd = 0;
    private int tOpenEnd = 0;
    private int tSpinStop = 0;
    private int tHoldEnd = 0;
    private int tRiseEnd = 0;
    private boolean peakPlayed = false;
    private boolean instant = false;
    private CrateAnimation animation = AnimationRegistry.get(AnimationRegistry.defaultId());
    private int animColor = 0xFFFFFF;
    private ItemStack rewardIcon = ItemStack.EMPTY;
    private final List<ItemStack> candidates = new ArrayList<ItemStack>();
    private int winnerIndex = 0;
    private Rarity effectRarity = Rarity.COMMON;
    private int[] candidateRarities = new int[0];
    private int soundStage = 0;
    private int winTick = -1;
    private int noteIndex = 0;
    private int lastReelIndex = -1;
    private int lastRiseTick = -100;
    public float ambientTime = 0.0f;
    private final Set<UUID> openedBy = new HashSet<UUID>();

    public boolean hasOpenedBy(UUID id) {
        return id != null && this.openedBy.contains(id);
    }

    public void markOpenedBy(UUID id) {
        if (id != null && this.openedBy.add(id)) {
            this.setChanged();
        }
    }

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModRegistry.CRATE_BE.get(), pos, state);
    }

    public CrateConfig getConfig() {
        return this.config;
    }

    public void setConfig(CrateConfig config) {
        this.config = config == null ? new CrateConfig() : config;
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

    public static int[] decodeRarities(CompoundTag wrap) {
        return wrap != null && wrap.contains("rar") ? wrap.getIntArray("rar") : new int[]{};
    }

    public void startAnimation(String animationId, int rarityColor, int winnerIndex, int winnerRarity, int[] candRarities, List<ItemStack> cands) {
        this.animation = AnimationRegistry.get(animationId);
        int base = Math.max(this.animation.style() == CrateAnimation.Style.INSTANT ? 26 : 6, this.animation.durationTicks());
        boolean bl = this.instant = this.animation.style() == CrateAnimation.Style.INSTANT;
        if (this.instant) {
            this.tSpiralEnd = 0;
            this.tOpenEnd = 0;
            this.tSpinStop = 0;
            this.tHoldEnd = 0;
            this.tRiseEnd = 0;
            this.animTotal = base;
        } else {
            this.tSpinStop = Math.round((float)base * 0.9f);
            Rarity cr = this.config.rarity;
            this.tSpiralEnd = 196;
            this.tOpenEnd = this.tSpiralEnd + 16;
            if (this.tOpenEnd >= this.tSpinStop - 4) {
                this.tOpenEnd = Math.max(this.tSpiralEnd + 2, this.tSpinStop - 6);
            }
            this.tRiseEnd = Math.max(4, this.tSpiralEnd - CrateBlockEntity.peakHoldTicks(cr));
            this.tHoldEnd = this.tSpinStop + 70;
            this.animTotal = this.tHoldEnd + 26;
        }
        this.animColor = rarityColor;
        Rarity[] rv = Rarity.values();
        this.effectRarity = rv[Math.max(0, Math.min(rv.length - 1, winnerRarity))];
        this.candidateRarities = candRarities == null ? new int[]{} : candRarities;
        this.candidates.clear();
        if (cands != null) {
            for (ItemStack s : cands) {
                if (s == null || s.isEmpty()) continue;
                this.candidates.add(s);
            }
        }
        this.winnerIndex = this.candidates.isEmpty() ? 0 : Math.max(0, Math.min(this.candidates.size() - 1, winnerIndex));
        this.rewardIcon = this.candidates.isEmpty() ? ItemStack.EMPTY : this.candidates.get(this.winnerIndex);
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
        return this.animating ? Math.min(1.0f, (float)this.animTick / (float)Math.max(1, this.animTotal)) : 0.0f;
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
        int t = this.animTick;
        if (t < this.tSpiralEnd) {
            return ParticleLayer.Phase.ANTICIPATION;
        }
        if (t < this.tOpenEnd) {
            return ParticleLayer.Phase.OPEN;
        }
        return t < this.tSpinStop ? ParticleLayer.Phase.REVEAL : ParticleLayer.Phase.FINALE;
    }

    public float lidOpen(float partial) {
        if (!this.animating) {
            return 0.0f;
        }
        if (this.instant) {
            return 1.0f;
        }
        float t = (float)this.animTick + partial;
        if (t <= (float)this.tSpiralEnd) {
            return 0.0f;
        }
        if (t < (float)this.tOpenEnd) {
            return CrateBlockEntity.easeOutBack((t - (float)this.tSpiralEnd) / Math.max(1.0f, (float)(this.tOpenEnd - this.tSpiralEnd)));
        }
        if (t < (float)this.tHoldEnd) {
            return 1.0f;
        }
        return t < (float)this.animTotal ? 1.0f - CrateBlockEntity.easeInOut(Math.min(1.0f, (t - (float)this.tHoldEnd) / Math.max(1.0f, (float)(this.animTotal - this.tHoldEnd)))) : 0.0f;
    }

    public float shake(float partial) {
        if (this.animating && !this.instant) {
            float t = (float)this.animTick + partial;
            if (t >= (float)this.tSpiralEnd) {
                return 0.0f;
            }
            float intensity = ((float)this.tSpiralEnd - t) / Math.max(1.0f, (float)this.tSpiralEnd);
            return (float)Math.sin(t * 2.4f) * 0.06f * intensity;
        }
        return 0.0f;
    }

    public float revealProgress(float partial) {
        if (this.instant) {
            return 1.0f;
        }
        float t = (float)this.animTick + partial;
        if (t <= (float)this.tOpenEnd) {
            return 0.0f;
        }
        return t >= (float)this.tSpinStop ? 1.0f : (t - (float)this.tOpenEnd) / Math.max(1.0f, (float)(this.tSpinStop - this.tOpenEnd));
    }

    public float finaleProgress(float partial) {
        if (this.instant) {
            return 1.0f;
        }
        float t = (float)this.animTick + partial;
        return t <= (float)this.tSpinStop ? 0.0f : Math.min(1.0f, (t - (float)this.tSpinStop) / 14.0f);
    }

    public float closeProgress(float partial) {
        if (this.animating && !this.instant) {
            float t = (float)this.animTick + partial;
            if (t <= (float)this.tHoldEnd) {
                return 0.0f;
            }
            return t >= (float)this.animTotal ? 1.0f : (t - (float)this.tHoldEnd) / Math.max(1.0f, (float)(this.animTotal - this.tHoldEnd));
        }
        return 0.0f;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CrateBlockEntity be) {
        be.ambientTime += 1.0f;
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
        } else if (be.config.particles) {
            be.emitLayers(level, pos, ParticleLayer.Phase.IDLE);
        }
    }

    private void emitLayers(Level level, BlockPos pos, ParticleLayer.Phase phase) {
        for (ParticleLayer layer : this.config.particleLayers) {
            ParticleOptions opt;
            if (layer.phase != phase || phase == ParticleLayer.Phase.IDLE && level.getGameTime() % (long)Math.max(1, layer.interval) != 0L || (opt = this.resolve(layer)) == null) continue;
            this.emitShape(level, pos, layer, opt);
        }
    }

    private ParticleOptions resolve(ParticleLayer layer) {
        String id;
        String string = id = layer.particleId == null ? "" : layer.particleId.trim();
        if (!id.equals("minecraft:dust") && !id.equals("dust")) {
            SimpleParticleType simpleParticleType4;
            ResourceLocation rl = ResourceLocation.tryParse((String)id);
            if (rl == null) {
                return null;
            }
            ParticleType type = (ParticleType)ForgeRegistries.PARTICLE_TYPES.getValue(rl);
            SimpleParticleType simpleParticleType3 = type instanceof SimpleParticleType ? (simpleParticleType4 = (SimpleParticleType)type) : null;
            return simpleParticleType3;
        }
        int color = layer.useRarityColor ? this.animColor : CrateBlockEntity.parseHex(layer.colorHex, this.animColor);
        return this.dust(color, 1.4f);
    }

    private DustParticleOptions dust(int color, float scale) {
        return new DustParticleOptions(new Vector3f((float)(color >> 16 & 0xFF) / 255.0f, (float)(color >> 8 & 0xFF) / 255.0f, (float)(color & 0xFF) / 255.0f), scale);
    }

    private double particleDensity() {
        return this.config.rarity == Rarity.COMMON ? 0.55 : 1.0;
    }

    private void emitShape(Level level, BlockPos pos, ParticleLayer layer, ParticleOptions opt) {
        double crateScale = this.config.rarity.sizeScale();
        double scale = 1.0 + (crateScale - 1.0) * 0.22;
        double cx = (double)pos.getX() + 0.5;
        double cy = (double)pos.getY() + Math.max(0.0, layer.yOffset) * scale;
        double cz = (double)pos.getZ() + 0.5;
        RandomSource rng = level.random;
        int n = Math.max(1, (int)Math.round((double)Math.max(1, layer.count) * this.particleDensity()));
        double r = layer.radius * scale;
        double sp = layer.speed;
        double spread = layer.spread * scale;
        double t = (double)this.ambientTime * 0.1;
        block11: for (int i = 0; i < n; ++i) {
            switch (layer.shape) {
                case HALO: {
                    double angle = t + (double)i * (Math.PI * 2 / (double)n);
                    level.addParticle(opt, cx + Math.cos(angle) * r, cy + 0.05 * scale * Math.sin(t * 1.7 + (double)i), cz + Math.sin(angle) * r, -Math.sin(angle) * sp, sp * 0.4, Math.cos(angle) * sp);
                    continue block11;
                }
                case RING: {
                    double angle = (double)i * (Math.PI * 2 / (double)n);
                    level.addParticle(opt, cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r, Math.cos(angle) * sp, 0.0, Math.sin(angle) * sp);
                    continue block11;
                }
                case BURST: {
                    double ax = (rng.nextDouble() - 0.5) * 2.0;
                    double az = (rng.nextDouble() - 0.5) * 2.0;
                    double ay = 0.4 + rng.nextDouble() * 0.6;
                    double mag = Math.max(0.001, Math.sqrt(ax * ax + ay * ay + az * az));
                    level.addParticle(opt, cx, cy, cz, ax / mag * (sp + spread), ay / mag * (sp + spread), az / mag * (sp + spread));
                    continue block11;
                }
                case COLUMN: {
                    level.addParticle(opt, cx + (rng.nextDouble() - 0.5) * spread, cy + rng.nextDouble() * (0.4 * scale + r), cz + (rng.nextDouble() - 0.5) * spread, 0.0, sp, 0.0);
                    continue block11;
                }
                case SPIRAL: {
                    double tt = (double)this.ambientTime * 0.18;
                    double frac = (double)((i + (int)(this.ambientTime % 3.0f)) % n) / (double)n;
                    double ang = tt + frac * (Math.PI * 4);
                    double rr = r * (1.05 - frac * 0.4);
                    level.addParticle(opt, cx + Math.cos(ang) * rr, cy + frac * 1.5 * scale, cz + Math.sin(ang) * rr, -Math.sin(ang) * sp, sp + 0.02, Math.cos(ang) * sp);
                    continue block11;
                }
                case FOUNTAIN: {
                    double angle = rng.nextDouble() * Math.PI * 2.0;
                    level.addParticle(opt, cx, cy, cz, Math.cos(angle) * spread, sp + rng.nextDouble() * 0.15, Math.sin(angle) * spread);
                    continue block11;
                }
                case VORTEX: {
                    double angle = t * 4.0 + (double)i * (Math.PI * 2 / (double)n);
                    double rr2 = r * (0.6 + 0.4 * Math.sin(t * 2.0 + (double)i));
                    level.addParticle(opt, cx + Math.cos(angle) * rr2, cy + rng.nextDouble() * 0.5 * scale, cz + Math.sin(angle) * rr2, -Math.cos(angle) * sp * 2.0, sp, -Math.sin(angle) * sp * 2.0);
                    continue block11;
                }
                case RAIN: {
                    level.addParticle(opt, cx + (rng.nextDouble() - 0.5) * (spread + r * 2.0), cy + rng.nextDouble() * 0.5 * scale, cz + (rng.nextDouble() - 0.5) * (spread + r * 2.0), 0.0, -sp, 0.0);
                    continue block11;
                }
                case POINT: {
                    level.addParticle(opt, cx, cy, cz, (rng.nextDouble() - 0.5) * sp, rng.nextDouble() * sp, (rng.nextDouble() - 0.5) * sp);
                }
            }
        }
    }

    private void emitAccent(Level level, BlockPos pos) {
        if (!this.instant) {
            int t = this.animTick;
            double cx = (double)pos.getX() + 0.5;
            double cz = (double)pos.getZ() + 0.5;
            double cyTop = (double)pos.getY() + 1.5;
            RandomSource rng = level.random;
            if (t >= this.tOpenEnd && t < this.tSpinStop && t % 3 == 0) {
                ParticleOptions amb = this.themeParticle(this.animation.theme());
                double ang = rng.nextDouble() * (Math.PI * 2);
                double rad = 0.5 + rng.nextDouble() * 0.2;
                level.addParticle(amb, cx + Math.cos(ang) * rad, (double)pos.getY() + 0.2 + rng.nextDouble() * 0.5, cz + Math.sin(ang) * rad, 0.0, 0.02 + rng.nextDouble() * 0.03, 0.0);
            }
            if (t >= this.tSpinStop && t < this.tHoldEnd && t % 2 == 0) {
                int i;
                ParticleOptions fin = this.finaleParticle(this.effectRarity);
                int burst = t < this.tSpinStop + 12 ? 6 : 2;
                for (i = 0; i < burst; ++i) {
                    double a2 = rng.nextDouble() * (Math.PI * 2);
                    double s = 0.2 + rng.nextDouble() * 0.5;
                    level.addParticle(fin, cx, cyTop, cz, Math.cos(a2) * s, 0.15 + rng.nextDouble() * 0.3, Math.sin(a2) * s);
                }
                for (i = 0; i < 3; ++i) {
                    double a3 = rng.nextDouble() * (Math.PI * 2);
                    double s2 = 0.15 + rng.nextDouble() * 0.35;
                    level.addParticle((ParticleOptions)this.dust(this.animColor, 1.5f), cx, cyTop, cz, Math.cos(a3) * s2, 0.1 + rng.nextDouble() * 0.2, Math.sin(a3) * s2);
                }
            }
            if (t >= this.tHoldEnd && t < this.animTotal && t % 2 == 0) {
                double a4 = rng.nextDouble() * (Math.PI * 2);
                double rad2 = 0.2 + rng.nextDouble() * 0.25;
                level.addParticle((ParticleOptions)this.dust(this.config.rarity.rgb(), 1.2f), cx + Math.cos(a4) * rad2, cyTop - 0.2, cz + Math.sin(a4) * rad2, 0.0, -0.06, 0.0);
            }
        }
    }

    private ParticleOptions finaleParticle(Rarity r) {
        return switch (r) {
            case COMMON -> ParticleTypes.END_ROD;
            case RARE -> ParticleTypes.GLOW;
            case EPIC -> ParticleTypes.WITCH;
            case LEGENDARY -> ParticleTypes.FIREWORK;
            case MYTHIC -> ParticleTypes.FLAME;
            default -> ParticleTypes.FIREWORK;
        };
    }

    private void emitBuildupSpiral(Level level, BlockPos pos) {
        int t;
        if (!this.instant && this.tSpiralEnd > 1 && (t = this.animTick) > 0 && t < this.tSpiralEnd) {
            boolean common;
            Rarity r = this.config.rarity;
            boolean bl = common = r == Rarity.COMMON;
            if (!common || ((int)this.ambientTime & 1) != 1) {
                double pscale = 1.0 + ((double)r.sizeScale() - 1.0) * 0.22;
                float p = Math.min(1.0f, (float)t / (float)Math.max(1, this.tSpiralEnd));
                double cx = (double)pos.getX() + 0.5;
                double cz = (double)pos.getZ() + 0.5;
                double baseY = (double)pos.getY() + 0.1;
                DustParticleOptions dust = this.dust(r.rgb(), 1.2f);
                ParticleOptions spark = this.openingSparkle(r);
                int arms = common ? 1 + Math.round(p * 1.0f) : 2 + Math.round(p * 4.0f);
                double turns = 2.0 + (double)p * 1.5;
                double height = (1.25 + (double)p * 0.45) * pscale;
                double baseR = 0.55 * pscale;
                double spin = (double)this.ambientTime * 0.3;
                int steps = common ? 2 : 3;
                for (int a = 0; a < arms; ++a) {
                    double armOff = (double)a * (Math.PI * 2 / (double)arms);
                    for (int s = 0; s < steps; ++s) {
                        boolean addSpark;
                        double frac = ((double)s + (double)(this.ambientTime % 4.0f) * 0.25) / (double)steps;
                        double ang = spin + armOff + frac * turns * (Math.PI * 2);
                        double rr = baseR * (1.05 - frac * 0.45);
                        double px = cx + Math.cos(ang) * rr;
                        double pz = cz + Math.sin(ang) * rr;
                        double py = baseY + frac * height;
                        double vTan = 0.04 + (double)p * 0.05;
                        level.addParticle((ParticleOptions)dust, px, py, pz, -Math.sin(ang) * vTan, 0.02 + (double)p * 0.03, Math.cos(ang) * vTan);
                        addSpark = common ? (s == steps - 1) : (s == steps - 1 || p > 0.6f);
                        if (!addSpark) continue;
                        level.addParticle(spark, px, py, pz, -Math.sin(ang) * vTan * 0.6, 0.03, Math.cos(ang) * vTan * 0.6);
                    }
                }
            }
        }
    }

    private void emitSpiralBurst(Level level, BlockPos pos) {
        if (!this.instant && this.tSpiralEnd > 1 && this.animTick == this.tSpiralEnd - 1) {
            Rarity r = this.config.rarity;
            int tier = r.ordinal();
            double scale = 1.0 + ((double)r.sizeScale() - 1.0) * 0.22;
            RandomSource rng = level.random;
            double cx = (double)pos.getX() + 0.5;
            double cz = (double)pos.getZ() + 0.5;
            double y = (double)pos.getY() + 0.6 * scale;
            double rad = 0.5 * scale;
            DustParticleOptions dust = this.dust(r.rgb(), 1.4f);
            ParticleOptions spark = this.openingSparkle(r);
            float power = 1.0f + (float)tier * 0.35f;
            level.addParticle((ParticleOptions)ParticleTypes.FLASH, cx, y, cz, 0.0, 0.0, 0.0);
            int puffs = 2 + tier;
            for (int i = 0; i < puffs; ++i) {
                level.addParticle((ParticleOptions)ParticleTypes.EXPLOSION, cx + (rng.nextDouble() - 0.5) * rad, y + (rng.nextDouble() - 0.5) * 0.3 * scale, cz + (rng.nextDouble() - 0.5) * rad, 0.0, 0.0, 0.0);
            }
            puffs = 20 + tier * 6;
            for (int j = 0; j < puffs; ++j) {
                double ang = (double)j * (Math.PI * 2 / (double)puffs);
                double px = cx + Math.cos(ang) * rad;
                double pz = cz + Math.sin(ang) * rad;
                double v = 0.18 * (double)power;
                level.addParticle((ParticleOptions)dust, px, y, pz, Math.cos(ang) * v, 0.06, Math.sin(ang) * v);
                level.addParticle(spark, px, y, pz, Math.cos(ang) * v * 0.7, 0.08, Math.sin(ang) * v * 0.7);
            }
            puffs = 24 + tier * 10;
            for (int k = 0; k < puffs; ++k) {
                double ax = rng.nextDouble() - 0.5;
                double ay = rng.nextDouble() * 0.9 + 0.1;
                double az = rng.nextDouble() - 0.5;
                double mag = Math.max(0.001, Math.sqrt(ax * ax + ay * ay + az * az));
                double sp = (0.25 + rng.nextDouble() * 0.35) * (double)power;
                ParticleOptions p = k % 3 == 0 ? ParticleTypes.FIREWORK : (k % 3 == 1 ? spark : dust);
                level.addParticle((ParticleOptions)p, cx, y, cz, ax / mag * sp, ay / mag * sp, az / mag * sp);
            }
            puffs = 8 + tier * 4;
            for (int l = 0; l < puffs; ++l) {
                double a = rng.nextDouble() * (Math.PI * 2);
                double rr = rng.nextDouble() * rad * 0.6;
                level.addParticle(spark, cx + Math.cos(a) * rr, y, cz + Math.sin(a) * rr, (rng.nextDouble() - 0.5) * 0.06, (0.25 + rng.nextDouble() * 0.4) * (double)power, (rng.nextDouble() - 0.5) * 0.06);
            }
        }
    }

    private ParticleOptions openingSparkle(Rarity r) {
        return switch (r) {
            case COMMON -> ParticleTypes.END_ROD;
            case RARE -> ParticleTypes.GLOW;
            case EPIC -> ParticleTypes.WITCH;
            case LEGENDARY -> ParticleTypes.ENCHANT;
            case MYTHIC -> ParticleTypes.FLAME;
            default -> ParticleTypes.END_ROD;
        };
    }

    private ParticleOptions themeParticle(CrateAnimation.Theme t) {
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

    private static int parseHex(String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        try {
            return (int)Long.parseLong(hex.replace("#", "").trim(), 16);
        }
        catch (NumberFormatException var3) {
            return fallback;
        }
    }

    private void advanceSounds() {
        if (this.instant) {
            if (this.soundStage < 60) {
                this.playWin(this.effectRarity);
                this.soundStage = 60;
            }
        } else {
            float p;
            int interval;
            int t = this.animTick;
            Rarity cr = this.config.rarity;
            if (this.soundStage == 0 && t >= 2) {
                this.playSpiralCharge(cr);
                this.soundStage = 1;
            }
            if (this.soundStage == 1 && t > 2 && t < this.tRiseEnd && t - this.lastRiseTick >= (interval = Math.max(2, Math.round(10.0f - (p = Math.min(1.0f, (float)(t - 2) / (float)Math.max(1, this.tRiseEnd - 2))) * 8.0f)))) {
                this.lastRiseTick = t;
                this.playSpiralRise(cr, p);
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
                float rp = this.revealProgress(0.0f);
                int n = this.candidates.size();
                int winner = Math.max(0, Math.min(n - 1, this.winnerIndex));
                float maxTravel = CrateBlockEntity.reelTravel(n, winner);
                int idx = (int)Math.floor(CrateBlockEntity.easeOutReel(Math.min(1.0f, rp)) * maxTravel);
                if (idx != this.lastReelIndex) {
                    this.lastReelIndex = idx;
                    float pitch = 0.9f + rp * 0.7f;
                    this.play((Holder<SoundEvent>)SoundEvents.UI_BUTTON_CLICK, 0.4f, pitch);
                }
            }
            if (t >= this.tSpinStop && this.soundStage >= 2 && this.soundStage < 60) {
                this.playWin(this.effectRarity);
                this.soundStage = 60;
                this.winTick = t;
                this.noteIndex = 0;
            } else if (this.soundStage == 60 && this.noteIndex == 0 && t - this.winTick >= 4) {
                this.playWinTail(this.effectRarity);
                this.noteIndex = 1;
            }
            if (this.soundStage >= 60 && this.soundStage < 70 && t >= this.tHoldEnd) {
                this.playClose(cr);
                this.soundStage = 70;
            }
        }
    }

    private void playUnlock(Rarity r) {
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
            }
        }
    }

    private void playSpiralCharge(Rarity r) {
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
            }
        }
    }

    private void playSpiralRise(Rarity r, float p) {
        float vol = Math.min(1.0f, 0.45f + p * 0.55f);
        switch (r) {
            case COMMON: {
                float pitch = Math.min(1.55f, 0.55f + p * 1.0f);
                this.play(SoundEvents.BEACON_POWER_SELECT, vol * 0.85f, pitch);
                break;
            }
            case RARE: {
                float pitch = Math.min(1.5f, 0.55f + p * 0.95f);
                this.play(SoundEvents.ENCHANTMENT_TABLE_USE, vol, pitch);
                if (!(p > 0.55f)) break;
                this.play(SoundEvents.CONDUIT_AMBIENT, vol * 0.3f, 0.85f + p * 0.4f);
                break;
            }
            case EPIC: {
                float pitch = Math.min(1.5f, 0.5f + p * 1.0f);
                this.play(SoundEvents.ENCHANTMENT_TABLE_USE, vol, pitch);
                if (p > 0.3f) {
                    this.play(SoundEvents.EVOKER_CAST_SPELL, vol * (0.1f + p * 0.35f), 0.6f + p * 0.6f);
                }
                if (!(p > 0.8f)) break;
                this.play(SoundEvents.CONDUIT_ACTIVATE, vol * 0.4f, 0.85f + p * 0.3f);
                break;
            }
            case LEGENDARY: {
                float pitch = Math.min(1.25f, 0.4f + p * 0.85f);
                this.play(SoundEvents.BEACON_POWER_SELECT, vol, pitch);
                if (p > 0.35f) {
                    this.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.15f + p * 0.25f, 0.7f + p * 0.45f);
                }
                if (!(p > 0.75f)) break;
                this.play((Holder<SoundEvent>)SoundEvents.RAID_HORN, vol * 0.28f, 0.72f + p * 0.32f);
                break;
            }
            case MYTHIC: {
                float pitch = Math.min(1.05f, 0.3f + p * 0.75f);
                this.play(SoundEvents.BEACON_POWER_SELECT, vol, pitch);
                if (p > 0.2f) {
                    this.play(SoundEvents.WARDEN_HEARTBEAT, vol * (0.2f + p * 0.4f), 0.55f + p * 0.5f);
                }
                if (p > 0.5f) {
                    this.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.2f + p * 0.25f, 0.65f + p * 0.45f);
                }
                if (!(p > 0.88f)) break;
                this.play(SoundEvents.ENDER_DRAGON_GROWL, 0.18f, 1.2f);
            }
        }
    }

    private static int peakHoldTicks(Rarity r) {
        return 44;
    }

    private static int spiralBonusTicks(Rarity r) {
        return 24;
    }

    private void playSpiralPeak(Rarity r) {
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
            }
        }
    }

    private void playOpenAccent(Rarity r) {
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
            }
        }
    }

    private void playWin(Rarity r) {
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
            }
        }
    }

    private void playWinTail(Rarity r) {
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
            }
        }
    }

    private void playClose(Rarity r) {
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
            }
        }
    }

    private void play(SoundEvent sound, float vol, float pitch) {
        if (this.level != null && sound != null) {
            this.level.playLocalSound((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5, sound, SoundSource.BLOCKS, vol, pitch, false);
        }
    }

    private void play(Holder<SoundEvent> sound, float vol, float pitch) {
        if (sound != null) {
            this.play((SoundEvent)sound.value(), vol, pitch);
        }
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c2 = 2.70158f;
        float x = t - 1.0f;
        return 1.0f + 2.70158f * x * x * x + 1.70158f * x * x;
    }

    private static float easeInOut(float t) {
        return t < 0.5f ? 2.0f * t * t : 1.0f - (float)Math.pow(-2.0f * t + 2.0f, 2.0) / 2.0f;
    }

    public static float easeOutReel(float t) {
        float x = 1.0f - t;
        return 1.0f - x * x * x * x * x;
    }

    public static float reelTravel(int n, int winner) {
        return n <= 0 ? 180.0f : (float)(180 + Math.floorMod(winner - 180, n));
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("config", (Tag)this.config.save());
        ListTag opened = new ListTag();
        for (UUID id : this.openedBy) {
            opened.add(StringTag.valueOf((String)id.toString()));
        }
        tag.put("openedBy", (Tag)opened);
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("config")) {
            this.config = CrateConfig.load(tag.getCompound("config"));
            this.animColor = this.config.rarity.rgb();
        }
        this.openedBy.clear();
        if (tag.contains("openedBy")) {
            ListTag opened = tag.getList("openedBy", 8);
            for (int i = 0; i < opened.size(); ++i) {
                try {
                    this.openedBy.add(UUID.fromString(opened.getString(i)));
                    continue;
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    // empty catch block
                }
            }
        }
    }

    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("config", (Tag)this.config.save());
        return tag;
    }

    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains("config")) {
            this.config = CrateConfig.load(tag.getCompound("config"));
            this.animColor = this.config.rarity.rgb();
        }
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null && tag.contains("config")) {
            this.config = CrateConfig.load(tag.getCompound("config"));
            this.animColor = this.config.rarity.rgb();
        }
    }

    public static List<ItemStack> decodeItems(CompoundTag wrap) {
        ArrayList<ItemStack> out = new ArrayList<ItemStack>();
        if (wrap == null) {
            return out;
        }
        ListTag list = wrap.getList("items", 10);
        for (int i = 0; i < list.size(); ++i) {
            out.add(ItemStack.of((CompoundTag)list.getCompound(i)));
        }
        return out;
    }
}

