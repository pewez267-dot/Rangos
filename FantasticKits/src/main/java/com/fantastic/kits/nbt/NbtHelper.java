package com.fantastic.kits.nbt;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pure helper around the {@link ItemStack} NBT API. Kept as a server-side
 * utility for programmatic kit assembly; the visual editor lives on the
 * client in {@link com.fantastic.kits.client.screen.NbtEditorScreen}.
 * <p>
 * Every mutator validates the raw input (sanitises identifiers, clamps levels)
 * and never throws, so a malformed admin entry simply becomes a no-op rather
 * than crashing the server.
 */
public final class NbtHelper {

    private NbtHelper() {}

    // ------------------------------------------------------------------
    // Display name / lore
    // ------------------------------------------------------------------

    public static void setDisplayName(ItemStack stack, String legacy) {
        if (stack == null || stack.isEmpty()) return;
        if (legacy == null) legacy = "";
        Component comp = legacyToComponent(legacy);
        CompoundTag display = stack.getOrCreateTagElement("display");
        display.putString("Name", Component.Serializer.toJson(comp));
    }

    public static void addLore(ItemStack stack, String line) {
        if (stack == null || stack.isEmpty() || line == null) return;
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag list = display.getList("Lore", Tag.TAG_STRING);
        list.add(StringTag.valueOf(Component.Serializer.toJson(legacyToComponent(line))));
        display.put("Lore", list);
    }

    public static void removeLastLore(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag list = display.getList("Lore", Tag.TAG_STRING);
        if (!list.isEmpty()) list.remove(list.size() - 1);
        display.put("Lore", list);
    }

    public static List<String> getLore(ItemStack stack) {
        List<String> out = new ArrayList<>();
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return out;
        CompoundTag display = stack.getTagElement("display");
        if (display == null) return out;
        ListTag list = display.getList("Lore", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    // ------------------------------------------------------------------
    // Enchantments
    // ------------------------------------------------------------------

    public static int countEnchantments(ItemStack stack) {
        if (stack == null || !stack.hasTag()) return 0;
        ListTag list = stack.getEnchantmentTags();
        return list.size();
    }

    public static void addEnchantment(ItemStack stack, String idStr, int level) {
        if (stack == null || stack.isEmpty() || idStr == null) return;
        ResourceLocation id = ResourceLocation.tryParse(idStr.trim());
        if (id == null) return;
        Enchantment ench = BuiltInRegistries.ENCHANTMENT.get(id);
        if (ench == null) return;
        int lvl = Math.max(1, Math.min(level, 255));
        stack.enchant(ench, lvl);
    }

    public static void clearEnchantments(ItemStack stack) {
        if (stack == null || !stack.hasTag()) return;
        stack.getOrCreateTag().remove("Enchantments");
        stack.getOrCreateTag().remove("StoredEnchantments");
    }

    // ------------------------------------------------------------------
    // Attribute modifiers
    // ------------------------------------------------------------------

    public static void addAttribute(ItemStack stack, String attribute, double amount) {
        if (stack == null || stack.isEmpty() || attribute == null) return;
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = tag.getList("AttributeModifiers", Tag.TAG_COMPOUND);
        CompoundTag entry = new CompoundTag();
        entry.putString("AttributeName", attribute.trim());
        entry.putString("Name", "fk_" + attribute.trim());
        entry.putDouble("Amount", amount);
        entry.putInt("Operation", 0);
        entry.putUUID("UUID", UUID.randomUUID());
        list.add(entry);
        tag.put("AttributeModifiers", list);
    }

    public static void clearAttributes(ItemStack stack) {
        if (stack == null || !stack.hasTag()) return;
        stack.getOrCreateTag().remove("AttributeModifiers");
    }

    // ------------------------------------------------------------------
    // Hide flags / Unbreakable / Durability / CustomModelData / CustomTags
    // ------------------------------------------------------------------

    public static void toggleHideFlags(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getInt("HideFlags") == 127) tag.putInt("HideFlags", 0);
        else tag.putInt("HideFlags", 127);
    }

    public static boolean isUnbreakable(ItemStack stack) {
        return stack != null && stack.hasTag() && stack.getOrCreateTag().getBoolean("Unbreakable");
    }

    public static void toggleUnbreakable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("Unbreakable", !tag.getBoolean("Unbreakable"));
    }

    public static void repairFully(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        stack.setDamageValue(0);
    }

    public static void setCustomModelData(ItemStack stack, int value) {
        if (stack == null || stack.isEmpty()) return;
        stack.getOrCreateTag().putInt("CustomModelData", value);
    }

    public static void setCustomTag(ItemStack stack, String key, String value) {
        if (stack == null || stack.isEmpty() || key == null) return;
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag custom = tag.getCompound("CustomTags");
        custom.putString(key, value == null ? "" : value);
        tag.put("CustomTags", custom);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Component legacyToComponent(String legacy) {
        // Accept '&' or section character as colour codes; default styling is non-italic
        // so the renamed name does not show italic by default.
        if (legacy == null) return Component.literal("");
        String s = legacy.replace('&', '\u00A7');
        return Component.literal(s).withStyle(style -> style.withItalic(false));
    }
}
