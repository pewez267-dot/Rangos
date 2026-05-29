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
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DownTicker {

    /** Surrender (E) / self-revive (F) channel length: 3 seconds. */
    private static final int CHANNEL_TICKS = 60;

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

            // ---- E = surrender, F = self-revive (toggle channels, 3s) ----
            handleChannels(downed, state, cfg);
            if (!DownManager.isDown(state.playerUuid)) continue; // resolved this tick

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
                            downed.getX(), downed.getY() + 1.4, downed.getZ(), 1, 0.3, 0.2, 0.3, 0.0);
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

            // ---- Countdown (bossbar = the "contador") ----
            state.remainingTicks--;
            state.bossBar.setPercent(Math.max(0f, (float) state.remainingTicks / state.totalTicks));
            if (state.remainingTicks % 20 == 0) {
                int secondsLeft = (state.remainingTicks + 19) / 20;
                state.bossBar.setName(Text.literal("Noqueado - ")
                        .formatted(Formatting.RED, Formatting.BOLD)
                        .append(Text.literal(secondsLeft + "s").formatted(Formatting.WHITE)));
            }
            if (state.remainingTicks % 40 == 0 && state.remainingTicks > 0) {
                world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.22f, 0.7f);
            }
            if (state.remainingTicks <= 0) {
                DownManager.forceDeath(downed, downed.getDamageSources().genericKill());
            }
        }

        for (UUID id : toRemove) DownManager.removeWithoutRevival(id);
    }

    /**
     * E toggles a 3s surrender channel; F toggles a 3s self-revive channel.
     * A short prompt / progress line is shown in the action bar (only when no
     * ally is currently reviving, so we don't fight their progress text).
     */
    private static void handleChannels(ServerPlayerEntity downed, DownState state, ReviveConfig cfg) {
        UUID id = downed.getUuid();

        if (DownManager.consumeSurrenderToggle(id)) {
            state.surrendering = !state.surrendering;
            state.surrenderTicks = 0;
            state.selfReviving = false;
        }
        if (DownManager.consumeSelfToggle(id)) {
            boolean canSelf = cfg.allowSelfRevive && downed.experienceLevel >= cfg.selfReviveLevelCost;
            state.selfReviving = canSelf && !state.selfReviving;
            state.selfTicks = 0;
            state.surrendering = false;
        }

        Text msg = null;

        if (state.surrendering) {
            state.surrenderTicks++;
            int pct = Math.min(100, state.surrenderTicks * 100 / CHANNEL_TICKS);
            msg = Text.literal("Rindiendote " + pct + "%").formatted(Formatting.RED);
            if (state.surrenderTicks >= CHANNEL_TICKS) {
                DownManager.forceDeath(downed, downed.getDamageSources().genericKill());
                return;
            }
        } else if (state.selfReviving) {
            state.selfTicks++;
            int pct = Math.min(100, state.selfTicks * 100 / CHANNEL_TICKS);
            msg = Text.literal("Auto-reviviendo " + pct + "%").formatted(Formatting.GREEN);
            if (state.selfTicks >= CHANNEL_TICKS) {
                if (!DownManager.selfRevive(downed)) state.selfReviving = false;
                return;
            }
        } else {
            // Compact idle prompt.
            msg = Text.literal("E ").formatted(Formatting.RED, Formatting.BOLD)
                    .append(Text.literal("rendirse  ").formatted(Formatting.GRAY))
                    .append(Text.literal("F ").formatted(Formatting.GREEN, Formatting.BOLD))
                    .append(Text.literal("auto-revivir").formatted(Formatting.GRAY));
        }

        if (msg != null && !state.channelActive) {
            downed.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(msg));
        }
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

    private static String bar(int pct) {
        int filled = pct / 10;
        StringBuilder b = new StringBuilder(10);
        for (int i = 0; i < 10; i++) b.append(i < filled ? '|' : '.');
        return b.toString();
    }

    private static boolean isLookingAt(ServerPlayerEntity reviver, ServerPlayerEntity downed) {
        Vec3d eye = reviver.getEyePos();
        Vec3d look = reviver.getRotationVec(1.0f);
        Vec3d aim = downed.getPos().add(0, downed.getHeight() * 0.5, 0);
        Vec3d toAim = aim.subtract(eye);
        double dist = toAim.length();
        if (dist < 0.001) return true;
        return look.dotProduct(toAim.multiply(1.0 / dist)) > 0.5;
    }
}
