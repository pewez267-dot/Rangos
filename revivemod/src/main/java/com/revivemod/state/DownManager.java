package com.revivemod.state;

import com.revivemod.ReviveMod;
import com.revivemod.config.ReviveConfig;
import com.revivemod.gui.OptionsScreenHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
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
 * Central tracker of downed players. Keyed by player UUID so the state survives
 * dimension changes, /tpa, /tp, logout/login (we re-knock them down on join).
 */
public final class DownManager {

    private static final Map<UUID, DownState> DOWNED = new HashMap<>();

    /** UUIDs of players who are actively channeling a revive THIS tick. Rebuilt
     *  every server tick by DownTicker. Used by DamageHandler to grant the
     *  reviver invincibility while they revive a teammate. */
    private static final Set<UUID> ACTIVE_REVIVERS = ConcurrentHashMap.newKeySet();

    /** Player UUIDs currently being force-killed. ALLOW_DEATH ignores these so
     *  forceDeath can never re-trigger the knock-down loop. */
    private static final Set<UUID> FORCE_KILLING = ConcurrentHashMap.newKeySet();

    private DownManager() {}

    public static boolean isDown(ServerPlayerEntity player) {
        return DOWNED.containsKey(player.getUuid());
    }

    public static boolean isDown(UUID uuid) {
        return DOWNED.containsKey(uuid);
    }

    public static DownState get(ServerPlayerEntity player) {
        return DOWNED.get(player.getUuid());
    }

    public static DownState get(UUID uuid) {
        return DOWNED.get(uuid);
    }

    public static Collection<DownState> all() {
        return Collections.unmodifiableCollection(DOWNED.values());
    }

    // ----- reviver invincibility -----

    public static void clearActiveRevivers() { ACTIVE_REVIVERS.clear(); }
    public static void markReviving(UUID uuid) { ACTIVE_REVIVERS.add(uuid); }
    public static boolean isReviving(UUID uuid) { return ACTIVE_REVIVERS.contains(uuid); }
    public static boolean isForceKilling(UUID uuid) { return FORCE_KILLING.contains(uuid); }

    /**
     * Knock a player down: cancel the death, restore HP, lay them down with the
     * SLEEPING pose (no jitter — see PlayerEntityMixin javadoc), apply effects,
     * start the bossbar, open the on-screen options menu.
     */
    public static void knockDown(ServerPlayerEntity player, DamageSource cause) {
        ReviveConfig cfg = ReviveMod.getConfig();
        int totalTicks = Math.max(20, cfg.downTimeSeconds * 20);

        DownState state = new DownState(
                player.getUuid(),
                totalTicks,
                player.getServerWorld().getRegistryKey(),
                player.getPos()
        );
        state.snapshotFood = player.getHungerManager().getFoodLevel();
        state.snapshotSaturation = player.getHungerManager().getSaturationLevel();
        state.lockedSlot = player.getInventory().selectedSlot;
        for (StatusEffectInstance eff : player.getStatusEffects()) {
            state.snapshotEffects.add(new StatusEffectInstance(eff));
        }
        DOWNED.put(player.getUuid(), state);

        // Restore HP so they don't die from the same damage tick.
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
        player.setSwimming(false);

        applyDownEffects(player);

        // Lay them down. We set SLEEPING_POSITION directly (no trySleep flow,
        // no skip-night, no spawn-point change) — vanilla updatePose will pick
        // the SLEEPING pose because isSleeping() is now true on both sides.
        player.setSleepingPosition(safeSleepPos(player));

        enforceLockedSlot(player);

        // Bossbar.
        state.bossBar.addPlayer(player);
        state.bossBar.setName(Text.literal("Noqueado").formatted(Formatting.RED, Formatting.BOLD));
        state.bossBar.setPercent(1.0f);

        // On-screen options menu (surrender / self-revive). Opened straight away
        // so the player sees the buttons in screen, not in chat.
        openOptions(player);

        // Soft amethyst chime, low pitch.
        ServerWorld world = player.getServerWorld();
        world.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS,
                0.7f, 0.7f);

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

