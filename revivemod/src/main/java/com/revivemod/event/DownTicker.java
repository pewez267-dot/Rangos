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
                state.reviveProgressTicks++;

                // Particles to show channel progress.
                if (state.reviveProgressTicks % 4 == 0) {
                    world.spawnParticles(ParticleTypes.HEART,
                            downed.getX(), downed.getY() + 1.6, downed.getZ(),
                            1, 0.3, 0.2, 0.3, 0.0);
                }

                // Soft "tink" pulse every 10 ticks (0.5s) with rising pitch as
                // the channel approaches completion.
                if (state.reviveProgressTicks % 10 == 0) {
                    float p = (float) state.reviveProgressTicks / Math.max(1, cfg.reviveTimeTicks);
                    if (p > 1f) p = 1f;
                    float pitch = 0.9f + p * 0.7f; // 0.9 -> 1.6
                    world.playSound(null,
                            downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_HIT,
                            SoundCategory.PLAYERS,
                            0.45f, pitch);
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
                // Channel broke: notify the reviver if they're still online and play a soft cancel SFX.
                if (state.channelActive && state.reviverUuid != null) {
                    ServerPlayerEntity oldReviver = server.getPlayerManager().getPlayer(state.reviverUuid);
                    if (oldReviver != null) {
                        oldReviver.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                                Text.literal("Reanimacion cancelada").formatted(Formatting.RED)));
                    }
                    world.playSound(null,
                            downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK,
                            SoundCategory.PLAYERS,
                            0.35f, 0.7f);
                }
                state.reviverUuid = null;
                state.reviveProgressTicks = 0;
                state.channelActive = false;
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

            // Periodic feedback for the downed player. Soft amethyst "tink" every
            // 2 seconds at low volume so it stays aesthetic.
            if (state.remainingTicks % 40 == 0 && state.remainingTicks > 0) {
                world.playSound(null,
                        downed.getX(), downed.getY(), downed.getZ(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_HIT,
                        SoundCategory.PLAYERS,
                        0.25f, 0.7f);
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
     * Find a valid reviver. The channel is "armed" by right-clicking the downed
     * player (handled in InteractionHandler) which sets {@code reviverUuid}.
     * Here we keep the channel alive only as long as the original reviver is:
     *   - online
     *   - in range
     *   - looking roughly at the downed player (line-of-sight cone)
     *   - not knocked-down or in spectator mode
     */
    private static ServerPlayerEntity findReviver(ServerPlayerEntity downed, ReviveConfig cfg) {
        DownState state = DownManager.get(downed);
        if (state == null || state.reviverUuid == null) return null;

        ServerPlayerEntity reviver = downed.getServerWorld().getServer()
                .getPlayerManager().getPlayer(state.reviverUuid);
        if (reviver == null) return null;
        if (reviver.isSpectator()) return null;
        if (DownManager.isDown(reviver)) return null;
        if (!reviver.isAlive()) return null;
        if (reviver.getServerWorld() != downed.getServerWorld()) return null;

        double maxSq = cfg.reviveDistance * cfg.reviveDistance;
        if (reviver.squaredDistanceTo(downed) > maxSq) return null;
        if (!isLookingAt(reviver, downed)) return null;

        return reviver;
    }

    /**
     * True if the reviver's look direction points roughly at the downed player's
     * body (cosine threshold ~= 0.7, i.e. a ~45 degrees half-angle cone).
     */
    private static boolean isLookingAt(ServerPlayerEntity reviver, ServerPlayerEntity downed) {
        net.minecraft.util.math.Vec3d eye = reviver.getEyePos();
        net.minecraft.util.math.Vec3d look = reviver.getRotationVec(1.0f);
        // Aim near head height so a reviver standing directly next to the body
        // and looking forward still passes the cone (chest-height aim fails at
        // very short horizontal distances).
        net.minecraft.util.math.Vec3d aim = downed.getPos().add(0, downed.getHeight() * 0.85, 0);
        net.minecraft.util.math.Vec3d toAim = aim.subtract(eye);
        double dist = toAim.length();
        if (dist < 0.001) return true;
        double dot = look.dotProduct(toAim.multiply(1.0 / dist));
        return dot > 0.70;
    }

    private static String bar(int pct) {
        int filled = pct / 5;
        StringBuilder b = new StringBuilder(20);
        for (int i = 0; i < 20; i++) b.append(i < filled ? '|' : '.');
        return b.toString();
    }
}
