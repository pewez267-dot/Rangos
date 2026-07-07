/*
 * Decompiled with CFR 0.152.
 */
package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.event.PassiveEffectsManager;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class PlayerTracker {
    private static final Map<UUID, UUID> lastClaim = new ConcurrentHashMap<UUID, UUID>();
    private static final Map<UUID, Long> lastAlert = new ConcurrentHashMap<UUID, Long>();
    private static final long ALERT_COOLDOWN_TICKS = 600L;
    private static int bypassReminderCounter = 0;
    private static final Map<UUID, Long> lastBanHit = new ConcurrentHashMap<UUID, Long>();

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
                PlayerTracker.handle(world, player);
                if (!showBypassReminder || !player.hasPermissions(2) || !ClaimManager.getInstance().isBypassing(player.getUUID())) continue;
                player.displayClientMessage((Component)Component.literal((String)"[!] BYPASS ACTIVO").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}), true);
            }
        }
    }

    private static void handle(ServerLevel world, ServerPlayer player) {
        Claim now = ClaimManager.getInstance().getClaimAt((Level)world, player.blockPosition());
        UUID prev = lastClaim.get(player.getUUID());
        UUID nowId = now == null ? null : PlayerTracker.zoneId(now);
        if (Objects.equals(prev, nowId)) {
            // Misma zona (o mismo grupo): NO repetir mensajes de entrada.
            if (now != null && now.isBanned(player.getUUID()) && !player.hasPermissions(2)) {
                PlayerTracker.repelBanned(world, player, now);
            }
            return;
        }
        // Salio de la zona anterior
        Claim left = PlayerTracker.resolveZone(prev);
        if (left != null) {
            MutableComponent msg = left.getFlags().showLeave && left.getFlags().leaveMessage != null && !left.getFlags().leaveMessage.isBlank() ? Component.literal((String)"[Protecci\u00f3n] ").withStyle(ChatFormatting.GRAY).append((Component)Component.literal((String)PlayerTracker.truncate(left.getFlags().leaveMessage, 50)).withStyle(ChatFormatting.GOLD)) : Component.literal((String)"[Protecci\u00f3n] ").withStyle(ChatFormatting.GRAY).append((Component)Component.literal((String)"Saliendo de la zona ").withStyle(ChatFormatting.RED)).append((Component)Component.literal((String)PlayerTracker.truncate(PlayerTracker.zoneLabel(left), 24)).withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD}));
            player.displayClientMessage((Component)msg, true);
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0f, 0.9f);
        }
        // Entro a la zona nueva
        if (now != null) {
            if (now.isBanned(player.getUUID()) && !player.hasPermissions(2)) {
                PlayerTracker.repelBanned(world, player, now);
                lastClaim.remove(player.getUUID());
                return;
            }
            MutableComponent entryMsg = now.getFlags().showWelcome && now.getFlags().welcomeMessage != null && !now.getFlags().welcomeMessage.isBlank() ? Component.literal((String)"[Protecci\u00f3n] ").withStyle(ChatFormatting.GRAY).append((Component)Component.literal((String)PlayerTracker.truncate(now.getFlags().welcomeMessage, 50)).withStyle(ChatFormatting.GREEN)) : Component.literal((String)"[Protecci\u00f3n] ").withStyle(ChatFormatting.GRAY).append((Component)Component.literal((String)"Entrando a la zona ").withStyle(ChatFormatting.GREEN)).append((Component)Component.literal((String)PlayerTracker.truncate(PlayerTracker.zoneLabel(now), 24)).withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD}));
            player.displayClientMessage((Component)entryMsg, true);
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0f, 1.4f);
            if (now.getFlags().trespasserAlerts && !now.canModify((Player)player)) {
                long t = world.getGameTime();
                Long last = lastAlert.get(player.getUUID());
                if (last == null || t - last > 600L) {
                    lastAlert.put(player.getUUID(), t);
                    UUID ownerId = PlayerTracker.zoneOwner(now);
                    ServerPlayer owner = ownerId == null ? null : world.getServer().getPlayerList().getPlayer(ownerId);
                    if (owner != null) {
                        MutableComponent alert = Component.literal((String)"[!] ").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}).append((Component)Component.literal((String)PlayerTracker.truncate(player.getName().getString(), 16)).withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD})).append((Component)Component.literal((String)(" entr\u00f3 a tu zona en X=" + now.getX() + " Z=" + now.getZ())).withStyle(ChatFormatting.YELLOW));
                        owner.displayClientMessage((Component)alert, false);
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

    // Id de "zona" efectiva: el grupo si esta agrupada; si no, la claim.
    private static UUID zoneId(Claim c) {
        return c.getGroupId() != null ? c.getGroupId() : c.getClaimId();
    }

    // Resuelve un id de zona (grupo o claim) a una claim representativa.
    private static Claim resolveZone(UUID zoneId) {
        if (zoneId == null) {
            return null;
        }
        com.claimblocks.data.ClaimGroup g = ClaimManager.getInstance().getGroup(zoneId);
        if (g != null) {
            return ClaimManager.getInstance().getMotherClaim(zoneId);
        }
        return PlayerTracker.findClaimById(zoneId);
    }

    // Nombre a mostrar de la zona: nombre del grupo si esta agrupada; si no, "dueno (tamano)".
    private static String zoneLabel(Claim c) {
        if (c == null) {
            return "";
        }
        if (c.getGroupId() != null) {
            com.claimblocks.data.ClaimGroup g = ClaimManager.getInstance().getGroup(c.getGroupId());
            if (g != null) {
                return g.getName();
            }
        }
        return c.getOwnerName() + " (" + c.sizeLabel() + ")";
    }

    private static UUID zoneOwner(Claim c) {
        if (c.getGroupId() != null) {
            com.claimblocks.data.ClaimGroup g = ClaimManager.getInstance().getGroup(c.getGroupId());
            if (g != null && g.getMotherOwnerId() != null) {
                return g.getMotherOwnerId();
            }
        }
        return c.getOwnerUUID();
    }

    private static Claim findClaimById(UUID id) {
        for (Claim c : ClaimManager.getInstance().getAllClaims()) {
            if (!c.getClaimId().equals(id)) continue;
            return c;
        }
        return null;
    }

    private static void repelBanned(ServerLevel world, ServerPlayer player, Claim claim) {
        double cx = (double)claim.getX() + 0.5;
        double cz = (double)claim.getZ() + 0.5;
        double dx = player.getX() - cx;
        double dz = player.getZ() - cz;
        double mag = Math.max(1.0E-4, Math.sqrt(dx * dx + dz * dz));
        double dirX = dx / mag;
        double dirZ = dz / mag;
        player.setDeltaMovement(dirX * 1.5, 0.42, dirZ * 1.5);
        player.hurtMarked = true;
        player.hasImpulse = true;
        long now = world.getGameTime();
        Long last = lastBanHit.get(player.getUUID());
        if (last == null || now - last >= 15L) {
            lastBanHit.put(player.getUUID(), now);
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().magic(), 5.0f);
            player.displayClientMessage((Component)Component.literal((String)"[!] Est\u00e1s baneado de esta zona.").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}), false);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "...";
    }
}

