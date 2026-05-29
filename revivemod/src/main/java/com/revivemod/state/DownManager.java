package com.revivemod.state;

import com.revivemod.ReviveMod;
import com.revivemod.config.ReviveConfig;
import net.minecraft.block.BedBlock;
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
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
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
    /** Pending E (surrender) / F (self-revive) toggles queued from the netty
     *  thread by ServerPlayNetworkHandlerMixin; consumed on the main thread. */
    private static final Set<UUID> PENDING_SURRENDER = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PENDING_SELF = ConcurrentHashMap.newKeySet();

    private DownManager() {}

    public static void requestSurrenderToggle(UUID u) { if (isDown(u)) PENDING_SURRENDER.add(u); }
    public static void requestSelfToggle(UUID u) { if (isDown(u)) PENDING_SELF.add(u); }
    public static boolean consumeSurrenderToggle(UUID u) { return PENDING_SURRENDER.remove(u); }
    public static boolean consumeSelfToggle(UUID u) { return PENDING_SELF.remove(u); }

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

        // Lay the player on their back (SLEEPING pose) — 100% server-side, both
        // the player and observers see it, no jitter. setSleepingPosition alone
        // is enough; the mixin suppresses the wake-up / skip-night side effects.
        player.setSleepingPosition(safeSleepPos(player));

        enforceLockedSlot(player);

        state.bossBar.addPlayer(player);
        state.bossBar.setName(Text.literal("Desangrandose").formatted(Formatting.RED, Formatting.BOLD));
        state.bossBar.setPercent(1.0f);

        ServerWorld world = player.getServerWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.7f, 0.7f);

        // Broadcast: "<player> se esta desangrando".
        Text msg = Text.literal(player.getGameProfile().getName()).formatted(Formatting.YELLOW)
                .append(Text.literal(" se esta desangrando.").formatted(Formatting.RED));
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
            p.sendMessage(msg, false);
        }

        // Clickable options for the downed player — ONLY for vanilla clients
        // (no mod installed client-side). Modded clients get the on-screen HUD
        // instead and must NOT be spammed with chat buttons.
        if (!net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(
                player, com.revivemod.network.Payloads.SURRENDER_ID)) {
            sendBleedOptions(player, cfg);
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

    /** A sleeping position that won't collide with real-bed sleep semantics. */
    private static BlockPos safeSleepPos(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        if (player.getServerWorld().getBlockState(pos).getBlock() instanceof BedBlock) {
            pos = pos.down();
        }
        return pos;
    }

    /** Clickable [Rendirse] / [Auto-revivir] shown in chat (works in the sleep screen). */
    private static void sendBleedOptions(ServerPlayerEntity player, ReviveConfig cfg) {
        Text surrender = Text.literal("[Rendirse]")
                .formatted(Formatting.RED, Formatting.BOLD)
                .styled(s -> s
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/revive surrender"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Mueres ahora"))));
        Text msg = Text.literal("Estas desangrandose. ").formatted(Formatting.GRAY).append(surrender);
        if (cfg.allowSelfRevive) {
            Text self = Text.literal("  [Auto-revivir (" + cfg.selfReviveLevelCost + " niveles)]")
                    .formatted(Formatting.GREEN, Formatting.BOLD)
                    .styled(s -> s
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/revive self"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Te revives pagando " + cfg.selfReviveLevelCost + " niveles"))));
            msg = msg.copy().append(self);
        }
        player.sendMessage(msg, false);
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
     *  mixin no longer cancels the wake-up). */
    public static void clearProne(ServerPlayerEntity player) {
        player.clearSleepingPosition();
        player.wakeUp(true, true);
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
        player.setSleepingPosition(safeSleepPos(player));
        enforceLockedSlot(player);
    }

    public static void enforcePosition(ServerPlayerEntity player) {
        DownState state = DOWNED.get(player.getUuid());
        if (state == null) return;
        state.downDimension = player.getServerWorld().getRegistryKey();
        state.downPosition = player.getPos();
    }
}
