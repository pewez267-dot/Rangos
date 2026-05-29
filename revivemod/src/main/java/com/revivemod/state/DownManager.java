package com.revivemod.state;

import com.revivemod.ReviveMod;
import com.revivemod.config.ReviveConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.damage.DamageSource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central tracker of downed players (UUID-keyed, survives teleports / relogin).
 *
 * Down state visualisation: SWIMMING pose (the player crawls). The server only
 * sets the pose ONCE on knockdown; PlayerEntityMixin then cancels per-tick
 * updateSwimming / updatePose so the server stops broadcasting deltas. The
 * vanilla client local will still recompute STANDING in its own tick, but
 * since the server stops re-broadcasting, no ping-pong -> no jitter.
 * Otros jugadores ven el arrastre normal.
 */
public final class DownManager {

    private static final Map<UUID, DownState> DOWNED = new HashMap<>();
    private static final Set<UUID> ACTIVE_REVIVERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> FORCE_KILLING = ConcurrentHashMap.newKeySet();

    private DownManager() {}

    public static boolean isDown(ServerPlayerEntity player) { return DOWNED.containsKey(player.getUuid()); }
    public static boolean isDown(UUID uuid) { return DOWNED.containsKey(uuid); }
    public static DownState get(ServerPlayerEntity player) { return DOWNED.get(player.getUuid()); }
    public static DownState get(UUID uuid) { return DOWNED.get(uuid); }
    public static Collection<DownState> all() { return Collections.unmodifiableCollection(DOWNED.values()); }

    public static void clearActiveRevivers() { ACTIVE_REVIVERS.clear(); }
    public static void markReviving(UUID uuid) { ACTIVE_REVIVERS.add(uuid); }
    public static boolean isReviving(UUID uuid) { return ACTIVE_REVIVERS.contains(uuid); }
    public static boolean isForceKilling(UUID uuid) { return FORCE_KILLING.contains(uuid); }

