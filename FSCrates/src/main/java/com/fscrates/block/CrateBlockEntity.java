package com.fscrates.block;

import com.fscrates.animation.AnimationRegistry;
import com.fscrates.animation.CrateAnimation;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.ParticleLayer;
import com.fscrates.config.Rarity;
import com.fscrates.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores a placed crate's {@link CrateConfig} (server) and drives the entire
 * in-world opening animation (client): the chest jolts and opens, a strip of
 * candidate rewards spins like a Trial-Chamber vault and decelerates onto the
 * winner (which floats clearly above the chest), all framed by fully editable
 * particle layers and per-rarity sounds. No GUI window is involved.
 */
public class CrateBlockEntity extends BlockEntity {

    private CrateConfig config = new CrateConfig();

    // ----- client animation state -----
    public boolean animating = false;
    public int animTick = 0;
    public int animTotal = 70;
    private CrateAnimation animation = AnimationRegistry.get(AnimationRegistry.defaultId());
    private int animColor = 0xFFFFFF;
    private ItemStack rewardIcon = ItemStack.EMPTY;
    private final List<ItemStack> candidates = new ArrayList<>();
    private int winnerIndex = 0;
    private int soundStage = 0;
    public float ambientTime = 0f;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.CRATE_BE.get(), pos, state);
    }

    public CrateConfig getConfig() { return config; }

    public void setConfig(CrateConfig config) {
        this.config = config == null ? new CrateConfig() : config;
        this.animColor = this.config.rarity.rgb();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public Rarity getRarity() { return config.rarity; }
    public CrateAnimation getAnimation() { return animation; }
    public int getAnimColor() { return animColor; }
    public ItemStack getRewardIcon() { return rewardIcon; }
    public List<ItemStack> getCandidates() { return candidates; }
    public int getWinnerIndex() { return winnerIndex; }

    // ------------------------------------------------------------------
    // Client animation control
    // ------------------------------------------------------------------

    public void startAnimation(String animationId, int rarityColor, ItemStack reward, List<ItemStack> cands) {
        this.animation = AnimationRegistry.get(animationId);
        this.animTotal = Math.max(6, animation.durationTicks());
        this.animColor = rarityColor;
        this.rewardIcon = reward == null ? ItemStack.EMPTY : reward;
        this.candidates.clear();
        if (cands != null) {
            for (ItemStack s : cands) {
                if (s != null && !s.isEmpty()) {
                    this.candidates.add(s);
                }
            }
        }
        if (!this.rewardIcon.isEmpty() && !containsItem(this.candidates, this.rewardIcon)) {
            this.candidates.add(this.rewardIcon);
        }
        if (this.candidates.isEmpty() && !this.rewardIcon.isEmpty()) {
            this.candidates.add(this.rewardIcon);
        }
        // winner index = position of the reward icon among candidates
        this.winnerIndex = 0;
        for (int i = 0; i < this.candidates.size(); i++) {
            if (ItemStack.isSameItemSameTags(this.candidates.get(i), this.rewardIcon)) {
                this.winnerIndex = i;
                break;
            }
        }
        this.animTick = 0;
        this.soundStage = 0;
        this.animating = true;
        if (level != null) {
            playStartSounds();
        }
    }

    private static boolean containsItem(List<ItemStack> list, ItemStack s) {
        for (ItemStack i : list) {
            if (ItemStack.isSameItemSameTags(i, s)) {
                return true;
            }
        }
        return false;
    }

    public float progress() {
        return animating ? Math.min(1f, animTick / (float) Math.max(1, animTotal)) : 0f;
    }

    public ParticleLayer.Phase currentPhase() {
        if (!animating) {
            return ParticleLayer.Phase.IDLE;
        }
        float p = progress();
        if (p < 0.16f) return ParticleLayer.Phase.ANTICIPATION;
        if (p < 0.42f) return ParticleLayer.Phase.OPEN;
        if (p < 0.82f) return ParticleLayer.Phase.REVEAL;
        return ParticleLayer.Phase.FINALE;
    }

    /** Lid open amount 0..1 with a closing tail. */
    public float lidOpen(float partial) {
        if (!animating) return 0f;
        float p = (animTick + partial) / Math.max(1, animTotal);
        if (p < 0.10f) return 0f;
        if (p < 0.30f) return easeOutBack((p - 0.10f) / 0.20f);
        if (p < 0.92f) return 1f;
        return 1f - easeInOut(Math.min(1f, (p - 0.92f) / 0.08f));
    }

    public float shake(float partial) {
        if (!animating) return 0f;
        float p = (animTick + partial) / Math.max(1, animTotal);
        if (p >= 0.16f) return 0f;
        float intensity = (0.16f - p) / 0.16f;
        return (float) Math.sin((animTick + partial) * 2.1f) * 0.05f * intensity;
    }

    /** Reveal progress 0..1 (during the REVEAL phase), eased for the roulette. */
    public float revealProgress(float partial) {
        float p = (animTick + partial) / Math.max(1, animTotal);
        if (p <= 0.42f) return 0f;
        if (p >= 0.82f) return 1f;
        return (p - 0.42f) / 0.40f;
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void clientTick(Level level, BlockPos pos, BlockState state, CrateBlockEntity be) {
        be.ambientTime += 1f;
        if (be.animating) {
            be.animTick++;
            be.emitLayers(level, pos, be.currentPhase());
            be.advanceSounds(level);
            if (be.animTick >= be.animTotal) {
                be.animating = false;
                be.animTick = 0;
                be.rewardIcon = ItemStack.EMPTY;
                be.candidates.clear();
            }
        } else if (be.config.particles) {
            be.emitLayers(level, pos, ParticleLayer.Phase.IDLE);
        }
    }

    // ------------------------------------------------------------------
    // Data-driven particles
    // ------------------------------------------------------------------

    private void emitLayers(Level level, BlockPos pos, ParticleLayer.Phase phase) {
        for (ParticleLayer layer : config.particleLayers) {
            if (layer.phase != phase) {
                continue;
            }
            if (phase == ParticleLayer.Phase.IDLE) {
                if (level.getGameTime() % Math.max(1, layer.interval) != 0) {
                    continue;
                }
            }
            ParticleOptions opt = resolve(layer);
            if (opt == null) {
                continue;
            }
            emitShape(level, pos, layer, opt);
        }
    }

    private ParticleOptions resolve(ParticleLayer layer) {
        String id = layer.particleId == null ? "" : layer.particleId;
        if (id.endsWith("dust") || id.endsWith("dust_color") || id.equals("minecraft:dust")) {
            int color = layer.useRarityColor ? animColor : parseHex(layer.colorHex, animColor);
            Vector3f c = new Vector3f(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f);
            return new DustParticleOptions(c, 1.3f);
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            return null;
        }
        ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(rl);
        if (type instanceof SimpleParticleType simple) {
            return simple;
        }
        return null; // particle needs extra data we can't synthesise generically
    }

    private void emitShape(Level level, BlockPos pos, ParticleLayer layer, ParticleOptions opt) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + layer.yOffset;
        double cz = pos.getZ() + 0.5;
        var rng = level.random;
        int n = Math.max(1, layer.count);
        double r = layer.radius;
        double sp = layer.speed;
        double spread = layer.spread;
        double t = ambientTime * 0.1;

        for (int i = 0; i < n; i++) {
            double angle;
            switch (layer.shape) {
                case HALO -> {
                    angle = t + i * (Math.PI * 2 / n);
                    level.addParticle(opt, cx + Math.cos(angle) * r, cy + 0.1 * Math.sin(t * 1.7 + i), cz + Math.sin(angle) * r,
                            0, sp, 0);
                }
                case RING -> {
                    angle = i * (Math.PI * 2 / n);
                    level.addParticle(opt, cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r,
                            Math.cos(angle) * sp, 0.01, Math.sin(angle) * sp);
                }
                case BURST -> {
                    double ax = (rng.nextDouble() - 0.5);
                    double ay = rng.nextDouble();
                    double az = (rng.nextDouble() - 0.5);
                    level.addParticle(opt, cx, cy, cz, ax * sp, ay * sp + 0.05, az * sp);
                }
                case COLUMN -> level.addParticle(opt,
                        cx + (rng.nextDouble() - 0.5) * spread, cy + rng.nextDouble() * (0.4 + r), cz + (rng.nextDouble() - 0.5) * spread,
                        0, sp, 0);
                case SPIRAL -> {
                    angle = t * 3 + i * 0.7;
                    double rr = r * (0.3 + (i / (double) n) * 0.7);
                    level.addParticle(opt, cx + Math.cos(angle) * rr, cy + (i / (double) n) * 0.8, cz + Math.sin(angle) * rr,
                            0, sp, 0);
                }
                case FOUNTAIN -> {
                    angle = rng.nextDouble() * Math.PI * 2;
                    level.addParticle(opt, cx, cy, cz, Math.cos(angle) * spread, sp + rng.nextDouble() * 0.1, Math.sin(angle) * spread);
                }
                case VORTEX -> {
                    angle = t * 4 + i * (Math.PI * 2 / n);
                    level.addParticle(opt, cx + Math.cos(angle) * r, cy + rng.nextDouble() * 0.4, cz + Math.sin(angle) * r,
                            -Math.cos(angle) * sp * 2, sp, -Math.sin(angle) * sp * 2);
                }
                case RAIN -> level.addParticle(opt,
                        cx + (rng.nextDouble() - 0.5) * (spread + r * 2), cy + 1.3 + rng.nextDouble() * 0.5, cz + (rng.nextDouble() - 0.5) * (spread + r * 2),
                        0, -sp, 0);
                case POINT -> level.addParticle(opt, cx, cy, cz,
                        (rng.nextDouble() - 0.5) * sp, rng.nextDouble() * sp, (rng.nextDouble() - 0.5) * sp);
            }
        }
    }

    private static int parseHex(String hex, int fallback) {
        if (hex == null) return fallback;
        try {
            return (int) Long.parseLong(hex.replace("#", "").trim(), 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ------------------------------------------------------------------
    // Per-rarity sounds: soft and epic, scaling with tier
    // ------------------------------------------------------------------

    private void playStartSounds() {
        // a soft magical "unlock" cue, pitch rising with tier
        float pitch = 0.8f + config.rarity.ordinal() * 0.06f;
        play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.55f, pitch);
        play(SoundEvents.CHEST_OPEN, 0.35f, 1.1f);
    }

    private void advanceSounds(Level level) {
        float p = progress();
        if (soundStage == 0 && p >= 0.16f) {
            // lid swings open
            play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f, 1.1f);
            if (config.rarity.ordinal() >= Rarity.EPIC.ordinal()) {
                play(SoundEvents.BEACON_ACTIVATE, 0.4f, 1.3f);
            }
            soundStage = 1;
        }
        if (soundStage >= 1 && soundStage < 50 && p >= 0.42f && p < 0.82f) {
            // roulette ticks, slowing down as the reveal eases out
            float rp = revealProgress(0f);
            int interval = 2 + (int) (rp * 7);
            if (animTick % Math.max(2, interval) == 0) {
                float tickPitch = 1.2f + rp * 0.8f;
                play(SoundEvents.NOTE_BLOCK_HAT.value(), 0.4f, tickPitch);
            }
            soundStage = 1;
        }
        if (soundStage < 60 && p >= 0.82f) {
            playWinSounds();
            soundStage = 60;
        }
    }

    private void playWinSounds() {
        switch (config.rarity) {
            case COMMON -> play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, 1.2f);
            case RARE -> {
                play(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.7f, 1.1f);
                play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.4f);
            }
            case EPIC -> {
                play(SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.2f);
                play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, 1.5f);
            }
            case LEGENDARY -> {
                play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
                play(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.6f, 1.6f);
            }
            case MYTHIC -> {
                play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 0.9f);
                play(SoundEvents.TOTEM_USE, 0.6f, 1.1f);
                play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.5f, 1.2f);
            }
        }
    }

    private void play(SoundEvent sound, float vol, float pitch) {
        if (level == null || sound == null) {
            return;
        }
        level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                sound, SoundSource.BLOCKS, vol, pitch, false);
    }

    // ------------------------------------------------------------------
    // Easing
    // ------------------------------------------------------------------

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1f, x = t - 1f;
        return 1f + c3 * x * x * x + c1 * x * x;
    }

    private static float easeInOut(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("config", config.save());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("config")) {
            config = CrateConfig.load(tag.getCompound("config"));
            animColor = config.rarity.rgb();
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("config", config.save());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains("config")) {
            config = CrateConfig.load(tag.getCompound("config"));
            animColor = config.rarity.rgb();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null && tag.contains("config")) {
            config = CrateConfig.load(tag.getCompound("config"));
            animColor = config.rarity.rgb();
        }
    }

    public static List<ItemStack> decodeItems(CompoundTag wrap) {
        List<ItemStack> out = new ArrayList<>();
        if (wrap == null) {
            return out;
        }
        ListTag list = wrap.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            out.add(ItemStack.of(list.getCompound(i)));
        }
        return out;
    }
}
