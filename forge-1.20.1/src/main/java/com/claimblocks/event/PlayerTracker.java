package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;

public final class PlayerTracker {
    private static final Map<UUID, UUID> lastClaim = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastAlert = new ConcurrentHashMap<>();
    private static final long ALERT_COOLDOWN_TICKS = 600L;
    private static int bypassReminderCounter = 0;

    public static void onDisconnect(UUID id) {
        lastClaim.remove(id);
        lastAlert.remove(id);
        ClaimManager.getInstance().onPlayerDisconnect(id);
        PassiveEffectsManager.onPlayerDisconnect(id);
    }

    public static void tick(MinecraftServer server) {
        boolean showBypassReminder = ++bypassReminderCounter % 60 == 0;
        for (ServerLevel world : server.getAllLevels()) {
            for (ServerPlayer player : world.players()) {
                handle(world, player);
                if (!showBypassReminder || !player.hasPermissions(2) || !ClaimManager.getInstance().isBypassing(player.getUUID())) continue;
                player.displayClientMessage(Component.literal("[!] BYPASS ACTIVO").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            }
        }
    }

    private static void handle(ServerLevel world, ServerPlayer player) {
        Claim now = ClaimManager.getInstance().getClaimAt(world, player.blockPosition());
        UUID prev = lastClaim.get(player.getUUID());
        UUID nowId = now == null ? null : now.getClaimId();
        if (Objects.equals(prev, nowId)) {
            if (now != null && now.isBanned(player.getUUID()) && !player.hasPermissions(2)) {
                pushOutOfClaim(world, player, now);
            }
            return;
        }
        if (prev != null) {
            Claim left = findClaimById(prev);
            if (left != null) {
                MutableComponent msg = Component.literal("[Claim] ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("Saliendo de la zona de ").withStyle(ChatFormatting.RED))
                        .append(Component.literal(truncate(left.getOwnerName(), 20)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
                player.displayClientMessage(msg, true);
                player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
            }
        }
        if (now != null) {
            if (now.isBanned(player.getUUID()) && !player.hasPermissions(2)) {
                player.displayClientMessage(Component.literal("[!] Est\u00e1s baneado de esta zona.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
                pushOutOfClaim(world, player, now);
                lastClaim.remove(player.getUUID());
                return;
            }
            MutableComponent entryMsg;
            if (now.getFlags().showWelcome && now.getFlags().welcomeMessage != null && !now.getFlags().welcomeMessage.isBlank()) {
                entryMsg = Component.literal("[Claim] ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(truncate(now.getFlags().welcomeMessage, 50)).withStyle(ChatFormatting.GREEN));
            } else {
                entryMsg = Component.literal("[Claim] ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("Entrando a la zona de ").withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(truncate(now.getOwnerName(), 16)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                        .append(Component.literal(" (" + now.sizeLabel() + ")").withStyle(ChatFormatting.GRAY));
            }
            player.displayClientMessage(entryMsg, true);
            player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
            if (now.getFlags().trespasserAlerts && !now.canModify(player)) {
                long t = world.getGameTime();
                Long last = lastAlert.get(player.getUUID());
                if (last == null || t - last > ALERT_COOLDOWN_TICKS) {
                    lastAlert.put(player.getUUID(), t);
                    ServerPlayer owner = world.getServer().getPlayerList().getPlayer(now.getOwnerUUID());
                    if (owner != null) {
                        MutableComponent alert = Component.literal("[!] ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                                .append(Component.literal(truncate(player.getName().getString(), 16)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                                .append(Component.literal(" entr\u00f3 a tu zona en X=" + now.getX() + " Z=" + now.getZ()).withStyle(ChatFormatting.YELLOW));
                        owner.displayClientMessage(alert, false);
                    }
                }
            }
        }
        if (nowId == null) {
            lastClaim.remove(player.getUUID());
        } else {
            lastClaim.put(player.getUUID(), nowId);
        }
    }

    private static Claim findClaimById(UUID id) {
        for (Claim c : ClaimManager.getInstance().getAllClaims()) {
            if (c.getClaimId().equals(id)) return c;
        }
        return null;
    }

    private static void pushOutOfClaim(ServerLevel world, ServerPlayer player, Claim claim) {
        int r = claim.getRadius() + 2;
        double dx = player.getX() - claim.getX();
        double dz = player.getZ() - claim.getZ();
        double mag = Math.max(1.0E-4, Math.sqrt(dx * dx + dz * dz));
        double tx = claim.getX() + 0.5 + dx / mag * r;
        double tz = claim.getZ() + 0.5 + dz / mag * r;
        int ix = (int) Math.floor(tx);
        int iz = (int) Math.floor(tz);
        world.getChunk(ix >> 4, iz >> 4);
        int safeY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);
        int minY = world.getMinBuildHeight();
        int maxY = world.getMaxBuildHeight();
        if (safeY <= minY) safeY = (int) player.getY();
        if (safeY >= maxY) safeY = maxY - 2;
        player.teleportTo(tx, safeY, tz);
        player.setDeltaMovement(0, 0, 0);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }
}
