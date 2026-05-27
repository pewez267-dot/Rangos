package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks when a player crosses a claim boundary, and:
 *   - notifies the owner if TRESPASSER_ALERTS is on (cooldown 30s per player)
 *   - emits a thin particle outline of the claim for the entering player
 */
public class ClaimEntryTracker {
    /** player uuid -> center pos hash of current claim (or "") */
    private static final Map<UUID, String> currentClaim = new HashMap<>();
    /** player uuid -> last alert tick (cooldown) */
    private static final Map<UUID, Long> lastAlertTick = new HashMap<>();

    private static final int ALERT_COOLDOWN_TICKS = 600; // 30s
    private static final int VISUALIZATION_INTERVAL = 20; // 1s
    private static int tick = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tick++;
            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    handlePlayer(world, player);
                }
            }
        });
    }

    private static void handlePlayer(ServerWorld world, ServerPlayerEntity player) {
        Claim claim = ClaimManager.getInstance().getClaimAt(world, player.getBlockPos());
        UUID id = player.getUuid();
        String currentKey = claim == null ? "" : (claim.getDimension() + ":" + claim.getCenter().asLong());
        String prev = currentClaim.getOrDefault(id, "");

        if (!prev.equals(currentKey)) {
            currentClaim.put(id, currentKey);
            if (claim != null && !claim.canModify(player)) {
                // Player just entered a claim they don't own
                player.sendMessage(Text.literal("§e[Claim] §fYou entered §a"
                    + claim.getOwnerName() + "§f's claim (Tier " + claim.getTier() + ")"), true);
                if (claim.getFlags().isTrespasserAlerts()) {
                    notifyOwner(world, claim, player);
                }
            }
        }

        // Periodic visualisation: send a few particles around the boundary
        if (claim != null && tick % VISUALIZATION_INTERVAL == 0) {
            spawnBorderParticles(world, claim);
        }
    }

    private static void notifyOwner(ServerWorld world, Claim claim, ServerPlayerEntity intruder) {
        long now = world.getServer().getTicks();
        Long last = lastAlertTick.get(intruder.getUuid());
        if (last != null && now - last < ALERT_COOLDOWN_TICKS) return;
        lastAlertTick.put(intruder.getUuid(), now);

        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(claim.getOwnerId());
        if (owner != null) {
            owner.sendMessage(Text.literal("§c[Claim Alert] §f" + intruder.getName().getString()
                + " entered your claim at " + claim.getCenter().toShortString()), false);
        }
    }

    private static void spawnBorderParticles(ServerWorld world, Claim claim) {
        Box box = claim.getBoundingBox();
        // 12 edges of the cube, sample a few points each
        int samples = Math.max(6, Math.min(20, claim.getRadius() / 2));
        for (int i = 0; i <= samples; i++) {
            double t = (double) i / samples;
            // bottom-y edges
            spawnLine(world, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, t);
            spawnLine(world, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, t);
            spawnLine(world, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, t);
            spawnLine(world, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, t);
            // top-y edges
            spawnLine(world, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, t);
            spawnLine(world, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, t);
            spawnLine(world, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, t);
            spawnLine(world, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, t);
            // verticals
            spawnLine(world, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, t);
            spawnLine(world, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, t);
            spawnLine(world, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, t);
            spawnLine(world, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, t);
        }
    }

    private static void spawnLine(ServerWorld world, double x1, double y1, double z1,
                                  double x2, double y2, double z2, double t) {
        double x = x1 + (x2 - x1) * t;
        double y = y1 + (y2 - y1) * t;
        double z = z1 + (z2 - z1) * t;
        world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
    }
}
