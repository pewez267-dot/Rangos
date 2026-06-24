package com.fantastickits.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for serializing/deserializing ItemStacks to/from CompoundTag (NBT).
 * Handles all required attributes: name, lore, enchantments, attributes, custom model data,
 * unbreakable flag, item flags, and arbitrary NBT.
 */
public final class NBTSerializer {

    private NBTSerializer() {}

    /**
     * Serialize an ItemStack to a CompoundTag containing all data.
     * This is the complete representation stored in kits.json.
     */
    public static CompoundTag serializeItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        return stack.save(new CompoundTag());
    }

    /**
     * Deserialize a CompoundTag back into an ItemStack.
     */
    public static ItemStack deserializeItemStack(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(tag);
    }

    /**
     * Set a custom display name on an item using raw JSON text component format.
     */
    public static void setDisplayName(ItemStack stack, String jsonName) {
        CompoundTag display = stack.getOrCreateTagElement("display");
        display.putString("Name", jsonName);
    }

    /**
     * Get the custom display name JSON string from an item.
     */
    public static String getDisplayName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("display", Tag.TAG_COMPOUND)) {
            CompoundTag display = tag.getCompound("display");
            if (display.contains("Name", Tag.TAG_STRING)) {
                return display.getString("Name");
            }
        }
        return "";
    }

    /**
     * Set lore lines on an item. Each line should be a JSON text component string.
     */
    public static void setLore(ItemStack stack, List<String> loreLines) {
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag loreTag = new ListTag();
        for (String line : loreLines) {
            loreTag.add(StringTag.valueOf(line));
        }
        display.put("Lore", loreTag);
    }

    /**
     * Get all lore lines as JSON text component strings.
     */
    public static List<String> getLore(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("display", Tag.TAG_COMPOUND)) {
            CompoundTag display = tag.getCompound("display");
            if (display.contains("Lore", Tag.TAG_LIST)) {
                ListTag loreTag = display.getList("Lore", Tag.TAG_STRING);
                for (int i = 0; i < loreTag.size(); i++) {
                    lines.add(loreTag.getString(i));
                }
            }
        }
        return lines;
    }

    /**
     * Set custom model data on an item.
     */
    public static void setCustomModelData(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt("CustomModelData", value);
    }

    /**
     * Get custom model data from an item.
     */
    public static int getCustomModelData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("CustomModelData", Tag.TAG_INT)) {
            return tag.getInt("CustomModelData");
        }
        return 0;
    }

    /**
     * Set the unbreakable flag on an item.
     */
    public static void setUnbreakable(ItemStack stack, boolean unbreakable) {
        stack.getOrCreateTag().putBoolean("Unbreakable", unbreakable);
    }

    /**
     * Check if an item has the unbreakable flag.
     */
    public static boolean isUnbreakable(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean("Unbreakable");
    }

    /**
     * Set item flags (HideFlags bitmask).
     * Bit 0 = Enchantments, 1 = AttributeModifiers, 2 = Unbreakable,
     * 3 = CanDestroy, 4 = CanPlaceOn, 5 = Other (potion effects, etc.)
     */
    public static void setHideFlags(ItemStack stack, int flags) {
        stack.getOrCreateTag().putInt("HideFlags", flags);
    }

    /**
     * Get item flags bitmask.
     */
    public static int getHideFlags(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("HideFlags", Tag.TAG_INT)) {
            return tag.getInt("HideFlags");
        }
        return 0;
    }

    /**
     * Add an attribute modifier to an item.
     */
    public static void addAttributeModifier(ItemStack stack, String attributeName,
                                            String modifierName, double amount,
                                            AttributeModifier.Operation operation,
                                            String slot) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag modifiers;
        if (tag.contains("AttributeModifiers", Tag.TAG_LIST)) {
            modifiers = tag.getList("AttributeModifiers", Tag.TAG_COMPOUND);
        } else {
            modifiers = new ListTag();
        }

        CompoundTag modifier = new CompoundTag();
        modifier.putString("AttributeName", attributeName);
        modifier.putString("Name", modifierName);
        modifier.putDouble("Amount", amount);
        modifier.putInt("Operation", operation.toValue());
        modifier.putString("Slot", slot);
        // Generate a unique UUID for this modifier
        UUID uuid = UUID.randomUUID();
        modifier.putIntArray("UUID", new int[]{
                (int) (uuid.getMostSignificantBits() >> 32),
                (int) uuid.getMostSignificantBits(),
                (int) (uuid.getLeastSignificantBits() >> 32),
                (int) uuid.getLeastSignificantBits()
        });

        modifiers.add(modifier);
        tag.put("AttributeModifiers", modifiers);
    }

    /**
     * Get attribute modifiers from an item as a list of CompoundTags.
     */
    public static List<CompoundTag> getAttributeModifiers(ItemStack stack) {
        List<CompoundTag> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("AttributeModifiers", Tag.TAG_LIST)) {
            ListTag modifiers = tag.getList("AttributeModifiers", Tag.TAG_COMPOUND);
            for (int i = 0; i < modifiers.size(); i++) {
                result.add(modifiers.getCompound(i));
            }
        }
        return result;
    }

    /**
     * Remove all attribute modifiers from an item.
     */
    public static void clearAttributeModifiers(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove("AttributeModifiers");
        }
    }

    /**
     * Set arbitrary NBT data on an item (merges with existing tag).
     */
    public static void mergeNbt(ItemStack stack, CompoundTag additional) {
        CompoundTag existing = stack.getOrCreateTag();
        existing.merge(additional);
    }

    /**
     * Get the full NBT tag of an item (for inspection/editing).
     */
    public static CompoundTag getFullNbt(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.copy() : new CompoundTag();
    }
}
