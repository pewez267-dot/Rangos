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
import java.util.List;
import java.util.UUID;

/**
 * Per-player downed state. Stored in DownManager keyed by player UUID so it survives
 * teleports, dimension changes, even logout/login.
 */
public class DownState {
    public final UUID playerUuid;
    /** Total ticks the player should remain down before truly dying. */
    public final int totalTicks;
    /** Ticks remaining before death. */
    public int remainingTicks;
    /** Where the player went down. */
    public RegistryKey<World> downDimension;
    public Vec3d downPosition;
    /** Bossbar shown to the downed player and any nearby reviver. */
    public final ServerBossBar bossBar;
    /** Active reviver UUID (if any) and current channel progress. */
    public UUID reviverUuid;
    public int reviveProgressTicks;
    /** Snapshot of the food / saturation level the player had when going down. */
    public int snapshotFood;
    public float snapshotSaturation;
    /** Snapshot of pre-existing potion effects so revive doesn't strip them. */
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
