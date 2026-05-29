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
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
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

    public static void clearActiveRevivers() {
        ACTIVE_REVIVERS.clear();
    }

    public static void markReviving(UUID uuid) {
        ACTIVE_REVIVERS.add(uuid);
    }

    public static boolean isReviving(UUID uuid) {
        return ACTIVE_REVIVERS.contains(uuid);
    }

    public static boolean isForceKilling(UUID uuid) {
        return FORCE_KILLING.contains(uuid);
    }

    /**
     * Knock a player down. Cancels any pending death, restores their HP,
     * applies the immobilising effects, and starts the countdown bossbar.
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

        // Restore HP so the player doesn't die from the same damage tick later.
        float maxHealth = player.getMaxHealth();
        if (player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH) != null) {
            maxHealth = (float) player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).getValue();
        }
        player.setHealth(Math.max(1.0f, maxHealth));
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0f);

        // Stop combat / fall / fire ticks that may have triggered the death.
        player.extinguish();
        player.fallDistance = 0f;
        player.setOnFire(false);
        player.timeUntilRegen = 40;
        player.setSprinting(false);

        applyDownEffects(player);
        enforceProne(player);

        // Show bossbar to the player.
        state.bossBar.addPlayer(player);
        state.bossBar.setName(Text.literal("Noqueado").formatted(Formatting.RED, Formatting.BOLD));
        state.bossBar.setPercent(1.0f);

        // Title for the downed player.
        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
                Text.literal("Estas noqueado").formatted(Formatting.DARK_RED, Formatting.BOLD)));
        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(
                Text.literal("Te pueden revivir con click derecho").formatted(Formatting.GRAY)));
        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 60, 20));

        // Clickable surrender / self-revive options in chat.
        sendOptions(player, cfg);

        // Soft amethyst chime at low pitch on knockdown.
        ServerWorld world = player.getServerWorld();
        world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS,
                0.7f, 0.7f
        );

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

        // Clear hostile mob aggro on the downed player so they aren't pummelled
        // for the entire countdown.
        if (cfg.clearMobAggroOnDown) {
            world.getEntitiesByClass(
                    net.minecraft.entity.mob.MobEntity.class,
                    player.getBoundingBox().expand(48.0),
                    mob -> mob.getTarget() == player
            ).forEach(mob -> mob.setTarget(null));
        }
    }

    /** Build the "<name> por <attacker>" cause component. */
    private static Text causeName(DamageSource source) {
        if (source != null) {
            Entity attacker = source.getAttacker();
            if (attacker != null) {
                return attacker.getDisplayName().copy().formatted(Formatting.RED);
            }
            Entity direct = source.getSource();
            if (direct != null) {
                return direct.getDisplayName().copy().formatted(Formatting.RED);
            }
        }
        return Text.literal("el entorno").formatted(Formatting.RED);
    }

    /** Send the clickable surrender / self-revive options to the downed player. */
    private static void sendOptions(ServerPlayerEntity player, ReviveConfig cfg) {
        Text surrender = Text.literal("[Rendirse]")
                .formatted(Formatting.RED, Formatting.BOLD)
                .styled(s -> s
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/revive surrender"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Te rindes y mueres ahora"))));

        Text msg = Text.literal("Opciones: ").formatted(Formatting.GRAY).append(surrender);

        if (cfg.allowSelfRevive) {
            Text self = Text.literal("  [Auto-revivir (" + cfg.selfReviveLevelCost + " niveles)]")
                    .formatted(Formatting.GREEN, Formatting.BOLD)
                    .styled(s -> s
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/revive self"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Te revives a ti mismo por " + cfg.selfReviveLevelCost + " niveles de experiencia"))));
            msg = msg.copy().append(self);
        }
        player.sendMessage(msg, false);
    }

    /**
     * Re-apply all down-state effects to a player. Used on tick & on
     * world change / login so the state can never desync.
     */
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

    /**
     * Force the downed player into a prone/crawl pose and lock their hotbar slot.
     * Called on knockdown and every tick by DownTicker so it can never desync.
     */
    public static void enforceProne(ServerPlayerEntity player) {
        if (!player.isSwimming()) {
            player.setSwimming(true);
        }
        if (player.getPose() != EntityPose.SWIMMING) {
            player.setPose(EntityPose.SWIMMING);
        }
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
     * Cleanly revive a player. Removes the state, removes the effects,
     * restores HP / food, plays effects. Intentionally produces NO on-screen
     * title and NO chat broadcast (only sound + particles).
     */
    public static void revive(ServerPlayerEntity player) {
        DownState state = DOWNED.remove(player.getUuid());
        if (state == null) return;

        ReviveConfig cfg = ReviveMod.getConfig();

        state.bossBar.clearPlayers();
        ACTIVE_REVIVERS.remove(player.getUuid());

        clearDownEffects(player);
        clearProne(player);

        // Restore the snapshot of pre-existing effects.
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
        // Soft amethyst chime at high pitch on revive (no title / no chat message).
        world.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS,
                0.8f, 1.5f);
        world.spawnParticles(ParticleTypes.HEART,
                player.getX(), player.getY() + 1.0, player.getZ(),
                12, 0.4, 0.8, 0.4, 0.02);
    }

    /** Strip every effect we applied while down. */
    public static void clearDownEffects(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
        player.removeStatusEffect(StatusEffects.WEAKNESS);
        player.removeStatusEffect(StatusEffects.BLINDNESS);
        player.removeStatusEffect(StatusEffects.GLOWING);
    }

    /** Reset the prone pose so the player stands back up. */
    public static void clearProne(ServerPlayerEntity player) {
        player.setSwimming(false);
        player.setPose(EntityPose.STANDING);
    }

    /**
     * Self-revive paying XP levels. Returns true on success, false if the player
     * doesn't have enough levels or self-revive is disabled.
     */
    public static boolean selfRevive(ServerPlayerEntity player) {
        ReviveConfig cfg = ReviveMod.getConfig();
        if (!cfg.allowSelfRevive) return false;
        if (!isDown(player)) return false;
        if (player.experienceLevel < cfg.selfReviveLevelCost) {
            return false;
        }
        player.addExperienceLevels(-cfg.selfReviveLevelCost);
        revive(player);
        return true;
    }

    /**
     * Force the player to actually die (timer expired, /revive kill, surrender, void).
     */
    public static void forceDeath(ServerPlayerEntity player, DamageSource source) {
        DownState state = DOWNED.remove(player.getUuid());
        if (state != null) {
            state.bossBar.clearPlayers();
        }
        ACTIVE_REVIVERS.remove(player.getUuid());
        clearDownEffects(player);
        clearProne(player);

        DamageSource finalSrc = player.getDamageSources().genericKill();

        FORCE_KILLING.add(player.getUuid());
        try {
            player.timeUntilRegen = 0;
            player.damage(finalSrc, Float.MAX_VALUE);
            if (player.isAlive()) {
                player.setHealth(0f);
            }
        } finally {
            FORCE_KILLING.remove(player.getUuid());
        }
    }

    public static void clearAll(MinecraftServer server) {
        for (DownState state : DOWNED.values()) {
            state.bossBar.clearPlayers();
        }
        DOWNED.clear();
        ACTIVE_REVIVERS.clear();
    }

    /** Remove without revival (player logged out, etc.). */
    public static DownState removeWithoutRevival(UUID uuid) {
        DownState st = DOWNED.remove(uuid);
        if (st != null) st.bossBar.clearPlayers();
        ACTIVE_REVIVERS.remove(uuid);
        return st;
    }

    /** Re-add a player to their bossbar after re-login or world change. */
    public static void reattach(ServerPlayerEntity player) {
        DownState state = DOWNED.get(player.getUuid());
        if (state == null) return;
        state.bossBar.addPlayer(player);
        applyDownEffects(player);
        enforceProne(player);
    }

    /**
     * Update the recorded "down position" to follow the player. We do NOT snap them
     * back, because that fights legitimate teleports (/tp, /tpa, ender pearls,
     * piston pushes, etc.) which is exactly the bug Hardcore Revival had.
     */
    public static void enforcePosition(ServerPlayerEntity player) {
        DownState state = DOWNED.get(player.getUuid());
        if (state == null) return;
        state.downDimension = player.getServerWorld().getRegistryKey();
        state.downPosition = player.getPos();
    }
}
