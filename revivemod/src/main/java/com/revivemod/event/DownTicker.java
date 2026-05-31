package com.revivemod.event;

import com.revivemod.ReviveMod;
import com.revivemod.config.ReviveConfig;
import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DownTicker {

    /** Bright red "blood" dust used for the bleeding effect (small scale so it
     *  reads as blood drops, not a potion cloud). Built per-spawn from config. */
    private static DustParticleEffect blood(ReviveConfig cfg) {
        return new DustParticleEffect(new Vector3f(0.55f, 0.0f, 0.0f), cfg.bloodParticleScale);
    }

    /** Small pretty white dust shown alongside the blood (same timing/spread). */
    private static DustParticleEffect white(ReviveConfig cfg) {
        return new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), cfg.whiteParticleScale);
    }

    /** A fixed, musical amethyst pitch sequence (a pentatonic-ish ladder). Used
     *  both ascending (being revived) and descending (countdown), so the chimes
     *  always follow a melody instead of sounding random. */
    private static final float[] SEQ = { 0.66f, 0.75f, 0.84f, 1.0f, 1.12f, 1.26f, 1.5f, 1.68f };

    private DownTicker() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DownTicker::tick);
    }

    private static void tick(MinecraftServer server) {
        DownManager.clearActiveRevivers();
        if (DownManager.all().isEmpty()) return;

        ReviveConfig cfg = ReviveMod.getConfig();
        List<UUID> toRemove = new ArrayList<>();

        DownState[] snapshot = DownManager.all().toArray(new DownState[0]);
        for (DownState state : snapshot) {
            if (!DownManager.isDown(state.playerUuid)) continue;
            ServerPlayerEntity downed = server.getPlayerManager().getPlayer(state.playerUuid);
            if (downed == null) continue;
            if (!downed.isAlive() || downed.isRemoved()) {
                toRemove.add(state.playerUuid);
                continue;
            }

            ServerWorld world = downed.getServerWorld();
            DownManager.enforcePosition(downed);
            DownManager.enforceLockedSlot(downed);
            if (state.remainingTicks % 20 == 0) DownManager.applyDownEffects(downed);

            // ---- Surrender / self-revive triggered by client (full 4s hold) ----
            if (DownManager.consumeSurrenderToggle(state.playerUuid)) {
                DownManager.forceDeath(downed, downed.getDamageSources().genericKill());
                continue;
            }
            if (DownManager.consumeSelfToggle(state.playerUuid)) {
                if (DownManager.selfRevive(downed)) continue;
            }

            // ---- Bleeding: a few red blood drops + a little white sparkle ----
            if (state.remainingTicks % Math.max(1, cfg.bloodParticleInterval) == 0) {
                world.spawnParticles(blood(cfg),
                        downed.getX(), downed.getY() + 0.2, downed.getZ(),
                        Math.max(0, cfg.bloodParticleCount), 0.3, 0.05, 0.3, 0.0);
                world.spawnParticles(white(cfg),
                        downed.getX(), downed.getY() + 0.2, downed.getZ(),
                        Math.max(0, cfg.whiteParticleCount), 0.3, 0.05, 0.3, 0.0);
            }

            // ---- Revive by allies actively right-clicking ----
            List<ServerPlayerEntity> revivers = collectRevivers(downed, state, cfg, server);
            int n = revivers.size();
            if (n > 0) {
                state.reviveProgressTicks += n;
                boolean wasActive = state.channelActive;
                state.channelActive = true;
                for (ServerPlayerEntity r : revivers) DownManager.markReviving(r.getUuid());

                if (!wasActive) {
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.5f, 1.2f);
                }
                if (state.reviveProgressTicks % 4 == 0) {
                    world.spawnParticles(ParticleTypes.HEART,
                            downed.getX(), downed.getY() + 1.0, downed.getZ(), 1, 0.3, 0.2, 0.3, 0.0);
                }
                // Soft amethyst chime that steps UP through a fixed sequence
                // every ~0.5s while being revived (ascending = recovering).
                if (state.reviveProgressTicks / 10 != (state.reviveProgressTicks - n) / 10) {
                    int step = (state.reviveProgressTicks / 10) % SEQ.length;
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, cfg.reviveTickVolume, SEQ[step]);
                }
                int pct = Math.min(100, (int) (100.0 * state.reviveProgressTicks / Math.max(1, cfg.reviveTimeTicks)));
                String speed = n > 1 ? " x" + n : "";
                String bar = bar(pct);
                // Progress bar shown to the reviver(s) AND the downed player,
                // styled like the original v1.1.0 bar: Label [||||....] 60%.
                net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket reviverMsg =
                        new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                                Text.literal("Reviviendo " ).formatted(Formatting.WHITE)
                                        .append(Text.literal("[" + bar + "] " + pct + "%" + speed).formatted(Formatting.YELLOW)));
                for (ServerPlayerEntity r : revivers) {
                    r.networkHandler.sendPacket(reviverMsg);
                }
                downed.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                        Text.literal("Te están reviviendo ").formatted(Formatting.WHITE)
                                .append(Text.literal("[" + bar + "] " + pct + "%").formatted(Formatting.GREEN))));
                if (state.reviveProgressTicks >= cfg.reviveTimeTicks) {
                    DownManager.revive(downed);
                    continue;
                }
            } else {
                if (state.channelActive) {
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.4f, 0.6f);
                }
                state.channelActive = false;
                state.reviveProgressTicks = 0;
            }

            // ---- Countdown (bossbar) ----
            state.remainingTicks--;
            state.bossBar.setPercent(Math.max(0f, (float) state.remainingTicks / state.totalTicks));
            if (state.remainingTicks % 20 == 0) {
                int secondsLeft = (state.remainingTicks + 19) / 20;
                state.bossBar.setName(Text.literal("Desangrándose - ")
                        .formatted(Formatting.RED, Formatting.BOLD)
                        .append(Text.literal(secondsLeft + "s").formatted(Formatting.WHITE)));

                // Countdown heartbeat: a soft amethyst chime each second that
                // steps DOWN through a fixed pitch sequence (descending = fading
                // away). Deterministic, never random. Skipped while being revived.
                if (!state.channelActive && state.remainingTicks > 0) {
                    int idx = ((state.remainingTicks / 20)) % SEQ.length;
                    float pitch = SEQ[SEQ.length - 1 - idx]; // walk the sequence downward
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, cfg.countdownTickVolume, pitch);
                }
            }
            if (state.remainingTicks <= 0) {
                DownManager.forceDeath(downed, downed.getDamageSources().genericKill());
            }
        }

        for (UUID id : toRemove) DownManager.removeWithoutRevival(id);
    }

    /** 20-char text progress bar like the original v1.1.0: |||||........ */
    private static String bar(int pct) {
        int filled = pct / 5;
        StringBuilder b = new StringBuilder(20);
        for (int i = 0; i < 20; i++) b.append(i < filled ? '|' : '.');
        return b.toString();
    }

    /**
     * Revivers are allies who right-clicked recently (within their click window)
     * and are still in range. The window decays every tick, so an ally must keep
     * right-clicking to make progress — looking alone does nothing.
     */
    private static List<ServerPlayerEntity> collectRevivers(ServerPlayerEntity downed, DownState state,
                                                            ReviveConfig cfg, MinecraftServer server) {
        List<ServerPlayerEntity> result = new ArrayList<>();
        double maxSq = cfg.reviveDistance * cfg.reviveDistance;
        Iterator<Map.Entry<UUID, Integer>> it = state.reviverWindow.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            int left = e.getValue() - 1;
            if (left <= 0) { it.remove(); continue; }
            e.setValue(left);
            ServerPlayerEntity r = server.getPlayerManager().getPlayer(e.getKey());
            if (r == null || !r.isAlive() || r.isSpectator() || DownManager.isDown(r)
                    || r.getServerWorld() != downed.getServerWorld()
                    || r.squaredDistanceTo(downed) > maxSq) {
                continue;
            }
            result.add(r);
        }
        return result;
    }
}
