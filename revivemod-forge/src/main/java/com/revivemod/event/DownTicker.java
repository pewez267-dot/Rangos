package com.revivemod.event;

import com.revivemod.RevivemodForge;
import com.revivemod.config.ReviveConfig;
import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3f;

public final class DownTicker {
    private static final float[] SEQ = new float[]{0.66f, 0.75f, 0.84f, 1.0f, 1.12f, 1.26f, 1.5f, 1.68f};

    private static DustParticleOptions blood(ReviveConfig cfg) {
        return new DustParticleOptions(new Vector3f(0.55f, 0.0f, 0.0f), cfg.bloodParticleScale);
    }

    private static DustParticleOptions white(ReviveConfig cfg) {
        return new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), cfg.whiteParticleScale);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        DownTicker.tick(event.getServer());
    }

    private static void tick(MinecraftServer server) {
        DownManager.clearActiveRevivers();
        if (DownManager.all().isEmpty()) {
            return;
        }
        ReviveConfig cfg = RevivemodForge.getConfig();
        ArrayList<UUID> toRemove = new ArrayList<UUID>();
        DownState[] snapshot = DownManager.all().toArray(new DownState[0]);
        for (DownState state : snapshot) {
            List<ServerPlayer> revivers;
            int n;
            ServerPlayer downed;
            if (!DownManager.isDown(state.playerUuid) || (downed = server.getPlayerList().getPlayer(state.playerUuid)) == null) continue;
            if (!downed.isAlive() || downed.isRemoved()) {
                toRemove.add(state.playerUuid);
                continue;
            }
            ServerLevel world = downed.serverLevel();
            DownManager.enforcePosition(downed);
            DownManager.enforceLockedSlot(downed);
            downed.setPose(Pose.SWIMMING);
            if (state.remainingTicks % 20 == 0) {
                DownManager.applyDownEffects(downed);
            }
            if (DownManager.consumeSurrenderToggle(state.playerUuid)) {
                DownManager.forceDeath(downed, downed.damageSources().genericKill());
                continue;
            }
            if (DownManager.consumeSelfToggle(state.playerUuid) && DownManager.selfRevive(downed)) continue;
            if (state.remainingTicks % Math.max(1, cfg.bloodParticleInterval) == 0) {
                world.sendParticles((ParticleOptions)DownTicker.blood(cfg), downed.getX(), downed.getY() + 0.2, downed.getZ(), Math.max(0, cfg.bloodParticleCount), 0.3, 0.05, 0.3, 0.0);
                world.sendParticles((ParticleOptions)DownTicker.white(cfg), downed.getX(), downed.getY() + 0.2, downed.getZ(), Math.max(0, cfg.whiteParticleCount), 0.3, 0.05, 0.3, 0.0);
            }
            if ((n = (revivers = DownTicker.collectRevivers(downed, state, cfg, server)).size()) > 0) {
                state.reviveProgressTicks += n;
                boolean wasActive = state.channelActive;
                state.channelActive = true;
                for (ServerPlayer r : revivers) {
                    DownManager.markReviving(r.getUUID());
                }
                if (!wasActive) {
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.2f);
                }
                if (state.reviveProgressTicks % 4 == 0) {
                    world.sendParticles((ParticleOptions)ParticleTypes.HEART, downed.getX(), downed.getY() + 1.0, downed.getZ(), 1, 0.3, 0.2, 0.3, 0.0);
                }
                if (state.reviveProgressTicks / 10 != (state.reviveProgressTicks - n) / 10) {
                    int step = state.reviveProgressTicks / 10 % SEQ.length;
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, cfg.reviveTickVolume, SEQ[step]);
                }
                int pct = Math.min(100, (int)(100.0 * (double)state.reviveProgressTicks / (double)Math.max(1, cfg.reviveTimeTicks)));
                Object speed = n > 1 ? " x" + n : "";
                String bar = DownTicker.bar(pct);
                ClientboundSetActionBarTextPacket reviverMsg = new ClientboundSetActionBarTextPacket((Component)Component.literal((String)"Reviviendo ").withStyle(ChatFormatting.WHITE).append((Component)Component.literal((String)("[" + bar + "] " + pct + "%" + (String)speed)).withStyle(ChatFormatting.YELLOW)));
                for (ServerPlayer r : revivers) {
                    r.connection.send((Packet)reviverMsg);
                }
                downed.connection.send((Packet)new ClientboundSetActionBarTextPacket((Component)Component.literal((String)"Te est\u00e1n reviviendo ").withStyle(ChatFormatting.WHITE).append((Component)Component.literal((String)("[" + bar + "] " + pct + "%")).withStyle(ChatFormatting.GREEN))));
                if (state.reviveProgressTicks >= cfg.reviveTimeTicks) {
                    DownManager.revive(downed);
                    continue;
                }
            } else {
                if (state.channelActive) {
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4f, 0.6f);
                }
                state.channelActive = false;
                state.reviveProgressTicks = 0;
            }
            --state.remainingTicks;
            state.bossBar.setProgress(Math.max(0.0f, (float)state.remainingTicks / (float)state.totalTicks));
            if (state.remainingTicks % 20 == 0) {
                int secondsLeft = (state.remainingTicks + 19) / 20;
                state.bossBar.setName((Component)Component.literal((String)"Desangr\u00e1ndose - ").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}).append((Component)Component.literal((String)(secondsLeft + "s")).withStyle(ChatFormatting.WHITE)));
                if (!state.channelActive && state.remainingTicks > 0) {
                    int idx = state.remainingTicks / 20 % SEQ.length;
                    float pitch = SEQ[SEQ.length - 1 - idx];
                    world.playSound(null, downed.getX(), downed.getY(), downed.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, cfg.countdownTickVolume, pitch);
                }
            }
            if (state.remainingTicks > 0) continue;
            DownManager.forceDeath(downed, downed.damageSources().genericKill());
        }
        for (UUID id : toRemove) {
            DownManager.removeWithoutRevival(id);
        }
    }

    private static String bar(int pct) {
        int filled = pct / 5;
        StringBuilder b = new StringBuilder(20);
        for (int i = 0; i < 20; ++i) {
            b.append(i < filled ? (char)'|' : '.');
        }
        return b.toString();
    }

    private static List<ServerPlayer> collectRevivers(ServerPlayer downed, DownState state, ReviveConfig cfg, MinecraftServer server) {
        ArrayList<ServerPlayer> result = new ArrayList<ServerPlayer>();
        double maxSq = cfg.reviveDistance * cfg.reviveDistance;
        Iterator<Map.Entry<UUID, Integer>> it = state.reviverWindow.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            int left = e.getValue() - 1;
            if (left <= 0) {
                it.remove();
                continue;
            }
            e.setValue(left);
            ServerPlayer r = server.getPlayerList().getPlayer(e.getKey());
            if (r == null || !r.isAlive() || r.isSpectator() || DownManager.isDown(r) || r.serverLevel() != downed.serverLevel() || r.distanceToSqr((Entity)downed) > maxSq) continue;
            result.add(r);
        }
        return result;
    }
}

