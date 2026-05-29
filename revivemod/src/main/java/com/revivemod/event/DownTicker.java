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

    /** Bright red "blood" dust used for the bleeding effect. */
    private static final DustParticleEffect BLOOD = new DustParticleEffect(new Vector3f(0.62f, 0.0f, 0.0f), 1.4f);

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

            // ---- Bleeding: red blood particles around the body ----
            if (state.remainingTicks % 6 == 0) {
                world.spawnParticles(BLOOD,
                        downed.getX(), downed.getY() + 0.25, downed.getZ(),
                        6, 0.35, 0.1, 0.35, 0.0);
            }
            if (state.remainingTicks % 14 == 0) {
                world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR,
                        downed.getX(), downed.getY() + 0.6, downed.getZ(),
                        2, 0.3, 0.2, 0.3, 0.0);
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
                            SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 0.5f, 1.4f);
                }
                if (state.reviveProgressTicks % 4 == 0) {
                    world.spawnParticles(ParticleTypes.HEART,
                            downed.getX(), downed.getY() + 1.0, downed.getZ(), 1, 0.3, 0.2, 0.3, 0.0);
                }
                // Soft rising bell every ~0.5s while being revived.
                if (state.reviveProgressTicks / 10 != (state.reviveProgressTicks - n) / 10) {
                    float p = Math.min(1f, (float) state.reviveProgressTicks / Math.max(1, cfg.reviveTimeTicks));
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.PLAYERS, 0.45f, 1.0f + p * 0.8f);
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
                        Text.literal("Te estan reviviendo ").formatted(Formatting.WHITE)
                                .append(Text.literal("[" + bar + "] " + pct + "%").formatted(Formatting.GREEN))));
                if (state.reviveProgressTicks >= cfg.reviveTimeTicks) {
                    DownManager.revive(downed);
                    continue;
                }
            } else {
                if (state.channelActive) {
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 0.3f, 0.8f);
                }
                state.channelActive = false;
                state.reviveProgressTicks = 0;
            }

            // ---- Countdown (bossbar) ----
            state.remainingTicks--;
            state.bossBar.setPercent(Math.max(0f, (float) state.remainingTicks / state.totalTicks));
            if (state.remainingTicks % 20 == 0) {
                int secondsLeft = (state.remainingTicks + 19) / 20;
                state.bossBar.setName(Text.literal("Desangrandose - ")
                        .formatted(Formatting.RED, Formatting.BOLD)
                        .append(Text.literal(secondsLeft + "s").formatted(Formatting.WHITE)));

                // Dying heartbeat: a soft note-block 'bit' pulse every second.
                // The pitch climbs as the timer runs out so it feels more urgent.
                // Skipped while an ally is actively reviving (that has its own SFX).
                if (!state.channelActive && state.remainingTicks > 0) {
                    float frac = (float) state.remainingTicks / state.totalTicks; // 1.0 -> 0.0
                    float pitch = 1.2f - frac * 0.6f; // 0.6 (full) -> 1.2 (almost dead)
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.PLAYERS, 0.5f, pitch);
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
