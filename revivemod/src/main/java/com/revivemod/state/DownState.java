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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    /** Reviver UUID -> ticks of "click window" left. A reviver must keep
     *  right-clicking to keep their window alive; when it hits 0 they stop
     *  counting. This makes reviving require active right-clicks, not just looking. */
    public final Map<UUID, Integer> reviverWindow = new HashMap<>();
    public int reviveProgressTicks;
    public boolean channelActive;
    /** Hotbar slot to lock to. */
    public int lockedSlot;
    /** Surrender (E) / self-revive (F) channel state. */
    public boolean surrendering;
    public int surrenderTicks;
    public boolean selfReviving;
    public int selfTicks;
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