    /** Open the on-screen options menu (surrender / self-revive) for a downed player. */
    public static void openOptions(ServerPlayerEntity player) {
        if (!isDown(player)) return;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new OptionsScreenHandler(syncId, inv),
                Text.literal("Estas noqueado").formatted(Formatting.DARK_RED, Formatting.BOLD)));
    }

    public static void applyDownEffects(ServerPlayerEntity player) {
        ReviveConfig cfg = ReviveMod.getConfig();
        // Slowness as a backup if the SLEEPING immobilisation ever desyncs;
        // otherwise the sleeping flag itself bypasses input movement.
        int slow = Math.max(0, Math.min(15, cfg.crawlSlowness));
        player.addStatusEffect(infinite(StatusEffects.SLOWNESS, slow));
        player.addStatusEffect(infinite(StatusEffects.MINING_FATIGUE, 4));
        player.addStatusEffect(infinite(StatusEffects.WEAKNESS, 4));
        player.addStatusEffect(infinite(StatusEffects.BLINDNESS, 0));
        if (cfg.glowingWhileDown) {
            player.addStatusEffect(infinite(StatusEffects.GLOWING, 0));
        }
    }

    /**
     * Re-apply the locked hotbar slot. Called every tick by DownTicker so the
     * player can never switch items while downed. The actual "lying down"
     * pose is handled passively by SLEEPING_POSITION + the mixin, no per-tick
     * forcing is needed.
     */
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

    /**
     * Cleanly revive a player: removes the state, removes the down effects,
     * restores HP / food, plays a richer particle + sound effect. NO green
     * title and NO chat broadcast (deliberately silent on UI).
     */
    public static void revive(ServerPlayerEntity player) {
        DownState state = DOWNED.remove(player.getUuid());
        if (state == null) return;

        ReviveConfig cfg = ReviveMod.getConfig();

        state.bossBar.clearPlayers();
        ACTIVE_REVIVERS.remove(player.getUuid());

        clearDownEffects(player);
        clearProne(player);

        // Make sure the options menu is closed.
        if (player.currentScreenHandler != player.playerScreenHandler) {
            player.closeHandledScreen();
        }

        // Restore the snapshot of pre-existing effects (skip the ones we owned).
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

        // Prettier revive FX: layered sounds + multiple particle bursts.
        ServerWorld world = player.getServerWorld();
        playReviveEffects(world, player);
    }

    /** Layered, soft-but-noticeable revive feedback (no on-screen text). */
    private static void playReviveEffects(ServerWorld world, ServerPlayerEntity player) {
        double x = player.getX(), y = player.getY(), z = player.getZ();

        // Three layered sounds: bright chime + warm bell undertone + soft levelup.
        world.playSound(null, x, y, z,
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 1.6f);
        world.playSound(null, x, y, z,
                SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.5f, 1.3f);
        world.playSound(null, x, y, z,
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.35f, 1.6f);

        // Heart cloud above the body.
        world.spawnParticles(ParticleTypes.HEART,
                x, y + 1.2, z,
                18, 0.5, 0.7, 0.5, 0.02);
        // Happy villager spiral around.
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                x, y + 1.0, z,
                30, 0.7, 1.0, 0.7, 0.05);
        // Enchantment glyphs (subtle ascending spiral).
        world.spawnParticles(ParticleTypes.ENCHANT,
                x, y + 1.6, z,
                25, 0.5, 0.8, 0.5, 0.6);
        // End-rod sparks shooting up briefly (vertical beam feel).
        world.spawnParticles(ParticleTypes.END_ROD,
                x, y + 0.2, z,
                12, 0.15, 0.0, 0.15, 0.18);
        // Glow droplets to add a warm aura.
        world.spawnParticles(ParticleTypes.GLOW,
                x, y + 1.0, z,
                10, 0.6, 0.6, 0.6, 0.0);
    }

    public static void clearDownEffects(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
        player.removeStatusEffect(StatusEffects.WEAKNESS);
        player.removeStatusEffect(StatusEffects.BLINDNESS);
        player.removeStatusEffect(StatusEffects.GLOWING);
    }

    /** Stand the player up (clear the SLEEPING_POSITION + sleep flow). */
    public static void clearProne(ServerPlayerEntity player) {
        player.clearSleepingPosition();
        // wakeUp normally is suppressed by our mixin while down, but we just
        // removed the player from DOWNED so the mixin no longer cancels it.
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

        if (player.currentScreenHandler != player.playerScreenHandler) {
            player.closeHandledScreen();
        }

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
        // Re-establish the lying-down pose in case the data tracker was reset.
        player.setSleepingPosition(safeSleepPos(player));
        enforceLockedSlot(player);
        // Reopen the options menu so a relogged player still sees the buttons.
        openOptions(player);
    }

    /** A sleeping position safe to use without colliding with bed-block sleep
     *  semantics (would otherwise inflate the chat sleeping-status counter). */
    private static net.minecraft.util.math.BlockPos safeSleepPos(ServerPlayerEntity player) {
        net.minecraft.util.math.BlockPos pos = player.getBlockPos();
        if (player.getServerWorld().getBlockState(pos).getBlock() instanceof net.minecraft.block.BedBlock) {
            pos = pos.down();
        }
        return pos;
    }

    public static void enforcePosition(ServerPlayerEntity player) {
        DownState state = DOWNED.get(player.getUuid());
        if (state == null) return;
        state.downDimension = player.getServerWorld().getRegistryKey();
        state.downPosition = player.getPos();
    }
}
