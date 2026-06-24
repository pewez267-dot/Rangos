package com.fantastic.kits.luckperms;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of a LuckPerms group as understood by Fantastic Kits.
 * <p>
 * The mod treats this as a read-only DTO; mutating LuckPerms state is done
 * exclusively through {@link LuckPermsHook} using the official API.
 */
public final class GroupInfo {

    private final String name;
    private final String displayName;
    private final int weight;
    private final List<String> inheritance;
    private final List<String> permissions;

    public GroupInfo(String name, String displayName, int weight,
                     List<String> inheritance, List<String> permissions) {
        this.name = Objects.requireNonNull(name);
        this.displayName = displayName == null || displayName.isBlank() ? name : displayName;
        this.weight = weight;
        this.inheritance = inheritance == null ? Collections.emptyList() : List.copyOf(inheritance);
        this.permissions = permissions == null ? Collections.emptyList() : List.copyOf(permissions);
    }

    public String name() { return name; }
    public String displayName() { return displayName; }
    public int weight() { return weight; }
    public List<String> inheritance() { return inheritance; }
    public List<String> permissions() { return permissions; }

    @Override
    public String toString() {
        return "GroupInfo[" + name + ", weight=" + weight + ", inherits=" + inheritance + "]";
    }
}
