package com.fantasticwatch.util;

import com.fantasticwatch.config.WatchConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Reads and writes the invisible Fantastic Watch tracking mark on item NBT.
 *
 * <p>The mark lives in a dedicated compound under the configured namespace (default
 * {@code fantasticwatch}) inside the item's root tag:</p>
 * <pre>
 * { fantasticwatch: { tracked:1b, item_uid:"fw-...", spawned_by:"&lt;uuid&gt;",
 *                     spawned_by_name:"...", spawned_at:"&lt;iso&gt;", spawned_pos:"x,y,z",
 *                     spawned_dim:"...", current_owner:"&lt;uuid&gt;", transfer_count:0 } }
 * </pre>
 *
 * <p><b>Invisibility &amp; persistence:</b> vanilla tooltips only render a fixed set of tags
 * (display, Enchantments, etc.), so a custom compound is never shown to the player. Because the
 * mark is part of the item's standard NBT it is copied by {@link ItemStack#copy()}, written to
 * disk with chunks and containers, and therefore survives drops, transfers, container storage,
 * server restarts, and any third-party mod that preserves item NBT (the overwhelming majority,
 * since copying a stack copies its tag).</p>
 */
public final class NbtUtil {

    public static final String KEY_TRACKED = "tracked";
    public static final String KEY_UID = "item_uid";
    public static final String KEY_SPAWNED_BY = "spawned_by";
    public static final String KEY_SPAWNED_BY_NAME = "spawned_by_name";
    public static final String KEY_SPAWNED_AT = "spawned_at";
    public static final String KEY_SPAWNED_POS = "spawned_pos";
    public static final String KEY_SPAWNED_DIM = "spawned_dim";
    public static final String KEY_CURRENT_OWNER = "current_owner";
    public static final String KEY_TRANSFER_COUNT = "transfer_count";

    private NbtUtil() {
    }

    /** Immutable snapshot of a mark's data, used when logging without repeatedly poking NBT. */
    public record MarkData(String uid, UUID spawnedBy, String spawnedByName, String spawnedAt,
                           String spawnedPos, String spawnedDim, UUID currentOwner, int transferCount) {
    }

    public static String namespace() {
        return WatchConfig.TAG_NAMESPACE.get();
    }

    /** @return {@code true} if the stack carries a valid tracking mark. */
    public static boolean isTracked(ItemStack stack) {
        CompoundTag mark = getMark(stack);
        return mark != null && mark.getBoolean(KEY_TRACKED);
    }

    /** @return the mark compound, or {@code null} if the stack is untracked. */
    public static CompoundTag getMark(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CompoundTag root = stack.getTag();
        String ns = namespace();
        if (root == null || !root.contains(ns, Tag.TAG_COMPOUND)) {
            return null;
        }
        return root.getCompound(ns);
    }

    /**
     * Applies a fresh tracking mark to the stack. The {@code current_owner} starts equal to the
     * spawning operator and {@code transfer_count} starts at zero.
     */
    public static void writeMark(ItemStack stack, String uid, UUID spawnedBy, String spawnedByName,
                                 String spawnedAt, String spawnedPos, String spawnedDim) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CompoundTag mark = new CompoundTag();
        mark.putBoolean(KEY_TRACKED, true);
        mark.putString(KEY_UID, uid);
        mark.putString(KEY_SPAWNED_BY, spawnedBy.toString());
        mark.putString(KEY_SPAWNED_BY_NAME, spawnedByName);
        mark.putString(KEY_SPAWNED_AT, spawnedAt);
        mark.putString(KEY_SPAWNED_POS, spawnedPos);
        mark.putString(KEY_SPAWNED_DIM, spawnedDim);
        mark.putString(KEY_CURRENT_OWNER, spawnedBy.toString());
        mark.putInt(KEY_TRANSFER_COUNT, 0);
        stack.getOrCreateTag().put(namespace(), mark);
    }

    public static String getUid(ItemStack stack) {
        CompoundTag mark = getMark(stack);
        return mark != null ? mark.getString(KEY_UID) : null;
    }

    public static String getSpawnedByName(ItemStack stack) {
        CompoundTag mark = getMark(stack);
        return mark != null ? mark.getString(KEY_SPAWNED_BY_NAME) : "unknown";
    }

    public static String getSpawnedAt(ItemStack stack) {
        CompoundTag mark = getMark(stack);
        return mark != null ? mark.getString(KEY_SPAWNED_AT) : null;
    }

    /** @return the spawning operator's UUID, or {@code null} when absent/unparseable. */
    public static UUID getSpawnedByUuid(ItemStack stack) {
        CompoundTag mark = getMark(stack);
        if (mark == null) {
            return null;
        }
        return parseUuid(mark.getString(KEY_SPAWNED_BY));
    }

    public static UUID getCurrentOwner(ItemStack stack) {
        CompoundTag mark = getMark(stack);
        if (mark == null) {
            return null;
        }
        return parseUuid(mark.getString(KEY_CURRENT_OWNER));
    }

    public static int getTransferCount(ItemStack stack) {
        CompoundTag mark = getMark(stack);
        return mark != null ? mark.getInt(KEY_TRANSFER_COUNT) : 0;
    }

    /** Updates the current owner. No-op if the stack is untracked. */
    public static void setCurrentOwner(ItemStack stack, UUID newOwner) {
        CompoundTag mark = getMark(stack);
        if (mark != null) {
            mark.putString(KEY_CURRENT_OWNER, newOwner.toString());
        }
    }

    /**
     * Increments and returns the transfer count.
     *
     * @return the new transfer count, or {@code 0} if the stack is untracked
     */
    public static int incrementTransferCount(ItemStack stack) {
        CompoundTag mark = getMark(stack);
        if (mark == null) {
            return 0;
        }
        int next = mark.getInt(KEY_TRANSFER_COUNT) + 1;
        mark.putInt(KEY_TRANSFER_COUNT, next);
        return next;
    }

    /** Removes the mark entirely, clearing the root tag too if it becomes empty. */
    public static void removeMark(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CompoundTag root = stack.getTag();
        if (root == null) {
            return;
        }
        root.remove(namespace());
        if (root.isEmpty()) {
            stack.setTag(null);
        }
    }

    /** Builds an immutable {@link MarkData} snapshot from a tracked stack, or {@code null}. */
    public static MarkData toMarkData(ItemStack stack) {
        CompoundTag mark = getMark(stack);
        if (mark == null) {
            return null;
        }
        return new MarkData(
                mark.getString(KEY_UID),
                parseUuid(mark.getString(KEY_SPAWNED_BY)),
                mark.getString(KEY_SPAWNED_BY_NAME),
                mark.getString(KEY_SPAWNED_AT),
                mark.getString(KEY_SPAWNED_POS),
                mark.getString(KEY_SPAWNED_DIM),
                parseUuid(mark.getString(KEY_CURRENT_OWNER)),
                mark.getInt(KEY_TRANSFER_COUNT));
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
