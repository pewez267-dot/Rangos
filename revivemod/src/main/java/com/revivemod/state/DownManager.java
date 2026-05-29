package com.revivemod.state;

import com.revivemod.ReviveMod;
import com.revivemod.config.ReviveConfig;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import java.util.UUID;

/**
 * Central tracker of downed players. Keyed by player UUID so the state survives
 * dimension changes, /tpa, /tp, logout/login (we re-knock them down on join).
 */
public final class DownManager {

    private static final Map<UUID, DownState> DOWNED = new HashMap<>();

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
        // Snapshot every effect that's currently on the player so we can restore
        // them on revive without having our knock-down effects clobber them.
        // We copy the instances since StatusEffectInstance is mutable.
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

        applyDownEffects(player);

        // Show bossbar to the player.
        state.bossBar.addPlayer(player);
        state.bossBar.setName(Text.literal("Noqueado").formatted(Formatting.RED, Formatting.BOLD));
        state.bossBar.setPercent(1.0f);

        // Friendly title for the downed player.
        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
                Text.literal("Estas noqueado").formatted(Formatting.DARK_RED, Formatting.BOLD)));
        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(
                Text.literal("Otro jugador puede revivirte con click derecho").formatted(Formatting.GRAY)));
        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 60, 20));

        // Soft amethyst chime at low pitch on knockdown.
        ServerWorld world = player.getServerWorld();
        world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS,
                0.7f, 0.7f
        );

        Text msg = Text.literal("[Revive] ")
                .formatted(Formatting.GOLD)
                .append(Text.literal(player.getGameProfile().getName()).formatted(Formatting.YELLOW))
                .append(Text.literal(" ha sido noqueado. Tienes ").formatted(Formatting.GRAY))
                .append(Text.literal(cfg.downTimeSeconds + "s").formatted(Formatting.RED))
                .append(Text.literal(" para revivirlo.").formatted(Formatting.GRAY));
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
            p.sendMessage(msg, false);
        }

        ReviveMod.LOGGER.info("[revivemod] {} knocked down (cause={})",
                player.getGameProfile().getName(),
                cause == null ? "unknown" : cause.getName());

        // Clear hostile mob aggro on the downed player so they aren't pummelled
        // for the entire 60-second countdown.
        if (cfg.clearMobAggroOnDown) {
            world.getEntitiesByClass(
                    net.minecraft.entity.mob.MobEntity.class,
                    player.getBoundingBox().expand(48.0),
                    mob -> mob.getTarget() == player
            ).forEach(mob -> mob.setTarget(null));
        }
    }

    /**
     * Re-apply all down-state effects to a player. Used on tick & on
     * world change / login so the state can never desync.
     */
    public static void applyDownEffects(ServerPlayerEntity player) {
        ReviveConfig cfg = ReviveMod.getConfig();

        // Slowness 7 = effectively immobile. We deliberately don't use JUMP_BOOST
        // amplifier hacks here because high amplifiers wrap to negative bytes
        // when the effect is sent to the client and may cause client crashes.
        player.addStatusEffect(infinite(StatusEffects.SLOWNESS, 7));
        player.addStatusEffect(infinite(StatusEffects.MINING_FATIGUE, 4));
        player.addStatusEffect(infinite(StatusEffects.WEAKNESS, 4));
        player.addStatusEffect(infinite(StatusEffects.BLINDNESS, 0));
        if (cfg.glowingWhileDown) {
            player.addStatusEffect(infinite(StatusEffects.GLOWING, 0));
        }
    }

    private static StatusEffectInstance infinite(RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int amplifier) {
        // -1 duration = infinite, hide particles & icon to avoid spam.
        return new StatusEffectInstance(effect, -1, amplifier, false, false, true);
    }

    /**
     * Cleanly revive a player. Removes the state, removes the effects,
     * restores HP / food, plays effects.
     */
    public static void revive(ServerPlayerEntity player) {
        DownState state = DOWNED.remove(player.getUuid());
        if (state == null) return;

        ReviveConfig cfg = ReviveMod.getConfig();

        state.bossBar.clearPlayers();

        clearDownEffects(player);

        // Restore the snapshot of pre-existing effects (so we don't strip a
        // legitimate Speed potion or beacon Haste).
        for (StatusEffectInstance eff : state.snapshotEffects) {
            // skip effects we ourselves clobber, those are gone for a reason
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
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0, false, true, true));

        ServerWorld world = player.getServerWorld();
        // Soft amethyst chime at high pitch on revive.
        world.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS,
                0.8f, 1.5f);
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                player.getX(), player.getY() + 1.0, player.getZ(),
                25, 0.5, 1.0, 0.5, 0.05);

        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
                Text.literal("Has sido revivido").formatted(Formatting.GREEN, Formatting.BOLD)));
        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 40, 20));

        Text msg = Text.literal("[Revive] ")
                .formatted(Formatting.GOLD)
                .append(Text.literal(player.getGameProfile().getName()).formatted(Formatting.YELLOW))
                .append(Text.literal(" ha sido revivido!").formatted(Formatting.GREEN));
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
            p.sendMessage(msg, false);
        }
    }

    /** Strip every effect we applied while down. */
    public static void clearDownEffects(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
        player.removeStatusEffect(StatusEffects.WEAKNESS);
        player.removeStatusEffect(StatusEffects.BLINDNESS);
        player.removeStatusEffect(StatusEffects.GLOWING);
    }

    /** Player UUIDs currently being force-killed. ALLOW_DEATH ignores these so
     *  forceDeath can never re-trigger the knock-down loop. */
    private static final java.util.Set<UUID> FORCE_KILLING = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static boolean isForceKilling(UUID uuid) {
        return FORCE_KILLING.contains(uuid);
    }

    /**
     * Force the player to actually die (timer expired, /revive kill, void, etc.).
     * Removes the state, clears effects, then deals lethal damage with a guard
     * so the death-handler can't put them back into the downed state.
     */
    public static void forceDeath(ServerPlayerEntity player, DamageSource source) {
        DownState state = DOWNED.remove(player.getUuid());
        if (state != null) {
            state.bossBar.clearPlayers();
        }
        clearDownEffects(player);

        // Always use genericKill so vanilla treats this as an unconditional death,
        // and so our ALLOW_DEATH listener's isLethalAllowed() returns true.
        DamageSource finalSrc = player.getDamageSources().genericKill();

        FORCE_KILLING.add(player.getUuid());
        try {
            player.timeUntilRegen = 0;
            player.damage(finalSrc, Float.MAX_VALUE);
            // Safety: if some other mod cancelled the damage, kill via setHealth.
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
    }

    /** Remove without revival (player logged out, etc.). */
    public static DownState removeWithoutRevival(UUID uuid) {
        DownState st = DOWNED.remove(uuid);
        if (st != null) st.bossBar.clearPlayers();
        return st;
    }

    /** Re-add a player to their bossbar after re-login or world change. */
    public static void reattach(ServerPlayerEntity player) {
        DownState state = DOWNED.get(player.getUuid());
        if (state == null) return;
        state.bossBar.addPlayer(player);
        applyDownEffects(player);
    }

    /** 
     * Update the recorded "down position" to follow the player. We do NOT snap them
     * back, because that fights legitimate teleports (/tp, /tpa, ender pearls,
     * piston pushes, etc.) which is exactly the bug Hardcore Revival had. The
     * Slowness 7 effect already makes them practically immobile by themselves;
     * external moves are accepted and we just update our anchor.
     */
    public static void enforcePosition(ServerPlayerEntity player) {
        DownState state = DOWNED.get(player.getUuid());
        if (state == null) return;
        state.downDimension = player.getServerWorld().getRegistryKey();
        state.downPosition = player.getPos();
    }
}
