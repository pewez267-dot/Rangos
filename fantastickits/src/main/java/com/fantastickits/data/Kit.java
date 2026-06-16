package com.fantastickits.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory representation of a single kit.
 *
 * <p>A kit is identified by its lowercase {@link #id}, shows a coloured
 * {@link #displayName}, is gated behind exactly one LuckPerms {@link #group}, and
 * delivers a fixed list of fully-NBT-configured {@link #items}.</p>
 *
 * <p>The set of server commands a member of {@link #group} may use is stored
 * separately in {@link GroupCommandStore} (keyed by group, not by kit), because a
 * single group can back several kits and the command grant is a property of the
 * rank, not of the kit.</p>
 */
public final class Kit {

    public String id;
    public String displayName;
    /** Exactly one LuckPerms group name, or empty string when unassigned. */
    public String group;
    public final List<ItemStack> items;

    public Kit() {
        this("kit");
    }

    public Kit(final String id) {
        this.id = normalizeId(id);
        this.displayName = this.id;
        this.group = "";
        this.items = new ArrayList<>();
    }

    public static String normalizeId(final String raw) {
        if (raw == null || raw.isBlank()) {
            return "kit";
        }
        return raw.trim().toLowerCase().replace(' ', '_');
    }

    /** Serialises this kit to NBT (used for network transfer and as the canonical form). */
    public CompoundTag toNbt() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("id", this.id == null ? "" : this.id);
        tag.putString("displayName", this.displayName == null ? "" : this.displayName);
        tag.putString("group", this.group == null ? "" : this.group);
        final ListTag list = new ListTag();
        for (final ItemStack stack : this.items) {
            if (stack != null && !stack.isEmpty()) {
                list.add(stack.save(new CompoundTag()));
            }
        }
        tag.put("items", list);
        return tag;
    }

    public static Kit fromNbt(final CompoundTag tag) {
        final Kit kit = new Kit(tag.getString("id"));
        kit.displayName = tag.contains("displayName") ? tag.getString("displayName") : kit.id;
        kit.group = tag.getString("group");
        kit.items.clear();
        final ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) {
                kit.items.add(stack);
            }
        }
        return kit;
    }

    /** Deep copy (item stacks are cloned) so client and server never share mutable state. */
    public Kit copy() {
        return fromNbt(this.toNbt());
    }

    public boolean hasGroup() {
        return this.group != null && !this.group.isBlank();
    }
}
