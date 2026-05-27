package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-tick tracker that detects when a player crosses a claim boundary, then:
 *   - Sends an action-bar message + small note-block sound
 *   - Honours the {@code showWelcome} flag (prints owner's custom welcome)
 *   - Honours the {@code trespasserAlerts} flag (DMs the owner)
 *   - Pushes banned players back outside
 */
public final class PlayerTracker {
    private static final Map<UUID, UUID> lastClaim = new HashMap<>();
    private static final Map<UUID, Long> lastAlert = new HashMap<>();
    private static final long ALERT_COOLDOWN_TICKS = 600;

    public static void register() { /* tick driven from main */ }

    public static void tick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                handle(world, player);
            }
        }
    }

    private static void handle(ServerWorld world, ServerPlayerEntity player) {
        Claim now = ClaimManager.getInstance().getClaimAt(world, player.getBlockPos());
        UUID prev = lastClaim.get(player.getUuid());
        UUID nowId = now == null ? null : now.getClaimId();

        if (java.util.Objects.equals(prev, nowId)) {
            if (now != null && now.isBanned(player.getUuid()) && !player.hasPermissionLevel(2)) {
                pushOutOfClaim(player, now);
            }
            return;
        }

        if (prev != null) {
            Claim left = findClaimById(prev);
            if (left != null) {
                Text msg = Text.literal("[Claim] ").formatted(Formatting.GRAY)
                    .append(Text.literal("Saliendo de la zona de ").formatted(Formatting.RED))
                    .append(Text.literal(truncate(left.getOwnerName(), 20))
                        .formatted(Formatting.WHITE, Formatting.BOLD));
                player.sendMessage(msg, true);
                player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                    SoundCategory.PLAYERS, 0.5f, 1.0f);
            }
        }
        if (now != null) {
            if (now.isBanned(player.getUuid()) && !player.hasPermissionLevel(2)) {
                player.sendMessage(Text.literal("[!] Estás baneado de esta zona.")
                    .formatted(Formatting.RED, Formatting.BOLD), false);
                pushOutOfClaim(player, now);
                lastClaim.remove(player.getUuid());
                return;
            }

            Text entryMsg;
            if (now.getFlags().showWelcome
                && now.getFlags().welcomeMessage != null
                && !now.getFlags().welcomeMessage.isBlank()) {
                entryMsg = Text.literal("[Claim] ").formatted(Formatting.GRAY)
                    .append(Text.literal(truncate(now.getFlags().welcomeMessage, 50))
                        .formatted(Formatting.GREEN));
            } else {
                entryMsg = Text.literal("[Claim] ").formatted(Formatting.GRAY)
                    .append(Text.literal("Entrando a la zona de ").formatted(Formatting.GREEN))
                    .append(Text.literal(truncate(now.getOwnerName(), 16))
                        .formatted(Formatting.WHITE, Formatting.BOLD))
                    .append(Text.literal(" (" + now.sizeLabel() + ")").formatted(Formatting.GRAY));
            }
            player.sendMessage(entryMsg, true);
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),
                SoundCategory.PLAYERS, 0.5f, 1.0f);

            if (now.getFlags().trespasserAlerts && !now.canModify(player)) {
                long t = world.getServer().getTicks();
                Long last = lastAlert.get(player.getUuid());
                if (last == null || t - last > ALERT_COOLDOWN_TICKS) {
                    lastAlert.put(player.getUuid(), t);
                    ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(now.getOwnerUUID());
                    if (owner != null) {
                        Text alert = Text.literal("[!] ").formatted(Formatting.RED, Formatting.BOLD)
                            .append(Text.literal(truncate(player.getName().getString(), 16))
                                .formatted(Formatting.WHITE, Formatting.BOLD))
                            .append(Text.literal(" entró a tu zona en X=" + now.getX()
                                + " Z=" + now.getZ()).formatted(Formatting.YELLOW));
                        owner.sendMessage(alert, false);
                    }
                }
            }
        }
        lastClaim.put(player.getUuid(), nowId);
    }

    private static Claim findClaimById(UUID id) {
        for (Claim c : ClaimManager.getInstance().getAllClaims()) {
            if (c.getClaimId().equals(id)) return c;
        }
        return null;
    }

    private static void pushOutOfClaim(ServerPlayerEntity player, Claim claim) {
        int r = claim.getRadius() + 2;
        Vec3d cur = player.getPos();
        double dx = cur.x - claim.getX();
        double dz = cur.z - claim.getZ();
        double mag = Math.max(0.0001, Math.sqrt(dx * dx + dz * dz));
        double tx = claim.getX() + 0.5 + (dx / mag) * r;
        double tz = claim.getZ() + 0.5 + (dz / mag) * r;
        BlockPos targetPos = BlockPos.ofFloored(tx, player.getY(), tz);
        player.requestTeleport(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }
}
