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
    public boolean animating;
    public int animTick;
    public int animTotal;
    private CrateAnimation animation;
    private int animColor;
    private ItemStack rewardIcon;
    private final List<ItemStack> candidates;
    private int winnerIndex;
    private int soundStage;
    private int winTick;
    private int noteIndex;
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
        this.soundStage = 0;
        this.winTick = -1;
        this.noteIndex = 0;
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
    
    public void startAnimation(final String animationId, final int rarityColor, final int winnerIndex, final List<ItemStack> cands) {
        this.animation = AnimationRegistry.get(animationId);
        this.animTotal = Math.max(6, this.animation.durationTicks());
        this.animColor = rarityColor;
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
        this.animating = true;
        if (this.level != null) {
            this.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.4f, 0.8f);
            this.play((SoundEvent)SoundEvents.NOTE_BLOCK_HARP.value(), 0.35f, 0.6f);
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
        final CrateAnimation.Theme theme = this.animation.theme();
        if (p >= 0.22f && p < 0.88f && this.animTick % 2 == 0) {
            final double a = this.ambientTime * 0.3;
            final double rad = 0.55;
            final ParticleOptions swirl = switch (theme) {
                case INFERNAL -> ParticleTypes.FLAME;
                case CELESTIAL -> ParticleTypes.END_ROD;
                case MAGIC -> ParticleTypes.WITCH;
                case ANCIENT -> ParticleTypes.ENCHANT;
                case NATURE -> ParticleTypes.HAPPY_VILLAGER;
                case NEON,  CASINO -> this.dust(this.animColor, 1.2f);
                default -> ParticleTypes.CRIT;
            };
            level.addParticle(swirl, cx + Math.cos(a) * rad, cyTop + 0.1 * Math.sin(a * 2.0), cz + Math.sin(a) * rad, 0.0, 0.04, 0.0);
            level.addParticle(swirl, cx + Math.cos(a + 3.141592653589793) * rad, cyTop, cz + Math.sin(a + 3.141592653589793) * rad, 0.0, 0.04, 0.0);
        }
        if (p >= 0.88f && this.animTick % 2 == 0) {
            for (int i = 0; i < 4; ++i) {
                final double a2 = rng.nextDouble() * 3.141592653589793 * 2.0;
                final double s = 0.2 + rng.nextDouble() * 0.4;
                final ParticleOptions fin = switch (theme) {
                    case INFERNAL -> ParticleTypes.LAVA;
                    case CELESTIAL -> ParticleTypes.END_ROD;
                    case NATURE -> ParticleTypes.HAPPY_VILLAGER;
                    case MAGIC,  ANCIENT -> ParticleTypes.TOTEM_OF_UNDYING;
                    default -> ParticleTypes.FIREWORK;
                };
                level.addParticle(fin, cx, cyTop, cz, Math.cos(a2) * s, 0.15 + rng.nextDouble() * 0.25, Math.sin(a2) * s);
            }
        }
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
        if (this.soundStage == 0 && p >= 0.1f) {
            this.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f, 1.1f);
            this.play((SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value(), 0.4f, 1.0f);
            this.play(SoundEvents.CHEST_OPEN, 0.4f, 1.1f);
            if (this.config.rarity.ordinal() >= Rarity.EPIC.ordinal()) {
                this.play(SoundEvents.BEACON_ACTIVATE, 0.4f, 1.3f);
            }
            this.soundStage = 1;
        }
        if (this.soundStage >= 1 && p >= 0.22f && p < 0.88f) {
            final float rp = this.revealProgress(0.0f);
            final int interval = 2 + (int)(rp * 10.0f);
            if (this.animTick % Math.max(2, interval) == 0) {
                final float pitch = 0.8f + rp * rp * 1.2f;
                this.play((SoundEvent)SoundEvents.NOTE_BLOCK_HARP.value(), 0.4f, pitch);
            }
        }
        if (p >= 0.88f) {
            if (this.soundStage < 60) {
                this.playWinImpact();
                this.soundStage = 60;
                this.winTick = this.animTick;
                this.noteIndex = 0;
            }
            else {
                final float[] notes = this.arpeggio();
                if (this.noteIndex < notes.length && this.animTick - this.winTick >= this.noteIndex * 2L) {
                    final float n = notes[this.noteIndex];
                    this.play((SoundEvent)SoundEvents.NOTE_BLOCK_CHIME.value(), 0.5f, n);
                    this.play((SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value(), 0.4f, n);
                    ++this.noteIndex;
                    if (this.noteIndex == notes.length) {
                        this.playWinFlourish();
                    }
                }
            }
        }
    }
    
    private void playWinImpact() {
        this.play((SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, 1.0f);
        this.play(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.2f);
        if (this.config.rarity.ordinal() >= Rarity.EPIC.ordinal()) {
            this.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.0f);
        }
        if (this.config.rarity == Rarity.MYTHIC) {
            this.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.45f, 1.1f);
        }
    }
    
    private float[] arpeggio() {
        return switch (this.config.rarity) {
            default -> throw new IncompatibleClassChangeError();
            case COMMON -> new float[] { 1.0f, 1.5f };
            case RARE -> new float[] { 1.0f, 1.26f, 1.5f };
            case EPIC -> new float[] { 1.0f, 1.26f, 1.5f, 2.0f };
            case LEGENDARY -> new float[] { 0.84f, 1.0f, 1.26f, 1.5f, 2.0f };
            case MYTHIC -> new float[] { 0.84f, 1.0f, 1.26f, 1.5f, 1.68f, 2.0f };
        };
    }
    
    private void playWinFlourish() {
        if (this.config.rarity.ordinal() >= Rarity.LEGENDARY.ordinal()) {
            this.play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.5f, 1.0f);
            this.play((SoundEvent)SoundEvents.RAID_HORN.value(), 0.35f, 1.4f);
        }
        if (this.config.rarity == Rarity.MYTHIC) {
            this.play(SoundEvents.TOTEM_USE, 0.6f, 1.0f);
            this.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
            this.play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.55f, 1.4f);
        }
    }
    
    private void play(final SoundEvent sound, final float vol, final float pitch) {
        if (this.level == null || sound == null) {
            return;
        }
        this.level.playLocalSound(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, sound, SoundSource.BLOCKS, vol, pitch, false);
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
