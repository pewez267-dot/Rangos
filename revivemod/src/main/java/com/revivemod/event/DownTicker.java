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
                            SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.5f, 1.0f);
                }
                if (state.reviveProgressTicks % 4 == 0) {
                    world.spawnParticles(ParticleTypes.HEART,
                            downed.getX(), downed.getY() + 1.0, downed.getZ(), 1, 0.3, 0.2, 0.3, 0.0);
                }
                if (state.reviveProgressTicks / 10 != (state.reviveProgressTicks - n) / 10) {
                    float p = Math.min(1f, (float) state.reviveProgressTicks / Math.max(1, cfg.reviveTimeTicks));
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.45f, 0.9f + p * 0.7f);
                }
                int pct = Math.min(100, (int) (100.0 * state.reviveProgressTicks / Math.max(1, cfg.reviveTimeTicks)));
                String speed = n > 1 ? " x" + n : "";
                for (ServerPlayerEntity r : revivers) {
                    final int fp = pct;
                    r.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                            Text.literal("Reviviendo " + fp + "%" + speed).formatted(Formatting.GREEN)));
                }
                if (state.reviveProgressTicks >= cfg.reviveTimeTicks) {
                    DownManager.revive(downed);
                    continue;
                }
            } else {
                if (state.channelActive) {
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.3f, 0.7f);
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
            }
            if (state.remainingTicks <= 0) {
                DownManager.forceDeath(downed, downed.getDamageSources().genericKill());
            }
        }

        for (UUID id : toRemove) DownManager.removeWithoutRevival(id);
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
