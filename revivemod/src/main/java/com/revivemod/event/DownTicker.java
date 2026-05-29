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

/**
 * Runs every server tick:
 *  - keeps each downed player prone + immobilised and following teleports
 *  - decrements the countdown and updates the bossbar
 *  - collects every valid reviver (armed by right-click, in range, looking) and
 *    advances the channel faster the more revivers there are
 *  - grants active revivers invincibility (registered in DownManager)
 *  - kills players whose timer expired
 */
public final class DownTicker {

    private DownTicker() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DownTicker::tick);
    }

    private static void tick(MinecraftServer server) {
        // Rebuild the active-reviver invincibility set from scratch each tick.
        DownManager.clearActiveRevivers();

        if (DownManager.all().isEmpty()) return;

        ReviveConfig cfg = ReviveMod.getConfig();
        List<UUID> toRemove = new ArrayList<>();

        DownState[] snapshot = DownManager.all().toArray(new DownState[0]);
        for (DownState state : snapshot) {
            if (!DownManager.isDown(state.playerUuid)) continue;
            ServerPlayerEntity downed = server.getPlayerManager().getPlayer(state.playerUuid);
            if (downed == null) {
                // Offline: pause the timer.
                continue;
            }
            if (!downed.isAlive() || downed.isRemoved()) {
                toRemove.add(state.playerUuid);
                continue;
            }

            ServerWorld world = downed.getServerWorld();

            // 1. Follow teleports (no snap-back) + lock hotbar slot.
            //    Lying-down pose is owned by the SLEEPING_POSITION data tracker
            //    and the PlayerEntityMixin — no per-tick pose forcing needed.
            DownManager.enforcePosition(downed);
            DownManager.enforceLockedSlot(downed);

            // 1.b. If the options menu got closed (ESC), reopen it on the
            //      very next tick. Without this, vanilla's MinecraftClient
            //      auto-opens its own SleepingChatScreen ("Stop Sleeping"
            //      button) the moment the player closes ours, producing a
            //      visible flicker. The server-side reopen is idempotent.
            if (downed.currentScreenHandler == downed.playerScreenHandler) {
                DownManager.openOptions(downed);
            }

            // 2. Re-apply effects once per second.
            if (state.remainingTicks % 20 == 0) {
                DownManager.applyDownEffects(downed);
            }

            // 3. Collect all currently-valid revivers (armed + in range + looking).
            List<ServerPlayerEntity> revivers = collectRevivers(downed, state, cfg, server);
            int n = revivers.size();

            if (n > 0) {
                // Faster with more revivers: progress += number of revivers.
                state.reviveProgressTicks += n;
                boolean wasActive = state.channelActive;
                state.channelActive = true;

                // Grant invincibility to everyone currently channeling.
                for (ServerPlayerEntity r : revivers) {
                    DownManager.markReviving(r.getUuid());
                }

                if (!wasActive) {
                    // Channel (re)started.
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.5f, 1.0f);
                }

                // Progress particles.
                if (state.reviveProgressTicks % 4 == 0) {
                    world.spawnParticles(ParticleTypes.HEART,
                            downed.getX(), downed.getY() + 1.6, downed.getZ(),
                            1, 0.3, 0.2, 0.3, 0.0);
                }

                // Soft rising "tink" once per ~0.5s of progress, regardless of
                // how many revivers (avoids sound spam when n is large).
                if (state.reviveProgressTicks / 10 != (state.reviveProgressTicks - n) / 10) {
                    float p = (float) state.reviveProgressTicks / Math.max(1, cfg.reviveTimeTicks);
                    if (p > 1f) p = 1f;
                    float pitch = 0.9f + p * 0.7f;
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.45f, pitch);
                }

                int pct = (int) (100.0 * state.reviveProgressTicks / Math.max(1, cfg.reviveTimeTicks));
                if (pct > 100) pct = 100;
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
                // No valid revivers right now.
                if (state.channelActive) {
                    // Channel just broke: soft cancel SFX + notify any armed players online.
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

            // 4. Countdown.
            state.remainingTicks--;
            float frac = Math.max(0f, (float) state.remainingTicks / state.totalTicks);
            state.bossBar.setPercent(frac);
            if (state.remainingTicks % 20 == 0) {
                int secondsLeft = (state.remainingTicks + 19) / 20;
                state.bossBar.setName(Text.literal("Noqueado - ")
                        .formatted(Formatting.RED, Formatting.BOLD)
                        .append(Text.literal(secondsLeft + "s").formatted(Formatting.WHITE)));
            }

            // Soft periodic heartbeat for the downed player.
            if (state.remainingTicks % 40 == 0 && state.remainingTicks > 0) {
                world.playSound(null, downed.getX(), downed.getY(), downed.getZ(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.25f, 0.7f);
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
     * Returns the list of revivers currently channeling: those who armed the
     * channel (right-clicked), are online, alive, not downed, in the same world,
     * within range, and looking at the downed player. Armed players who went
     * offline / got downed / changed dimension are pruned from the set; those
     * who merely walked away or looked away stay armed (so they can resume
     * without re-clicking) but don't count this tick.
     */
    private static List<ServerPlayerEntity> collectRevivers(ServerPlayerEntity downed, DownState state,
                                                            ReviveConfig cfg, MinecraftServer server) {
        List<ServerPlayerEntity> result = new ArrayList<>();
        double maxSq = cfg.reviveDistance * cfg.reviveDistance;

        Iterator<UUID> it = state.armedRevivers.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            ServerPlayerEntity r = server.getPlayerManager().getPlayer(id);
            // Prune permanently-invalid revivers.
            if (r == null || !r.isAlive() || r.isSpectator()
                    || DownManager.isDown(r)
                    || r.getServerWorld() != downed.getServerWorld()) {
                it.remove();
                continue;
            }
            // Count only those currently in range AND looking at the body.
            if (r.squaredDistanceTo(downed) <= maxSq && isLookingAt(r, downed)) {
                result.add(r);
            }
        }
        return result;
    }

    /** True if the reviver is looking roughly at the downed player (~45 deg cone). */
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
