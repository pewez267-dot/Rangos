/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.nbt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Server-safe utility that applies the visual NBT editor operations to an
 * {@link ItemStack}. Every method here is invoked from validated, server-side
 * code; the client GUI merely collects the desired values.
 *
 * <p>No client-only types are referenced, so this class is safe on a dedicated
 * server. All editing is performed through real 1.20.1 item NBT structures
 * (the {@code display}, {@code Enchantments}, {@code AttributeModifiers},
 * {@code HideFlags}, {@code Unbreakable}, {@code Damage} and
 * {@code CustomModelData} tags).</p>
 */
public final class NbtEditorService {

    // Vanilla HideFlags bit constants.
    public static final int HIDE_ENCHANTMENTS = 1;
    public static final int HIDE_ATTRIBUTES = 1 << 1;
    public static final int HIDE_UNBREAKABLE = 1 << 2;
    public static final int HIDE_CAN_DESTROY = 1 << 3;
    public static final int HIDE_CAN_PLACE = 1 << 4;
    public static final int HIDE_ADDITIONAL = 1 << 5;
    public static final int HIDE_DYE = 1 << 6;

    private static final String DISPLAY = "display";
    private static final String NAME = "Name";
    private static final String LORE = "Lore";
    private static final String CUSTOM = "FKitsCustom";

    private NbtEditorService() {
    }

    /** Translates {@code &}-style color codes to the section sign. */
    public static String colorize(String raw) {
        if (raw == null) {
            return "";
        }
        char[] chars = raw.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(chars[i + 1]) > -1) {
                chars[i] = '\u00a7';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    // ---- Display name ------------------------------------------------------

    public static void setDisplayName(ItemStack stack, String raw) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (raw == null || raw.isEmpty()) {
            clearDisplayName(stack);
            return;
        }
        Component component = Component.literal(colorize(raw));
        CompoundTag display = stack.getOrCreateTagElement(DISPLAY);
        display.putString(NAME, Component.Serializer.toJson(component));
    }

    public static void clearDisplayName(ItemStack stack) {
        if (stack == null) {
            return;
        }
        CompoundTag display = stack.getTagElement(DISPLAY);
        if (display != null) {
            display.remove(NAME);
        }
    }

    // ---- Lore --------------------------------------------------------------

    public static void setLore(ItemStack stack, List<String> lines) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CompoundTag display = stack.getOrCreateTagElement(DISPLAY);
        if (lines == null || lines.isEmpty()) {
            display.remove(LORE);
            return;
        }
        ListTag loreTag = new ListTag();
        for (String line : lines) {
            Component component = Component.literal(colorize(line == null ? "" : line));
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(component)));
        }
        display.put(LORE, loreTag);
    }

    public static List<String> getLoreRaw(ItemStack stack) {
        List<String> result = new ArrayList<>();
        if (stack == null) {
            return result;
        }
        CompoundTag display = stack.getTagElement(DISPLAY);
        if (display == null || !display.contains(LORE, Tag.TAG_LIST)) {
            return result;
        }
        ListTag loreTag = display.getList(LORE, Tag.TAG_STRING);
        for (int i = 0; i < loreTag.size(); i++) {
            Component component = Component.Serializer.fromJson(loreTag.getString(i));
            result.add(component == null ? "" : component.getString());
        }
        return result;
    }

    // ---- Enchantments ------------------------------------------------------

    public static void setEnchantment(ItemStack stack, Enchantment enchantment, int level) {
        if (stack == null || stack.isEmpty() || enchantment == null) {
            return;
        }
        Map<Enchantment, Integer> current = new LinkedHashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (level <= 0) {
            current.remove(enchantment);
        } else {
            current.put(enchantment, level);
        }
        EnchantmentHelper.setEnchantments(current, stack);
    }

    public static void removeEnchantment(ItemStack stack, Enchantment enchantment) {
        setEnchantment(stack, enchantment, 0);
    }

    public static void clearEnchantments(ItemStack stack) {
        if (stack == null) {
            return;
        }
        EnchantmentHelper.setEnchantments(new LinkedHashMap<>(), stack);
    }

    // ---- Attributes --------------------------------------------------------

    public static void addAttributeModifier(ItemStack stack, Attribute attribute, String name,
                                            double amount, AttributeModifier.Operation operation,
                                            EquipmentSlot slot) {
        if (stack == null || stack.isEmpty() || attribute == null || operation == null) {
            return;
        }
        AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(),
                name == null ? "fkits_modifier" : name, amount, operation);
        stack.addAttributeModifier(attribute, modifier, slot);
    }

    public static void clearAttributeModifiers(ItemStack stack) {
        if (stack == null) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove("AttributeModifiers");
        }
    }

    // ---- Flags / durability / model data -----------------------------------

    public static void setHideFlags(ItemStack stack, int flags) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putInt("HideFlags", flags);
    }

    public static int getHideFlags(ItemStack stack) {
        if (stack == null) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt("HideFlags");
    }

    public static void toggleHideFlag(ItemStack stack, int flagBit, boolean enabled) {
        int flags = getHideFlags(stack);
        if (enabled) {
            flags |= flagBit;
        } else {
            flags &= ~flagBit;
        }
        setHideFlags(stack, flags);
    }

    public static void setUnbreakable(ItemStack stack, boolean unbreakable) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (unbreakable) {
            stack.getOrCreateTag().putBoolean("Unbreakable", true);
        } else {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove("Unbreakable");
            }
        }
    }

    public static boolean isUnbreakable(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean("Unbreakable");
    }

    public static void setDamage(ItemStack stack, int damage) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.setDamageValue(Math.max(0, damage));
    }

    public static void setCustomModelData(ItemStack stack, int value) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (value <= 0) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove("CustomModelData");
            }
        } else {
            stack.getOrCreateTag().putInt("CustomModelData", value);
        }
    }

    public static int getCustomModelData(ItemStack stack) {
        if (stack == null) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt("CustomModelData");
    }

    // ---- Custom tags -------------------------------------------------------

    public static void putCustomTag(ItemStack stack, String key, String value) {
        if (stack == null || stack.isEmpty() || key == null || key.isEmpty()) {
            return;
        }
        CompoundTag custom = stack.getOrCreateTag().getCompound(CUSTOM);
        custom.putString(key, value == null ? "" : value);
        stack.getOrCreateTag().put(CUSTOM, custom);
    }

    public static void removeCustomTag(ItemStack stack, String key) {
        if (stack == null || key == null) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(CUSTOM, Tag.TAG_COMPOUND)) {
            CompoundTag custom = tag.getCompound(CUSTOM);
            custom.remove(key);
            if (custom.isEmpty()) {
                tag.remove(CUSTOM);
            } else {
                tag.put(CUSTOM, custom);
            }
        }
    }
}
