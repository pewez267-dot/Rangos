package com.pewez.fantasticessentials.util;

import com.pewez.fantasticessentials.config.Config;
import com.pewez.fantasticessentials.text.Messages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Handles teleport warmups (delayed teleports that can be cancelled by movement or damage)
 * and per-player command cooldowns.
 */
public final class TeleportManager {

    private static final Map<UUID, Warmup> WARMUPS = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();

    private TeleportManager() {
    }

    private static final class Warmup {
        final ServerPlayer player;
        final Runnable action;
        final Vec3 startPos;
        final double startHealth;
        int ticksLeft;

        Warmup(ServerPlayer player, Runnable action, int ticksLeft) {
            this.player = player;
            this.action = action;
            this.ticksLeft = ticksLeft;
            this.startPos = player.position();
            this.startHealth = player.getHealth();
        }
    }

    /**
     * Schedule a teleport with the configured warmup. If the warmup is 0 (or the player
     * bypasses it) the action runs immediately.
     */
    public static void scheduleTeleport(ServerPlayer player, Runnable action) {
        int seconds = Config.get().teleportWarmupSeconds;
        boolean bypass = player.hasPermissions(Config.get().warmupBypassLevel);
        if (seconds <= 0 || bypass) {
            action.run();
            return;
        }
        WARMUPS.put(player.getUUID(), new Warmup(player, action, seconds * 20));
        player.sendSystemMessage(Messages.prefixed("teleport.warmup",
                "&7Teleporting in &e{seconds}&7 seconds. Don't move!",
                Messages.of("seconds", String.valueOf(seconds))));
    }

    public static void cancel(UUID uuid) {
        WARMUPS.remove(uuid);
    }

    public static void onDamage(ServerPlayer player) {
        if (!Config.get().cancelWarmupOnDamage) {
            return;
        }
        if (WARMUPS.containsKey(player.getUUID())) {
            WARMUPS.remove(player.getUUID());
            player.sendSystemMessage(Messages.prefixed("teleport.cancelled.damage",
                    "&cTeleport cancelled because you took damage."));
        }
    }

    public static void tick() {
        if (WARMUPS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Warmup>> it = WARMUPS.entrySet().iterator();
        while (it.hasNext()) {
            Warmup warmup = it.next().getValue();
            ServerPlayer player = warmup.player;
            if (player.isRemoved() || !player.isAlive()) {
                it.remove();
                continue;
            }
            if (Config.get().cancelWarmupOnMove) {
                Vec3 pos = player.position();
                if (Math.abs(pos.x - warmup.startPos.x) > 0.2
                        || Math.abs(pos.y - warmup.startPos.y) > 0.2
                        || Math.abs(pos.z - warmup.startPos.z) > 0.2) {
                    it.remove();
                    player.sendSystemMessage(Messages.prefixed("teleport.cancelled.move",
                            "&cTeleport cancelled because you moved."));
                    continue;
                }
            }
            if (Config.get().cancelWarmupOnDamage && player.getHealth() < warmup.startHealth) {
                it.remove();
                player.sendSystemMessage(Messages.prefixed("teleport.cancelled.damage",
                        "&cTeleport cancelled because you took damage."));
                continue;
            }
            warmup.ticksLeft--;
            if (warmup.ticksLeft <= 0) {
                it.remove();
                warmup.action.run();
            }
        }
    }

    // ----- Cooldowns -----

    /**
     * @return remaining cooldown in seconds, or 0 if ready.
     */
    public static int remainingCooldown(ServerPlayer player, String key) {
        Map<String, Long> map = COOLDOWNS.get(player.getUUID());
        if (map == null) {
            return 0;
        }
        Long expiry = map.get(key);
        if (expiry == null) {
            return 0;
        }
        long remaining = expiry - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (int) Math.ceil(remaining / 1000.0);
    }

    public static void setCooldown(ServerPlayer player, String key, int seconds) {
        if (seconds <= 0 || player.hasPermissions(Config.get().cooldownBypassLevel)) {
            return;
        }
        COOLDOWNS.computeIfAbsent(player.getUUID(), u -> new HashMap<>())
                .put(key, System.currentTimeMillis() + seconds * 1000L);
    }

    public static void clear(UUID uuid) {
        WARMUPS.remove(uuid);
        COOLDOWNS.remove(uuid);
    }
}
