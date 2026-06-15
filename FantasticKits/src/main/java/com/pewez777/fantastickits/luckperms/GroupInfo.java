/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.luckperms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, dependency-free description of a LuckPerms group.
 *
 * <p>This DTO contains <strong>no</strong> reference to any {@code net.luckperms.api}
 * type, so it can be created, transported and read regardless of whether
 * LuckPerms is installed. It is the only group representation used by the rest
 * of the mod.</p>
 */
public final class GroupInfo {

    private final String name;
    private final String displayName;
    private final int weight;
    private final List<String> inheritedGroups;
    private final List<String> permissions;

    public GroupInfo(String name, String displayName, int weight,
                     List<String> inheritedGroups, List<String> permissions) {
        this.name = Objects.requireNonNull(name, "name");
        this.displayName = (displayName == null || displayName.isEmpty()) ? name : displayName;
        this.weight = weight;
        this.inheritedGroups = Collections.unmodifiableList(
                new ArrayList<>(inheritedGroups == null ? List.of() : inheritedGroups));
        this.permissions = Collections.unmodifiableList(
                new ArrayList<>(permissions == null ? List.of() : permissions));
    }

    /** Internal (canonical) group name as stored by LuckPerms. */
    public String getName() {
        return name;
    }

    /** Friendly display name (falls back to {@link #getName()}). */
    public String getDisplayName() {
        return displayName;
    }

    /** Group weight (defaults to 0 when LuckPerms reports none). */
    public int getWeight() {
        return weight;
    }

    /** Names of groups this group directly inherits from. */
    public List<String> getInheritedGroups() {
        return inheritedGroups;
    }

    /** Permission node strings directly held by this group. */
    public List<String> getPermissions() {
        return permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupInfo other)) {
            return false;
        }
        return name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return "GroupInfo{name='" + name + "', weight=" + weight + "}";
    }
}
