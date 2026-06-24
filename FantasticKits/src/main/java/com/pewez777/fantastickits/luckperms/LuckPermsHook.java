/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.luckperms;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

/**
 * Soft-dependency entry point for LuckPerms.
 *
 * <p>This class contains <strong>no</strong> {@code net.luckperms.api.*} import.
 * It detects LuckPerms purely via {@link Class#forName(String)} and, only when
 * present, instantiates {@link LuckPermsBridgeImpl} reflectively. If LuckPerms
 * is missing, {@code available} stays {@code false}, a clear warning is logged,
 * and the mod keeps loading normally. Every delegating method is fully guarded
 * so a runtime failure inside the bridge can never crash the server.</p>
 */
public final class LuckPermsHook {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String PROVIDER_CLASS = "net.luckperms.api.LuckPermsProvider";
    private static final String IMPL_CLASS = "com.pewez777.fantastickits.luckperms.LuckPermsBridgeImpl";

    private static volatile boolean available = false;
    private static volatile LuckPermsBridge bridge = null;

    private LuckPermsHook() {
    }

    /**
     * Detects LuckPerms and wires up the bridge. Safe to call multiple times;
     * never throws.
     */
    public static synchronized void initialize() {
        available = false;
        bridge = null;

        // Step 1: detect LuckPerms without touching any LuckPerms symbol.
        try {
            Class.forName(PROVIDER_CLASS);
        } catch (Throwable t) {
            LOGGER.warn("[F-Kits] LuckPerms was not detected. Rank-aware features "
                    + "(group ownership, strict claiming, command gating) are disabled. "
                    + "The mod will keep running normally.");
            return;
        }

        // Step 2: LuckPerms exists - instantiate the isolated bridge reflectively.
        try {
            Class<?> implClass = Class.forName(IMPL_CLASS);
            Object instance = implClass.getDeclaredConstructor().newInstance();
            bridge = (LuckPermsBridge) instance;
            available = true;
            LOGGER.info("[F-Kits] LuckPerms detected and integration enabled.");
        } catch (Throwable t) {
            available = false;
            bridge = null;
            LOGGER.warn("[F-Kits] LuckPerms was detected but the integration bridge "
                    + "failed to initialize. Rank-aware features are disabled.", t);
        }
    }

    /** @return {@code true} only when LuckPerms is present and the bridge is live. */
    public static boolean isAvailable() {
        return available && bridge != null;
    }

    /** @return all loaded groups, or an empty list when LuckPerms is unavailable. */
    public static List<GroupInfo> getAllGroups() {
        if (!isAvailable()) {
            return List.of();
        }
        try {
            return bridge.getAllGroups();
        } catch (Throwable t) {
            LOGGER.warn("[F-Kits] Failed to read LuckPerms groups.", t);
            return List.of();
        }
    }

    /** @return the player's exact primary group, or empty when unavailable. */
    public static Optional<String> getPrimaryGroup(UUID playerId) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            return bridge.getPrimaryGroup(playerId);
        } catch (Throwable t) {
            LOGGER.warn("[F-Kits] Failed to resolve primary group for {}.", playerId, t);
            return Optional.empty();
        }
    }

    /** Publishes group -&gt; kit -&gt; command nodes; no-op when unavailable. */
    public static void publishKitNodes(String groupName, String kitId, List<String> commands) {
        if (!isAvailable()) {
            return;
        }
        try {
            bridge.publishKitNodes(groupName, kitId, commands);
        } catch (Throwable t) {
            LOGGER.warn("[F-Kits] Failed to publish LuckPerms nodes for kit {}.", kitId, t);
        }
    }

    /** Revokes nodes previously published for a kit; no-op when unavailable. */
    public static void revokeKitNodes(String groupName, String kitId) {
        if (!isAvailable()) {
            return;
        }
        try {
            bridge.revokeKitNodes(groupName, kitId);
        } catch (Throwable t) {
            LOGGER.warn("[F-Kits] Failed to revoke LuckPerms nodes for kit {}.", kitId, t);
        }
    }
}
