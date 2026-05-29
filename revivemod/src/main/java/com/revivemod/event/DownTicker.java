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
import java.util.UUID;

public final class DownTicker {

    /** How long (in ticks) the player must hold Sneak / Sprint to trigger surrender / self-revive. */
    private static final int HOLD_TICKS = 100; // 5 seconds

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

            if (state.remainingTicks % 20 == 0) {
                DownManager.applyDownEffects(downed);
            }

            // ---- Sneak-to-surrender / Sprint-to-self-revive (5s hold each) ----
            handleHoldKeys(downed, state, cfg);
            if (!DownManager.isDown(state.playerUuid)) continue; // surrender / self-revive resolved this tick

            // ---- Revive channel (right-click + look) ----
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
                            downed.getX(), downed.getY() + 1.6, downed.getZ(),
                            1, 0.3, 0.2, 0.3, 0.0);
                }
                if (state.reviveProgressTicks / 10 != (state.reviveProgressTicks - n) / 10) {
                    float p = Math.min(1f, (float) state.reviveProgressTicks / Math.max(1, cfg.reviveTimeTicks));
                    float pitch = 0.9f + p * 0.7f;
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.45f, pitch);
                }
                int pct = Math.min(100, (int) (100.0 * state.reviveProgressTicks / Math.max(1, cfg.reviveTimeTicks)));
                String speed = n > 1 ? " x" + n : "";
                for (ServerPlayerEntity r : revivers) {
                    final int fp = pct;
                    r.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                            Text.literal("Reviviendo a " + downed.getGameProfile().getName() + " ")
                                    .formatted(Formatting.GREEN)
                                    .append(Text.literal("[" + bar(fp) + "] " + fp + "%" + speed)
                                            .formatted(Formatting.YELLOW))));
                }
                if (state.reviveProgressTicks >= cfg.reviveTimeTicks) {
                    DownManager.revive(downed);
                    continue;
                }
            } else {
                if (state.channelActive) {
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.35f, 0.7f);
                    for (UUID id : state.armedRevivers) {
                        ServerPlayerEntity r = server.getPlayerManager().getPlayer(id);
                        if (r != null) {
                            r.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                                    Text.literal("Reanimacion cancelada").formatted(Formatting.RED)));
                        }
                    }
                }
                state.channelActive = false;
                state.reviveProgressTicks = 0;
            }

            // ---- Countdown ----
            state.remainingTicks--;
            float frac = Math.max(0f, (float) state.remainingTicks / state.totalTicks);
            state.bossBar.setPercent(frac);
            if (state.remainingTicks % 20 == 0) {
                int secondsLeft = (state.remainingTicks + 19) / 20;
                state.bossBar.setName(Text.literal("Noqueado - ")
                        .formatted(Formatting.RED, Formatting.BOLD)
                        .append(Text.literal(secondsLeft + "s").formatted(Formatting.WHITE)));
            }
            if (state.remainingTicks % 40 == 0 && state.remainingTicks > 0) {
                world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.25f, 0.7f);
            }

            if (state.remainingTicks <= 0) {
                downed.sendMessage(Text.literal("Has muerto: nadie te revivio a tiempo")
                        .formatted(Formatting.DARK_RED), false);
                DownManager.forceDeath(downed, downed.getDamageSources().genericKill());
            }
        }

        for (UUID id : toRemove) DownManager.removeWithoutRevival(id);
    }

    /**
     * On-screen prompts and hold-key detection:
     *   SNEAK (Shift) held 5s   -> surrender
     *   SPRINT (Ctrl) held 5s   -> self-revive (paying selfReviveLevelCost levels)
     *
     * The instructions and per-key progress bar are pushed continuously to the
     * action bar (the slim text strip just above the hotbar), no inventory GUI.
     */
    private static void handleHoldKeys(ServerPlayerEntity downed, DownState state, ReviveConfig cfg) {
        boolean sneaking = downed.isSneaking();
        boolean sprinting = downed.isSprinting();

        state.sneakHoldTicks = sneaking ? Math.min(HOLD_TICKS, state.sneakHoldTicks + 1) : 0;
        boolean canSelf = cfg.allowSelfRevive && downed.experienceLevel >= cfg.selfReviveLevelCost;
        state.sprintHoldTicks = (sprinting && canSelf) ? Math.min(HOLD_TICKS, state.sprintHoldTicks + 1) : 0;

        // Compose the action-bar message.
        Text msg;
        if (state.sneakHoldTicks > 0) {
            int pct = state.sneakHoldTicks * 100 / HOLD_TICKS;
            msg = Text.literal("Rindiendote ")
                    .formatted(Formatting.RED, Formatting.BOLD)
                    .append(Text.literal("[" + bar(pct) + "] " + pct + "%").formatted(Formatting.GRAY));
        } else if (state.sprintHoldTicks > 0) {
            int pct = state.sprintHoldTicks * 100 / HOLD_TICKS;
            msg = Text.literal("Auto-reviviendo ")
                    .formatted(Formatting.GREEN, Formatting.BOLD)
                    .append(Text.literal("[" + bar(pct) + "] " + pct + "%").formatted(Formatting.GRAY));
        } else {
            // Idle: show both prompts.
            String costStr = cfg.allowSelfRevive
                    ? (canSelf ? (cfg.selfReviveLevelCost + " niveles")
                               : ("faltan niveles (" + downed.experienceLevel + "/" + cfg.selfReviveLevelCost + ")"))
                    : "desactivado";
            msg = Text.literal("Manten ").formatted(Formatting.GRAY)
                    .append(Text.literal("SHIFT").formatted(Formatting.RED, Formatting.BOLD))
                    .append(Text.literal(" para rendirte | ").formatted(Formatting.GRAY))
                    .append(Text.literal("CTRL").formatted(Formatting.GREEN, Formatting.BOLD))
                    .append(Text.literal(" auto-revivir (" + costStr + ")").formatted(Formatting.GRAY));
        }
        // Don't drown out a reviver's progress bar — only push our own action-bar text
        // when nobody is currently reviving the player.
        if (!state.channelActive) {
            downed.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(msg));
        }

        // Trigger when the hold completes.
        if (state.sneakHoldTicks >= HOLD_TICKS) {
            state.sneakHoldTicks = 0;
            DownManager.forceDeath(downed, downed.getDamageSources().genericKill());
            return;
        }
        if (state.sprintHoldTicks >= HOLD_TICKS) {
            state.sprintHoldTicks = 0;
            DownManager.selfRevive(downed);
        }
    }

    private static List<ServerPlayerEntity> collectRevivers(ServerPlayerEntity downed, DownState state,
                                                            ReviveConfig cfg, MinecraftServer server) {
        List<ServerPlayerEntity> result = new ArrayList<>();
        double maxSq = cfg.reviveDistance * cfg.reviveDistance;
        Iterator<UUID> it = state.armedRevivers.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            ServerPlayerEntity r = server.getPlayerManager().getPlayer(id);
            if (r == null || !r.isAlive() || r.isSpectator()
                    || DownManager.isDown(r)
                    || r.getServerWorld() != downed.getServerWorld()) {
                it.remove();
                continue;
            }
            if (r.squaredDistanceTo(downed) <= maxSq && isLookingAt(r, downed)) {
                result.add(r);
            }
        }
        return result;
    }

    static boolean isLookingAt(ServerPlayerEntity reviver, ServerPlayerEntity downed) {
        Vec3d eye = reviver.getEyePos();
        Vec3d look = reviver.getRotationVec(1.0f);
        Vec3d aim = downed.getPos().add(0, downed.getHeight() * 0.85, 0);
        Vec3d toAim = aim.subtract(eye);
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
