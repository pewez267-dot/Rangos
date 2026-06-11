package com.fscrates.block;

import com.fscrates.animation.AnimationRegistry;
import com.fscrates.animation.CrateAnimation;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores a placed crate's {@link CrateConfig} (server) and drives the entire
 * in-world opening animation (client). The animation lives on the crate itself —
 * the lid opens, the chest jolts, particles erupt and the reward floats out —
 * rather than in any GUI window.
 */
public class CrateBlockEntity extends BlockEntity {

    private CrateConfig config = new CrateConfig();

    // ----- client animation state -----
    public boolean animating = false;
    public int animTick = 0;
    public int animTotal = 60;
    private CrateAnimation animation = AnimationRegistry.get(AnimationRegistry.defaultId());
    private int animColor = 0xFFFFFF;
    private ItemStack rewardIcon = ItemStack.EMPTY;
    private final List<ItemStack> candidates = new ArrayList<>();
    private int soundStage = 0;
    /** Smooth ambient bob, advanced every client tick. */
    public float ambientTime = 0f;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.CRATE_BE.get(), pos, state);
    }

    public CrateConfig getConfig() {
        return config;
    }

    public void setConfig(CrateConfig config) {
        this.config = config == null ? new CrateConfig() : config;
        this.animColor = this.config.rarity.rgb();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public Rarity getRarity() {
        return config.rarity;
    }

    public CrateAnimation getAnimation() {
        return animation;
    }

    public int getAnimColor() {
        return animColor;
    }

    public ItemStack getRewardIcon() {
        return rewardIcon;
    }

    public List<ItemStack> getCandidates() {
        return candidates;
    }

    // ------------------------------------------------------------------
    // Client animation control (called from the network handler)
    // ------------------------------------------------------------------

    public void startAnimation(String animationId, int rarityColor, ItemStack reward, List<ItemStack> cands) {
        this.animation = AnimationRegistry.get(animationId);
        this.animTotal = Math.max(6, animation.durationTicks());
        this.animColor = rarityColor;
        this.rewardIcon = reward == null ? ItemStack.EMPTY : reward;
        this.candidates.clear();
        if (cands != null) {
            this.candidates.addAll(cands);
        }
        if (this.candidates.isEmpty() && !this.rewardIcon.isEmpty()) {
            this.candidates.add(this.rewardIcon);
        }
        this.animTick = 0;
        this.soundStage = 0;
        this.animating = true;
        playStageSound(0);
    }

    /** Normalised animation progress 0..1. */
    public float progress() {
        return animating ? Math.min(1f, animTick / (float) Math.max(1, animTotal)) : 0f;
    }

    /** Lid open amount 0 (closed) .. 1 (fully open), with a closing tail. */
    public float lidOpen(float partial) {
        if (!animating) {
            return 0f;
        }
        float p = (animTick + partial) / Math.max(1, animTotal);
        if (p < 0.18f) {
            return 0f;
        }
        if (p < 0.45f) {
            float t = (p - 0.18f) / 0.27f;
            return easeOutBack(t);
        }
        if (p < 0.9f) {
            return 1f;
        }
        // closing tail
        float t = (p - 0.9f) / 0.1f;
        return 1f - easeInOut(Math.min(1f, t));
    }

    /** Horizontal shake offset during the anticipation phase. */
    public float shake(float partial) {
        if (!animating) {
            return 0f;
        }
        float p = (animTick + partial) / Math.max(1, animTotal);
        if (p >= 0.18f) {
            return 0f;
        }
        float intensity = (0.18f - p) / 0.18f;
        return (float) Math.sin((animTick + partial) * 1.9f) * 0.05f * intensity;
    }

    // ------------------------------------------------------------------
    // Tickers
    // ------------------------------------------------------------------

    public static void clientTick(Level level, BlockPos pos, BlockState state, CrateBlockEntity be) {
        be.ambientTime += 1f;
        if (be.animating) {
            be.animTick++;
            be.emitAnimationParticles(level, pos);
            be.advanceSounds(level, pos);
            if (be.animTick >= be.animTotal) {
                be.animating = false;
                be.animTick = 0;
                be.rewardIcon = ItemStack.EMPTY;
                be.candidates.clear();
            }
        } else if (be.config.particles) {
            be.emitAmbientParticles(level, pos);
        }
    }

    // ------------------------------------------------------------------
    // Particles
    // ------------------------------------------------------------------

    private ParticleOptions dust() {
        Vector3f c = new Vector3f(
                ((animColor >> 16) & 0xFF) / 255f,
                ((animColor >> 8) & 0xFF) / 255f,
                (animColor & 0xFF) / 255f);
        return new DustParticleOptions(c, 1.4f);
    }

    private void emitAmbientParticles(Level level, BlockPos pos) {
        if (level.getGameTime() % 6 != 0) {
            return;
        }
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.55;
        double cz = pos.getZ() + 0.5;
        double ang = (ambientTime * 0.12) % (Math.PI * 2);
        double r = 0.45;
        level.addParticle(dust(), cx + Math.cos(ang) * r, cy + 0.15 * Math.sin(ambientTime * 0.18), cz + Math.sin(ang) * r,
                0, 0.01, 0);
        if (level.getGameTime() % 18 == 0) {
            level.addParticle(ParticleTypes.ENCHANT, cx, cy + 0.6, cz, 0, -0.02, 0);
        }
    }

    private void emitAnimationParticles(Level level, BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.7;
        double cz = pos.getZ() + 0.5;
        float p = progress();
        CrateAnimation.Theme theme = animation.theme();
        var rng = level.random;

        if (p < 0.18f) {
            // anticipation: charge swirl drawn inward
            for (int i = 0; i < 3; i++) {
                double a = rng.nextDouble() * Math.PI * 2;
                double rad = 0.8;
                level.addParticle(dust(),
                        cx + Math.cos(a) * rad, cy + rng.nextDouble() * 0.5, cz + Math.sin(a) * rad,
                        -Math.cos(a) * 0.06, 0.02, -Math.sin(a) * 0.06);
            }
            return;
        }

        if (p < 0.45f) {
            // opening: eruption out of the chest mouth
            for (int i = 0; i < 8; i++) {
                double vx = (rng.nextDouble() - 0.5) * 0.25;
                double vz = (rng.nextDouble() - 0.5) * 0.25;
                double vy = 0.18 + rng.nextDouble() * 0.22;
                level.addParticle(themedBurst(theme), cx, cy, cz, vx, vy, vz);
            }
            level.addParticle(dust(), cx, cy + 0.2, cz, 0, 0.1, 0);
            return;
        }

        if (p < 0.8f) {
            // reveal: a rising column of sparkles around the floating reward
            for (int i = 0; i < 5; i++) {
                double a = rng.nextDouble() * Math.PI * 2;
                double rad = 0.25 + rng.nextDouble() * 0.15;
                level.addParticle(ParticleTypes.END_ROD,
                        cx + Math.cos(a) * rad, cy + 0.3 + rng.nextDouble() * 0.6, cz + Math.sin(a) * rad,
                        0, 0.05, 0);
            }
            if (level.getGameTime() % 2 == 0) {
                level.addParticle(dust(), cx, cy + 0.8, cz, 0, 0.03, 0);
            }
            return;
        }

        // finale: celebratory outward burst
        for (int i = 0; i < 12; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double sp = 0.25 + rng.nextDouble() * 0.35;
            level.addParticle(themedFinale(theme),
                    cx, cy + 0.4, cz,
                    Math.cos(a) * sp, 0.1 + rng.nextDouble() * 0.25, Math.sin(a) * sp);
        }
    }

    private ParticleOptions themedBurst(CrateAnimation.Theme theme) {
        return switch (theme) {
            case INFERNAL -> ParticleTypes.FLAME;
            case CELESTIAL, MAGIC -> ParticleTypes.SOUL_FIRE_FLAME;
            case SCIFI -> ParticleTypes.ELECTRIC_SPARK;
            case NATURE -> ParticleTypes.HAPPY_VILLAGER;
            case CASINO, NEON -> ParticleTypes.FIREWORK;
            default -> dust();
        };
    }

    private ParticleOptions themedFinale(CrateAnimation.Theme theme) {
        return switch (theme) {
            case INFERNAL -> ParticleTypes.LAVA;
            case CELESTIAL -> ParticleTypes.END_ROD;
            case SCIFI -> ParticleTypes.ELECTRIC_SPARK;
            case NATURE -> ParticleTypes.HAPPY_VILLAGER;
            default -> ParticleTypes.FIREWORK;
        };
    }

    // ------------------------------------------------------------------
    // Sounds (vanilla events, themed)
    // ------------------------------------------------------------------

    private void advanceSounds(Level level, BlockPos pos) {
        float p = progress();
        if (soundStage == 0 && p >= 0.0f) {
            soundStage = 1; // start sound already played in startAnimation
        }
        if (soundStage == 1 && p >= 0.18f) {
            playStageSound(2);
            soundStage = 2;
        }
        if (soundStage == 2 && p >= 0.45f) {
            playStageSound(3);
            soundStage = 3;
        }
        if (soundStage == 3 && p >= 0.8f) {
            playStageSound(4);
            soundStage = 4;
        }
    }

    private void playStageSound(int stage) {
        if (level == null) {
            return;
        }
        CrateAnimation.Theme theme = animation.theme();
        SoundEvent sound;
        float pitch = 1.0f;
        switch (stage) {
            case 0 -> { // key click / unlock
                sound = SoundEvents.CHEST_LOCKED;
                pitch = 1.2f;
            }
            case 2 -> { // lid opens
                sound = SoundEvents.CHEST_OPEN;
            }
            case 3 -> { // reveal chime
                sound = theme == CrateAnimation.Theme.CASINO
                        ? SoundEvents.NOTE_BLOCK_BELL.value() : SoundEvents.BEACON_ACTIVATE;
                pitch = 1.0f;
            }
            case 4 -> { // finale
                sound = theme == CrateAnimation.Theme.INFERNAL
                        ? SoundEvents.GENERIC_EXPLODE : SoundEvents.FIREWORK_ROCKET_BLAST;
            }
            default -> {
                return;
            }
        }
        level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                sound, SoundSource.BLOCKS, 0.8f, pitch, false);
    }

    // ------------------------------------------------------------------
    // Easing helpers
    // ------------------------------------------------------------------

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float x = t - 1f;
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

    /** Helper to decode candidate item list from packet NBT. */
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
