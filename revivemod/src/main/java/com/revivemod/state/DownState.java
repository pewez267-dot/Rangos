package com.revivemod.state;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player downed state. Stored in DownManager keyed by player UUID so it survives
 * teleports, dimension changes, even logout/login.
 */
public class DownState {
    public final UUID playerUuid;
    public final int totalTicks;
    public int remainingTicks;
    public RegistryKey<World> downDimension;
    public Vec3d downPosition;
    public final ServerBossBar bossBar;
    /** Players who armed the revive channel by right-clicking. */
    public final Set<UUID> armedRevivers = new LinkedHashSet<>();
    public int reviveProgressTicks;
    public boolean channelActive;
    /** Hotbar slot to lock to. */
    public int lockedSlot;
    /** Sneak / sprint hold counters for surrender / self-revive (in ticks). */
    public int sneakHoldTicks;
    public int sprintHoldTicks;
    public int snapshotFood;
    public float snapshotSaturation;
    public final List<StatusEffectInstance> snapshotEffects = new ArrayList<>();

    public DownState(UUID playerUuid, int totalTicks, RegistryKey<World> dim, Vec3d pos) {
        this.playerUuid = playerUuid;
        this.totalTicks = totalTicks;
        this.remainingTicks = totalTicks;
        this.downDimension = dim;
        this.downPosition = pos;
        this.bossBar = new ServerBossBar(
                Text.literal("Noqueado"),
                BossBar.Color.RED,
                BossBar.Style.PROGRESS
        );
        this.bossBar.setDarkenSky(false);
        this.bossBar.setThickenFog(false);
        this.bossBar.setPercent(1.0f);
    }

    public Identifier bossBarId() {
        return Identifier.of("revivemod", "down_" + playerUuid);
    }
}
