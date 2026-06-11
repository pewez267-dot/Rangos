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
import net.minecraft.core.particles.ParticleTypes;
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
 * Stores a placed crate's {@link CrateConfig} (server) and drives the in-world
 * opening animation (client): long, suspenseful phases; an item roulette as the
 * reveal; themed accent particles and a layered, epic per-rarity sound build.
 */
public class CrateBlockEntity extends BlockEntity {

    private CrateConfig config = new CrateConfig();

    public static final float P_ANTICIPATION_END = 0.10f;
    public static final float P_OPEN_END         = 0.22f;
    public static final float P_REVEAL_END        = 0.88f;

    // ----- client animation state -----
    public boolean animating = false;
    public int animTick = 0;
    public int animTotal = 150;
    private CrateAnimation animation = AnimationRegistry.get(AnimationRegistry.defaultId());
    private int animColor = 0xFFFFFF;
    private ItemStack rewardIcon = ItemStack.EMPTY;
    private final List<ItemStack> candidates = new ArrayList<>();
    private int winnerIndex = 0;
    private int soundStage = 0;
    private int winTick = -1;
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
    // Animation control
    // ------------------------------------------------------------------

    public void startAnimation(String animationId, int rarityColor, ItemStack reward, List<ItemStack> cands) {
        this.animation = AnimationRegistry.get(animationId);
        this.animTotal = Math.max(6, animation.durationTicks());
        this.animColor = rarityColor;
        this.rewardIcon = reward == null ? ItemStack.EMPTY : reward;
        this.candidates.clear();
        if (cands != null) {
            for (ItemStack s : cands) {
                if (s != null && !s.isEmpty()) this.candidates.add(s);
            }
        }
        if (!this.rewardIcon.isEmpty() && !containsItem(this.candidates, this.rewardIcon)) {
            this.candidates.add(this.rewardIcon);
        }
        if (this.candidates.isEmpty() && !this.rewardIcon.isEmpty()) {
            this.candidates.add(this.rewardIcon);
        }
        this.winnerIndex = 0;
        for (int i = 0; i < this.candidates.size(); i++) {
            if (ItemStack.isSameItemSameTags(this.candidates.get(i), this.rewardIcon)) {
                this.winnerIndex = i;
                break;
            }
        }
        this.animTick = 0;
        this.soundStage = 0;
        this.winTick = -1;
        this.animating = true;
        if (level != null) {
            play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.40f, 0.7f);
            play(SoundEvents.BEACON_AMBIENT, 0.30f, 0.9f);
        }
    }

    private static boolean containsItem(List<ItemStack> list, ItemStack s) {
        for (ItemStack i : list) {
            if (ItemStack.isSameItemSameTags(i, s)) return true;
        }
        return false;
    }

    public float progress() {
        return animating ? Math.min(1f, animTick / (float) Math.max(1, animTotal)) : 0f;
    }

    public ParticleLayer.Phase currentPhase() {
        if (!animating) return ParticleLayer.Phase.IDLE;
        float p = progress();
        if (p < P_ANTICIPATION_END) return ParticleLayer.Phase.ANTICIPATION;
        if (p < P_OPEN_END) return ParticleLayer.Phase.OPEN;
        if (p < P_REVEAL_END) return ParticleLayer.Phase.REVEAL;
        return ParticleLayer.Phase.FINALE;
    }

    public float lidOpen(float partial) {
        if (!animating) return 0f;
        float p = (animTick + partial) / Math.max(1, animTotal);
        if (p < P_ANTICIPATION_END) return 0f;
        if (p < P_OPEN_END) return easeOutBack((p - P_ANTICIPATION_END) / (P_OPEN_END - P_ANTICIPATION_END));
        if (p < 0.94f) return 1f;
        return 1f - easeInOut(Math.min(1f, (p - 0.94f) / 0.06f));
    }

    public float shake(float partial) {
        if (!animating) return 0f;
        float p = (animTick + partial) / Math.max(1, animTotal);
        if (p >= P_ANTICIPATION_END) return 0f;
        float intensity = (P_ANTICIPATION_END - p) / P_ANTICIPATION_END;
        return (float) Math.sin((animTick + partial) * 2.4f) * 0.06f * intensity;
    }

    public float revealProgress(float partial) {
        float p = (animTick + partial) / Math.max(1, animTotal);
        if (p <= P_OPEN_END) return 0f;
        if (p >= P_REVEAL_END) return 1f;
        return (p - P_OPEN_END) / (P_REVEAL_END - P_OPEN_END);
    }

    public float finaleProgress(float partial) {
        float p = (animTick + partial) / Math.max(1, animTotal);
        if (p <= P_REVEAL_END) return 0f;
        return Math.min(1f, (p - P_REVEAL_END) / (1f - P_REVEAL_END));
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void clientTick(Level level, BlockPos pos, BlockState state, CrateBlockEntity be) {
        be.ambientTime += 1f;
        if (be.animating) {
            be.animTick++;
            be.emitLayers(level, pos, be.currentPhase());
            be.emitAccent(level, pos);
            be.advanceSounds();
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
    // User-defined particle layers
    // ------------------------------------------------------------------

    private void emitLayers(Level level, BlockPos pos, ParticleLayer.Phase phase) {
        for (ParticleLayer layer : config.particleLayers) {
            if (layer.phase != phase) continue;
            if (phase == ParticleLayer.Phase.IDLE && level.getGameTime() % Math.max(1, layer.interval) != 0) {
                continue;
            }
            ParticleOptions opt = resolve(layer);
            if (opt != null) emitShape(level, pos, layer, opt);
        }
    }

    private ParticleOptions resolve(ParticleLayer layer) {
        String id = layer.particleId == null ? "" : layer.particleId.trim();
        if (id.equals("minecraft:dust") || id.equals("dust")) {
            int color = layer.useRarityColor ? animColor : parseHex(layer.colorHex, animColor);
            return dust(color, 1.4f);
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(rl);
        return (type instanceof SimpleParticleType simple) ? simple : null;
    }

    private DustParticleOptions dust(int color, float scale) {
        return new DustParticleOptions(new Vector3f(((color >> 16) & 255) / 255f,
                ((color >> 8) & 255) / 255f, (color & 255) / 255f), scale);
    }

    private void emitShape(Level level, BlockPos pos, ParticleLayer layer, ParticleOptions opt) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + Math.max(0.0, layer.yOffset);
        double cz = pos.getZ() + 0.5;
        var rng = level.random;
        int n = Math.max(1, layer.count);
        double r = layer.radius, sp = layer.speed, spread = layer.spread, t = ambientTime * 0.1;

        for (int i = 0; i < n; i++) {
            double angle;
            switch (layer.shape) {
                case HALO -> {
                    angle = t + i * (Math.PI * 2 / n);
                    level.addParticle(opt, cx + Math.cos(angle) * r, cy + 0.05 * Math.sin(t * 1.7 + i), cz + Math.sin(angle) * r,
                            -Math.sin(angle) * sp, sp * 0.4, Math.cos(angle) * sp);
                }
                case RING -> {
                    angle = i * (Math.PI * 2 / n);
                    level.addParticle(opt, cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r,
                            Math.cos(angle) * sp, 0, Math.sin(angle) * sp);
                }
                case BURST -> {
                    double ax = (rng.nextDouble() - 0.5) * 2, az = (rng.nextDouble() - 0.5) * 2, ay = 0.4 + rng.nextDouble() * 0.6;
                    double mag = Math.max(0.001, Math.sqrt(ax * ax + ay * ay + az * az));
                    level.addParticle(opt, cx, cy, cz, ax / mag * (sp + spread), ay / mag * (sp + spread), az / mag * (sp + spread));
                }
                case COLUMN -> level.addParticle(opt, cx + (rng.nextDouble() - 0.5) * spread,
                        cy + rng.nextDouble() * (0.4 + r), cz + (rng.nextDouble() - 0.5) * spread, 0, sp, 0);
                case SPIRAL -> {
                    angle = t * 3 + i * 0.7;
                    double rr = r * (0.3 + (i / (double) n) * 0.7);
                    level.addParticle(opt, cx + Math.cos(angle) * rr, cy + (i / (double) n) * 1.0, cz + Math.sin(angle) * rr, 0, sp, 0);
                }
                case FOUNTAIN -> {
                    angle = rng.nextDouble() * Math.PI * 2;
                    level.addParticle(opt, cx, cy, cz, Math.cos(angle) * spread, sp + rng.nextDouble() * 0.15, Math.sin(angle) * spread);
                }
                case VORTEX -> {
                    angle = t * 4 + i * (Math.PI * 2 / n);
                    double rr2 = r * (0.6 + 0.4 * Math.sin(t * 2 + i));
                    level.addParticle(opt, cx + Math.cos(angle) * rr2, cy + rng.nextDouble() * 0.5, cz + Math.sin(angle) * rr2,
                            -Math.cos(angle) * sp * 2, sp, -Math.sin(angle) * sp * 2);
                }
                case RAIN -> level.addParticle(opt, cx + (rng.nextDouble() - 0.5) * (spread + r * 2),
                        cy + rng.nextDouble() * 0.5, cz + (rng.nextDouble() - 0.5) * (spread + r * 2), 0, -sp, 0);
                case POINT -> level.addParticle(opt, cx, cy, cz,
                        (rng.nextDouble() - 0.5) * sp, rng.nextDouble() * sp, (rng.nextDouble() - 0.5) * sp);
            }
        }
    }

    // ------------------------------------------------------------------
    // Built-in themed accent particles (so every animation feels themed even
    // without custom layers; also matches the light-beam colour)
    // ------------------------------------------------------------------

    private void emitAccent(Level level, BlockPos pos) {
        float p = progress();
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
        double cyTop = pos.getY() + 1.5; // around the floating reel
        var rng = level.random;
        CrateAnimation.Theme theme = animation.theme();

        // reveal: gentle swirl rising around the reel
        if (p >= P_OPEN_END && p < P_REVEAL_END && animTick % 2 == 0) {
            double a = ambientTime * 0.3;
            double rad = 0.55;
            ParticleOptions swirl = switch (theme) {
                case INFERNAL -> ParticleTypes.FLAME;
                case CELESTIAL -> ParticleTypes.END_ROD;
                case MAGIC -> ParticleTypes.WITCH;
                case ANCIENT -> ParticleTypes.ENCHANT;
                case NATURE -> ParticleTypes.HAPPY_VILLAGER;
                case NEON, CASINO -> dust(animColor, 1.2f);
                default -> ParticleTypes.CRIT;
            };
            level.addParticle(swirl, cx + Math.cos(a) * rad, cyTop + 0.1 * Math.sin(a * 2), cz + Math.sin(a) * rad, 0, 0.04, 0);
            level.addParticle(swirl, cx + Math.cos(a + Math.PI) * rad, cyTop, cz + Math.sin(a + Math.PI) * rad, 0, 0.04, 0);
        }

        // finale: celebratory burst
        if (p >= P_REVEAL_END && animTick % 2 == 0) {
            for (int i = 0; i < 4; i++) {
                double a = rng.nextDouble() * Math.PI * 2, s = 0.2 + rng.nextDouble() * 0.4;
                ParticleOptions fin = switch (theme) {
                    case INFERNAL -> ParticleTypes.LAVA;
                    case CELESTIAL -> ParticleTypes.END_ROD;
                    case NATURE -> ParticleTypes.HAPPY_VILLAGER;
                    case MAGIC, ANCIENT -> ParticleTypes.TOTEM_OF_UNDYING;
                    default -> ParticleTypes.FIREWORK;
                };
                level.addParticle(fin, cx, cyTop, cz, Math.cos(a) * s, 0.15 + rng.nextDouble() * 0.25, Math.sin(a) * s);
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
    // Sounds — soft build, big epic payoff (two-beat finale)
    // ------------------------------------------------------------------

    private void advanceSounds() {
        float p = progress();

        if (soundStage == 0 && p >= P_ANTICIPATION_END) {
            play(SoundEvents.ELYTRA_FLYING, 0.30f, 1.6f);
            play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.55f, 1.2f);
            play(SoundEvents.CHEST_OPEN, 0.45f, 1.05f);
            if (config.rarity.ordinal() >= Rarity.EPIC.ordinal()) {
                play(SoundEvents.BEACON_ACTIVATE, 0.45f, 1.3f);
            }
            soundStage = 1;
        }

        if (soundStage >= 1 && p >= P_OPEN_END && p < P_REVEAL_END) {
            float rp = revealProgress(0f);
            int interval = 2 + (int) (rp * 11);
            if (animTick % Math.max(2, interval) == 0) {
                float tickPitch = 0.85f + rp * 1.1f;
                play(SoundEvents.NOTE_BLOCK_HAT.value(), 0.35f, tickPitch);
                if (config.rarity.ordinal() >= Rarity.LEGENDARY.ordinal() && rp > 0.7f) {
                    play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.18f, tickPitch);
                }
            }
            if (animTick % 16 == 0) {
                play(SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 0.16f, 0.5f + rp * 0.3f);
            }
        }

        // FINALE beat 1 (impact) at reveal end
        if (soundStage < 60 && p >= P_REVEAL_END) {
            playWinBeat1();
            soundStage = 60;
            winTick = animTick;
        }
        // FINALE beat 2 (swell), a few ticks later
        if (soundStage == 60 && winTick >= 0 && animTick >= winTick + 5) {
            playWinBeat2();
            soundStage = 61;
        }
    }

    private void playWinBeat1() {
        play(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.5f, 0.7f);
        switch (config.rarity) {
            case COMMON -> play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, 1.0f);
            case RARE -> play(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.65f, 1.0f);
            case EPIC -> {
                play(SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.0f);
                play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.55f, 1.26f);
            }
            case LEGENDARY -> {
                play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.0f);
                play(SoundEvents.RAID_HORN.value(), 0.35f, 1.3f);
            }
            case MYTHIC -> {
                play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 0.8f);
                play(SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                play(SoundEvents.WITHER_SPAWN, 0.30f, 1.6f);
            }
        }
    }

    private void playWinBeat2() {
        switch (config.rarity) {
            case COMMON -> play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.5f, 1.5f);
            case RARE -> {
                play(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.55f, 1.26f);
                play(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.5f, 1.5f);
            }
            case EPIC -> {
                play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.4f);
                play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.5f, 2.0f);
            }
            case LEGENDARY -> {
                play(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.6f, 1.0f);
                play(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.6f, 1.5f);
                play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.55f, 2.0f);
                play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.45f, 1.2f);
            }
            case MYTHIC -> {
                // huge ascending fanfare
                play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
                play(SoundEvents.TOTEM_USE, 0.7f, 1.0f);
                play(SoundEvents.RAID_HORN.value(), 0.45f, 1.5f);
                play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, 1.0f);
                play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, 1.26f);
                play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, 1.5f);
                play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, 2.0f);
                play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.6f, 1.0f);
                play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.5f, 1.4f);
            }
        }
    }

    private void play(SoundEvent sound, float vol, float pitch) {
        if (level == null || sound == null) return;
        level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                sound, SoundSource.BLOCKS, vol, pitch, false);
    }

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
        if (wrap == null) return out;
        ListTag list = wrap.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            out.add(ItemStack.of(list.getCompound(i)));
        }
        return out;
    }
}
