package com.revivemod.state;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player downed state. Holds the countdown, the boss bar, the revive window of nearby
 * revivers and a snapshot of the player's status to restore on revive.
 */
public class DownState {
    public final UUID playerUuid;
    public final int totalTicks;
    public int remainingTicks;
    public ResourceKey<Level> downDimension;
    public Vec3 downPosition;
    public final ServerBossEvent bossBar;
    public final Map<UUID, Integer> reviverWindow = new HashMap<>();
    public int reviveProgressTicks;
    public boolean channelActive;
    public int lockedSlot;
    public boolean surrendering;
    public int surrenderTicks;
    public boolean selfReviving;
    public int selfTicks;
    public int snapshotFood;
    public float snapshotSaturation;
    public final List<MobEffectInstance> snapshotEffects = new ArrayList<>();

    public DownState(UUID playerUuid, int totalTicks, ResourceKey<Level> dim, Vec3 pos) {
        this.playerUuid = playerUuid;
        this.totalTicks = totalTicks;
        this.remainingTicks = totalTicks;
        this.downDimension = dim;
        this.downPosition = pos;
        this.bossBar = new ServerBossEvent(Component.literal("Noqueado"),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        this.bossBar.setDarkenScreen(false);
        this.bossBar.setCreateWorldFog(false);
        this.bossBar.setProgress(1.0f);
    }

    public ResourceLocation bossBarId() {
        return new ResourceLocation("revivemod", "down_" + this.playerUuid);
    }
}
