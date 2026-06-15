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

/**
 * Abstraction over the LuckPerms API.
 *
 * <p><strong>Soft-dependency contract:</strong> this interface intentionally
 * contains <em>no</em> {@code net.luckperms.api.*} types. The single concrete
 * implementation, {@link LuckPermsBridgeImpl}, is the only class in the whole
 * project allowed to import LuckPerms types, and it is loaded by reflection only
 * after LuckPerms has been confirmed present. This guarantees the JVM never
 * tries to resolve LuckPerms classes when the plugin is absent, eliminating the
 * {@code NoClassDefFoundError} class-loading crash.</p>
 */
public interface LuckPermsBridge {

    /**
     * @return every loaded group as dependency-free {@link GroupInfo} DTOs.
     */
    List<GroupInfo> getAllGroups();

    /**
     * @param playerId the player's UUID
     * @return the player's exact PRIMARY group name, if resolvable.
     */
    Optional<String> getPrimaryGroup(UUID playerId);

    /**
     * Publishes the group -&gt; kit -&gt; commands relationship to LuckPerms as
     * permission nodes, fully automatically.
     *
     * @param groupName owner group of the kit
     * @param kitId     internal kit identifier
     * @param commands  commands owned by the kit (without leading slash)
     */
    void publishKitNodes(String groupName, String kitId, List<String> commands);

    /**
     * Revokes every node previously published for the given kit on the given
     * group.
     *
     * @param groupName owner group of the kit
     * @param kitId     internal kit identifier
     */
    void revokeKitNodes(String groupName, String kitId);
}
