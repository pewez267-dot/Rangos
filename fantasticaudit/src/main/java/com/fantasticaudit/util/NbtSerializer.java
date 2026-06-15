package com.fantasticaudit.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Serialises Minecraft NBT into a compact, single-line SNBT string suitable for a log file.
 *
 * <p>Only vanilla {@link CompoundTag} facilities are used (no external NBT libraries). The
 * {@link CompoundTag#toString()} representation is already the canonical compact SNBT form,
 * which round-trips through {@code TagParser} if ever needed for forensic re-parsing.</p>
 */
public final class NbtSerializer {

    private NbtSerializer() {
    }

    /**
     * @param tag the tag to serialise; may be {@code null}
     * @return compact SNBT, or {@code {}} when the tag is {@code null} or empty
     */
    public static String serialize(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return "{}";
        }
        return tag.toString();
    }

    /**
     * Serialises the full NBT of an item stack, including modded attribute tags.
     *
     * @param stack the stack whose tag should be captured; may be {@code null}/empty
     * @return compact SNBT of the stack tag, or {@code {}} when there is none
     */
    public static String serializeStackTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "{}";
        }
        return serialize(stack.getTag());
    }
}
