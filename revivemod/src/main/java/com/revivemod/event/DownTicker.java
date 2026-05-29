package com.revivemod.event;

import com.revivemod.ReviveMod;
import com.revivemod.config.ReviveConfig;
import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs every server tick and is responsible for:
 *  - decrementing each downed player's countdown
 *  - updating the bossbar text and percentage
 *  - locking the downed player's position
 *  - looking for nearby sneaking allies and progressing the revival channel
 *  - killing players whose timer expired
 *  - playing periodic feedback (particles, action-bar)
 */
public final class DownTicker {

    private DownTicker() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DownTicker::tick);
    }

    private static void tick(MinecraftServer server) {
        if (DownManager.all().isEmpty()) return;

        ReviveConfig cfg = ReviveMod.getConfig();
        List<UUID> toRemove = new ArrayList<>();

        // Iterate over a snapshot of the values: revive() / forceDeath() mutate
        // the underlying map and we MUST NOT throw ConcurrentModificationException
        // when more than one player is downed at the same time.
        DownState[] snapshot = DownManager.all().toArray(new DownState[0]);
        for (DownState state : snapshot) {
            // Skip states that were already removed earlier in this same tick
            // (e.g. another revive cascade).
            if (!DownManager.isDown(state.playerUuid)) continue;
            ServerPlayerEntity downed = server.getPlayerManager().getPlayer(state.playerUuid);
            if (downed == null) {
                // Player offline: pause the timer (do nothing this tick).
                continue;
            }
            if (!downed.isAlive() || downed.isRemoved()) {
                toRemove.add(state.playerUuid);
                continue;
            }

            ServerWorld world = downed.getServerWorld();

            // 1. Position lock (handles /tp, /tpa, ender pearls launched mid-down, etc.).
            DownManager.enforcePosition(downed);

            // 2. Make sure the down effects are still applied (some commands strip effects).
            //    Throttled to once per second to avoid spamming effect-update packets.
            if (state.remainingTicks % 20 == 0) {
                DownManager.applyDownEffects(downed);
            }

            // 3. Find a reviver nearby.
            ServerPlayerEntity reviver = findReviver(downed, cfg);

            if (reviver != null) {
                // Continue or start the channel.
                if (state.reviverUuid == null || !state.reviverUuid.equals(reviver.getUuid())) {
                    state.reviverUuid = reviver.getUuid();
                    state.reviveProgressTicks = 0;
                    reviver.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                            Text.literal("Reviviendo a " + downed.getGameProfile().getName() + "...")
                                    .formatted(Formatting.GREEN)));
                }
                state.reviveProgressTicks++;

                // Particles to show channel progress.
                if (state.reviveProgressTicks % 4 == 0) {
                    world.spawnParticles(ParticleTypes.HEART,
                            downed.getX(), downed.getY() + 1.6, downed.getZ(),
                            1, 0.3, 0.2, 0.3, 0.0);
                }

                // Action-bar progress for the reviver every tick.
                int pct = (int) (100.0 * state.reviveProgressTicks / Math.max(1, cfg.reviveTimeTicks));
                if (pct > 100) pct = 100;
                reviver.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                        Text.literal("Reviviendo a " + downed.getGameProfile().getName() + " ")
                                .formatted(Formatting.GREEN)
                                .append(Text.literal("[" + bar(pct) + "] " + pct + "%").formatted(Formatting.YELLOW))));

                if (state.reviveProgressTicks >= cfg.reviveTimeTicks) {
                    DownManager.revive(downed);
                    continue;
                }
            } else {
                if (state.reviverUuid != null) {
                    ServerPlayerEntity oldReviver = server.getPlayerManager().getPlayer(state.reviverUuid);
                    if (oldReviver != null) {
                        oldReviver.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                                Text.literal("Reanimacion cancelada").formatted(Formatting.RED)));
                    }
                }
                state.reviverUuid = null;
                state.reviveProgressTicks = 0;
            }

            // 4. Countdown.
            state.remainingTicks--;
            float frac = Math.max(0f, (float) state.remainingTicks / state.totalTicks);
            state.bossBar.setPercent(frac);
            // Only update the bossbar title once per second to keep packet traffic minimal.
            if (state.remainingTicks % 20 == 0) {
                int secondsLeft = (state.remainingTicks + 19) / 20;
                state.bossBar.setName(Text.literal("Noqueado - ")
                        .formatted(Formatting.RED, Formatting.BOLD)
                        .append(Text.literal(secondsLeft + "s").formatted(Formatting.WHITE)));
            }

            // Periodic feedback for the downed player.
            if (state.remainingTicks % 20 == 0) {
                world.playSound(null,
                        downed.getX(), downed.getY(), downed.getZ(),
                        SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(),
                        SoundCategory.PLAYERS,
                        0.4f, 0.6f);
            }

            // Damage particles around the downed player.
            if (state.remainingTicks % 8 == 0) {
                world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR,
                        downed.getX(), downed.getY() + 1.0, downed.getZ(),
                        2, 0.3, 0.2, 0.3, 0.0);
            }

            // 5. Time up?
            if (state.remainingTicks <= 0) {
                downed.sendMessage(Text.literal("Has muerto: nadie te revivio a tiempo")
                        .formatted(Formatting.DARK_RED), false);
                DownManager.forceDeath(downed, downed.getDamageSources().genericKill());
            }
        }

        for (UUID id : toRemove) {
            DownManager.removeWithoutRevival(id);
        }
    }

    /**
     * Find a valid reviver. If we already have an in-progress channel, we
     * stick with the current reviver as long as they keep sneaking and stay
     * in range; only when they break the channel do we look for a new one.
     * This prevents a hostile player from interrupting a legitimate revive
     * by stepping closer than the original reviver.
     */
    private static ServerPlayerEntity findReviver(ServerPlayerEntity downed, ReviveConfig cfg) {
        DownState state = DownManager.get(downed);
        double maxSq = cfg.reviveDistance * cfg.reviveDistance;

        // 1. Check if the existing reviver is still valid and prefer them.
        if (state != null && state.reviverUuid != null) {
            ServerPlayerEntity current = downed.getServerWorld().getServer()
                    .getPlayerManager().getPlayer(state.reviverUuid);
            if (current != null && isValidReviver(current, downed, maxSq)) {
                return current;
            }
        }

        // 2. Otherwise pick the closest valid sneaker.
        double bestSq = maxSq;
        ServerPlayerEntity best = null;
        for (ServerPlayerEntity p : downed.getServerWorld().getPlayers()) {
            if (!isValidReviver(p, downed, maxSq)) continue;
            double sq = p.squaredDistanceTo(downed);
            if (sq <= bestSq) {
                bestSq = sq;
                best = p;
            }
        }
        return best;
    }

    private static boolean isValidReviver(ServerPlayerEntity p, ServerPlayerEntity downed, double maxSq) {
        if (p.getUuid().equals(downed.getUuid())) return false;
        if (p.isSpectator()) return false;
        if (DownManager.isDown(p)) return false;
        if (!p.isSneaking()) return false;
        return p.squaredDistanceTo(downed) <= maxSq;
    }

    private static String bar(int pct) {
        int filled = pct / 5;
        StringBuilder b = new StringBuilder(20);
        for (int i = 0; i < 20; i++) b.append(i < filled ? '|' : '.');
        return b.toString();
    }
}
