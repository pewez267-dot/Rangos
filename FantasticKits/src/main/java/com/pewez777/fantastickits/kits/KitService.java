/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.kits;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;
import com.pewez777.fantastickits.config.FantasticKitsConfig;
import com.pewez777.fantastickits.luckperms.LuckPermsHook;
import com.pewez777.fantastickits.security.AuditAction;
import com.pewez777.fantastickits.security.AuditLogger;
import com.pewez777.fantastickits.security.NetworkAddressUtil;
import com.pewez777.fantastickits.security.SecurityEventLogger;
import com.pewez777.fantastickits.security.SecurityEventType;
import com.pewez777.fantastickits.storage.PlayerKitData;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import org.slf4j.Logger;

/**
 * Core, fully server-side service for claiming and testing kits.
 *
 * <p>All gating rules live here: exact primary-group matching, single permanent
 * claim, anti-exploit cooldown and race-condition protection. Every outcome is
 * audited, and every blocked attempt also raises a security event.</p>
 */
public final class KitService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Per-player timestamp of the last claim attempt (anti double-click/macro). */
    private static final ConcurrentHashMap<UUID, Long> LAST_CLAIM_ATTEMPT = new ConcurrentHashMap<>();

    private KitService() {
    }

    /** The outcome of a claim attempt. */
    public enum ClaimResult {
        SUCCESS,
        ALREADY_CLAIMED,
        WRONG_GROUP,
        NO_LUCKPERMS,
        COOLDOWN,
        ERROR
    }

    /**
     * Checks whether the player's PRIMARY group matches the kit's owner group
     * EXACTLY. Inheritance, weights and hierarchies are never considered.
     */
    public static boolean primaryGroupMatchesExactly(UUID playerId, String ownerGroup) {
        if (ownerGroup == null || ownerGroup.isBlank()) {
            return false;
        }
        Optional<String> primary = LuckPermsHook.getPrimaryGroup(playerId);
        return primary.isPresent() && primary.get().equalsIgnoreCase(ownerGroup);
    }

    /**
     * Attempts to claim a kit for a player. This is the authoritative entry
     * point invoked after all packet/permission validation.
     */
    public static ClaimResult claim(ServerPlayer player, Kit kit) {
        if (player == null || kit == null) {
            return ClaimResult.ERROR;
        }
        final UUID id = player.getUUID();
        final String name = player.getGameProfile().getName();
        final String ip = NetworkAddressUtil.getIp(player);
        final String ownerGroup = kit.getOwnerGroup();

        // Anti-exploit: enforce a minimum interval between attempts.
        long now = System.currentTimeMillis();
        long cooldown = FantasticKitsConfig.CLAIM_COOLDOWN_MILLIS.get();
        Long last = LAST_CLAIM_ATTEMPT.put(id, now);
        if (last != null && (now - last) < cooldown) {
            SecurityEventLogger.log(SecurityEventType.REPEATED_REQUEST_SPAM, id, name,
                    "-", ownerGroup, kit.getName(), "claim",
                    "BLOCKED", "Claim attempt faster than cooldown (" + cooldown + "ms)");
            return ClaimResult.COOLDOWN;
        }

        PlayerKitData data = KitManager.get().players().get(id);
        // Synchronize on the per-player data object (same cached instance) to
        // serialize concurrent claim attempts and prevent race-condition dupes.
        synchronized (data) {
            data.setLastKnownName(name);

            boolean singleClaim = kit.isSingleClaim() || FantasticKitsConfig.SINGLE_PERMANENT_CLAIM.get();
            if (singleClaim && data.hasClaimed(kit.getId())) {
                AuditLogger.log(AuditAction.CLAIM_DENIED, id, name, ip, kit.getName(), ownerGroup,
                        "DENIED", "Already claimed previously");
                SecurityEventLogger.log(SecurityEventType.DUPLICATE_CLAIM_ATTEMPT, id, name,
                        "-", ownerGroup, kit.getName(), "claim",
                        "BLOCKED", "Permanent single-claim already used");
                return ClaimResult.ALREADY_CLAIMED;
            }

            if (!LuckPermsHook.isAvailable()) {
                AuditLogger.log(AuditAction.CLAIM_DENIED, id, name, ip, kit.getName(), ownerGroup,
                        "DENIED", "LuckPerms unavailable - cannot verify rank");
                SecurityEventLogger.log(SecurityEventType.INVALID_GROUP_ACCESS, id, name,
                        "(unknown)", ownerGroup, kit.getName(), "claim",
                        "BLOCKED", "LuckPerms not present, rank cannot be verified");
                return ClaimResult.NO_LUCKPERMS;
            }

            String detected = LuckPermsHook.getPrimaryGroup(id).orElse("(none)");
            if (!primaryGroupMatchesExactly(id, ownerGroup)) {
                AuditLogger.log(AuditAction.CLAIM_DENIED, id, name, ip, kit.getName(), ownerGroup,
                        "DENIED", "Primary group '" + detected + "' != required '" + ownerGroup + "'");
                SecurityEventLogger.log(SecurityEventType.INVALID_GROUP_ACCESS, id, name,
                        detected, ownerGroup, kit.getName(), "claim",
                        "BLOCKED", "Primary group does not match exactly");
                return ClaimResult.WRONG_GROUP;
            }

            // Eligible: deliver contents and run associated commands.
            try {
                giveContents(player, kit);
                runCommands(player, kit);
            } catch (Throwable t) {
                LOGGER.error("[F-Kits] Error while delivering kit '{}' to {}", kit.getName(), name, t);
                AuditLogger.log(AuditAction.CLAIM_DENIED, id, name, ip, kit.getName(), ownerGroup,
                        "FAILURE", "Delivery error: " + t.getClass().getSimpleName());
                return ClaimResult.ERROR;
            }

            // Persist the permanent claim and history.
            data.markClaimed(kit.getId());
            data.addHistory(new PlayerKitData.HistoryEntry(
                    kit.getId(), kit.getName(), ownerGroup, now, "CLAIM_KIT", "SUCCESS"));
            KitManager.get().players().save(data);

            AuditLogger.log(AuditAction.CLAIM_KIT, id, name, ip, kit.getName(), ownerGroup,
                    "SUCCESS", "Kit claimed (permanent)");
            return ClaimResult.SUCCESS;
        }
    }

    /**
     * Delivers a kit to an administrator for testing. Does NOT register a claim,
     * does NOT consume a use and does NOT alter statistics.
     */
    public static void test(ServerPlayer player, Kit kit) {
        if (player == null || kit == null) {
            return;
        }
        String name = player.getGameProfile().getName();
        String ip = NetworkAddressUtil.getIp(player);
        try {
            giveContents(player, kit);
            runCommands(player, kit);
            AuditLogger.log(AuditAction.TEST_KIT, player.getUUID(), name, ip, kit.getName(),
                    kit.getOwnerGroup(), "SUCCESS", "Test delivery (no claim recorded)");
        } catch (Throwable t) {
            LOGGER.error("[F-Kits] Error during test of kit '{}'", kit.getName(), t);
            AuditLogger.log(AuditAction.TEST_KIT, player.getUUID(), name, ip, kit.getName(),
                    kit.getOwnerGroup(), "FAILURE", "Test delivery error");
        }
    }

    private static void giveContents(ServerPlayer player, Kit kit) {
        for (ItemStack stack : kit.getItems()) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy)) {
                // Inventory full: drop the remainder at the player's feet so no
                // items are ever lost (anti-loss safety).
                player.drop(copy, false);
            }
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static void runCommands(ServerPlayer player, Kit kit) {
        if (kit.getCommands().isEmpty()) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        String name = player.getGameProfile().getName();
        CommandSourceStack source = player.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
        for (String raw : kit.getCommands()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String command = raw.replace("{player}", name).replace("%player%", name);
            try {
                server.getCommands().performPrefixedCommand(source, command);
            } catch (Throwable t) {
                LOGGER.warn("[F-Kits] Failed to run kit command '{}' for {}", command, name, t);
            }
        }
    }
}