    public static void knockDown(ServerPlayerEntity player, DamageSource cause) {
        ReviveConfig cfg = ReviveMod.getConfig();
        int totalTicks = Math.max(20, cfg.downTimeSeconds * 20);

        DownState state = new DownState(player.getUuid(), totalTicks,
                player.getServerWorld().getRegistryKey(), player.getPos());
        state.snapshotFood = player.getHungerManager().getFoodLevel();
        state.snapshotSaturation = player.getHungerManager().getSaturationLevel();
        state.lockedSlot = player.getInventory().selectedSlot;
        for (StatusEffectInstance eff : player.getStatusEffects()) {
            state.snapshotEffects.add(new StatusEffectInstance(eff));
        }
        DOWNED.put(player.getUuid(), state);

        float maxHealth = player.getMaxHealth();
        if (player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH) != null) {
            maxHealth = (float) player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).getValue();
        }
        player.setHealth(Math.max(1.0f, maxHealth));
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0f);

        player.extinguish();
        player.fallDistance = 0f;
        player.setOnFire(false);
        player.timeUntilRegen = 40;
        player.setSprinting(false);

        applyDownEffects(player);

        // Set crawl pose ONCE. Mixin holds it constant from now on (no per-tick deltas).
        player.setSwimming(true);
        player.setPose(EntityPose.SWIMMING);

        enforceLockedSlot(player);

        state.bossBar.addPlayer(player);
        state.bossBar.setName(Text.literal("Noqueado").formatted(Formatting.RED, Formatting.BOLD));
        state.bossBar.setPercent(1.0f);

        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
                Text.literal("Estas noqueado").formatted(Formatting.DARK_RED, Formatting.BOLD)));
        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 60, 20));

        ServerWorld world = player.getServerWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.7f, 0.7f);

        // Broadcast: "<player> ha sido noqueado por <cause>".
        Text msg = Text.literal(player.getGameProfile().getName()).formatted(Formatting.YELLOW)
                .append(Text.literal(" ha sido noqueado por ").formatted(Formatting.GRAY))
                .append(causeName(cause))
                .append(Text.literal(".").formatted(Formatting.GRAY));
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
            p.sendMessage(msg, false);
        }

        ReviveMod.LOGGER.info("[revivemod] {} knocked down (cause={})",
                player.getGameProfile().getName(),
                cause == null ? "unknown" : cause.getName());

        if (cfg.clearMobAggroOnDown) {
            world.getEntitiesByClass(
                    net.minecraft.entity.mob.MobEntity.class,
                    player.getBoundingBox().expand(48.0),
                    mob -> mob.getTarget() == player
            ).forEach(mob -> mob.setTarget(null));
        }
    }

    private static Text causeName(DamageSource source) {
        if (source != null) {
            Entity attacker = source.getAttacker();
            if (attacker != null) return attacker.getDisplayName().copy().formatted(Formatting.RED);
            Entity direct = source.getSource();
            if (direct != null) return direct.getDisplayName().copy().formatted(Formatting.RED);
        }
        return Text.literal("el entorno").formatted(Formatting.RED);
    }

    public static void applyDownEffects(ServerPlayerEntity player) {
        ReviveConfig cfg = ReviveMod.getConfig();
        int slow = Math.max(0, Math.min(15, cfg.crawlSlowness));
        player.addStatusEffect(infinite(StatusEffects.SLOWNESS, slow));
        player.addStatusEffect(infinite(StatusEffects.MINING_FATIGUE, 4));
        player.addStatusEffect(infinite(StatusEffects.WEAKNESS, 4));
        player.addStatusEffect(infinite(StatusEffects.BLINDNESS, 0));
        if (cfg.glowingWhileDown) {
            player.addStatusEffect(infinite(StatusEffects.GLOWING, 0));
        }
    }

    public static void enforceLockedSlot(ServerPlayerEntity player) {
        DownState st = DOWNED.get(player.getUuid());
        if (st != null && player.getInventory().selectedSlot != st.lockedSlot) {
            player.getInventory().selectedSlot = st.lockedSlot;
            player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(st.lockedSlot));
        }
    }

    private static StatusEffectInstance infinite(RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int amplifier) {
        return new StatusEffectInstance(effect, -1, amplifier, false, false, true);
    }

    /** Layered, soft-but-noticeable revive feedback (no on-screen text). */
    public static void revive(ServerPlayerEntity player) {
        DownState state = DOWNED.remove(player.getUuid());
        if (state == null) return;

        ReviveConfig cfg = ReviveMod.getConfig();
        state.bossBar.clearPlayers();
        ACTIVE_REVIVERS.remove(player.getUuid());

        clearDownEffects(player);
        clearProne(player);

        for (StatusEffectInstance eff : state.snapshotEffects) {
            if (eff.getEffectType() == StatusEffects.SLOWNESS) continue;
            if (eff.getEffectType() == StatusEffects.MINING_FATIGUE) continue;
            if (eff.getEffectType() == StatusEffects.WEAKNESS) continue;
            if (eff.getEffectType() == StatusEffects.BLINDNESS) continue;
            if (eff.getEffectType() == StatusEffects.GLOWING) continue;
            player.addStatusEffect(new StatusEffectInstance(eff));
        }

        player.setHealth(Math.max(1.0f, Math.min(player.getMaxHealth(), cfg.reviveHealth)));
        player.getHungerManager().setFoodLevel(Math.max(player.getHungerManager().getFoodLevel(), cfg.reviveFood));
        player.getHungerManager().setSaturationLevel(2.0f);
        player.timeUntilRegen = 60;
        player.fallDistance = 0f;

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1, false, true, true));

        ServerWorld world = player.getServerWorld();
        double x = player.getX(), y = player.getY(), z = player.getZ();
        world.playSound(null, x, y, z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 1.6f);
        world.playSound(null, x, y, z, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.5f, 1.3f);
        world.playSound(null, x, y, z, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.35f, 1.6f);
        world.spawnParticles(ParticleTypes.HEART, x, y + 1.2, z, 18, 0.5, 0.7, 0.5, 0.02);
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.0, z, 30, 0.7, 1.0, 0.7, 0.05);
        world.spawnParticles(ParticleTypes.ENCHANT, x, y + 1.6, z, 25, 0.5, 0.8, 0.5, 0.6);
        world.spawnParticles(ParticleTypes.END_ROD, x, y + 0.2, z, 12, 0.15, 0.0, 0.15, 0.18);
        world.spawnParticles(ParticleTypes.GLOW, x, y + 1.0, z, 10, 0.6, 0.6, 0.6, 0.0);
    }

    public static void clearDownEffects(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
        player.removeStatusEffect(StatusEffects.WEAKNESS);
        player.removeStatusEffect(StatusEffects.BLINDNESS);
        player.removeStatusEffect(StatusEffects.GLOWING);
    }

    /** Stand the player up (after the DOWNED entry has been removed so the
     *  mixin no longer cancels updatePose / updateSwimming). */
    public static void clearProne(ServerPlayerEntity player) {
        player.setSwimming(false);
        player.setPose(EntityPose.STANDING);
    }

    public static boolean selfRevive(ServerPlayerEntity player) {
        ReviveConfig cfg = ReviveMod.getConfig();
        if (!cfg.allowSelfRevive) return false;
        if (!isDown(player)) return false;
        if (player.experienceLevel < cfg.selfReviveLevelCost) return false;
        player.addExperienceLevels(-cfg.selfReviveLevelCost);
        revive(player);
        return true;
    }

    public static void forceDeath(ServerPlayerEntity player, DamageSource source) {
        DownState state = DOWNED.remove(player.getUuid());
        if (state != null) state.bossBar.clearPlayers();
        ACTIVE_REVIVERS.remove(player.getUuid());
        clearDownEffects(player);
        clearProne(player);

        DamageSource finalSrc = player.getDamageSources().genericKill();
        FORCE_KILLING.add(player.getUuid());
        try {
            player.timeUntilRegen = 0;
            player.damage(finalSrc, Float.MAX_VALUE);
            if (player.isAlive()) player.setHealth(0f);
        } finally {
            FORCE_KILLING.remove(player.getUuid());
        }
    }

    public static void clearAll(MinecraftServer server) {
        for (DownState state : DOWNED.values()) state.bossBar.clearPlayers();
        DOWNED.clear();
        ACTIVE_REVIVERS.clear();
    }

    public static DownState removeWithoutRevival(UUID uuid) {
        DownState st = DOWNED.remove(uuid);
        if (st != null) st.bossBar.clearPlayers();
        ACTIVE_REVIVERS.remove(uuid);
        return st;
    }

    public static void reattach(ServerPlayerEntity player) {
        DownState state = DOWNED.get(player.getUuid());
        if (state == null) return;
        state.bossBar.addPlayer(player);
        applyDownEffects(player);
        player.setSwimming(true);
        player.setPose(EntityPose.SWIMMING);
        enforceLockedSlot(player);
    }

    public static void enforcePosition(ServerPlayerEntity player) {
        DownState state = DOWNED.get(player.getUuid());
        if (state == null) return;
        state.downDimension = player.getServerWorld().getRegistryKey();
        state.downPosition = player.getPos();
    }
}
