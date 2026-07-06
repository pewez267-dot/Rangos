package com.fscrates.item;

import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.item.EditorWandItem;
import com.fscrates.item.KeyItem;
import com.fscrates.registry.ModRegistry;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;

public final class CrateItems {
    public static final String TAG_ROOT = "fscrates";
    public static final String TAG_IS_CRATE = "isCrate";
    public static final String TAG_CRATE_ID = "crateId";
    public static final String TAG_RARITY = "rarity";
    public static final String TAG_CONFIG = "config";
    public static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    private CrateItems() {
    }

    public static ItemStack buildCrate(CrateConfig crate) {
        ItemStack stack = new ItemStack((ItemLike)ModRegistry.CRATE_ITEM.get());
        CompoundTag root = new CompoundTag();
        root.putBoolean(TAG_IS_CRATE, true);
        root.putString(TAG_CRATE_ID, crate.id);
        root.putString(TAG_RARITY, crate.rarity.name());
        root.put(TAG_CONFIG, (Tag)crate.save());
        stack.getOrCreateTag().put(TAG_ROOT, (Tag)root);
        CompoundTag beTag = new CompoundTag();
        beTag.put(TAG_CONFIG, (Tag)crate.save());
        stack.getOrCreateTag().put(BLOCK_ENTITY_TAG, (Tag)beTag);
        MutableComponent name = Component.literal((String)(crate.displayName.isEmpty() ? "\u2726 Crate " + crate.rarity.displayName() + " \u2726" : crate.displayName)).withStyle(crate.rarity.color());
        stack.setHoverName((Component)name);
        if (crate.glow) {
            EnchantmentHelper.setEnchantments(Map.of(Enchantments.UNBREAKING, 1), (ItemStack)stack);
            stack.getOrCreateTag().putInt("HideFlags", 1);
        }
        CrateItems.applyLore(stack, "\u00a77Rareza base: " + crate.rarity.color() + crate.rarity.displayName(), "\u00a77Col\u00f3cala y \u00e1brela con la \u00a7d\u2726 Fantastic Key \u2726\u00a77.", crate.cooldownSeconds > 0 ? "\u00a78Cooldown: " + crate.cooldownSeconds : null);
        return stack;
    }

    public static ItemStack buildKey() {
        return new ItemStack((ItemLike)ModRegistry.key());
    }

    // Construye la LLAVE UNICA enlazada a una crate: modelo (via CustomModelData) + crateId +
    // nombre editable. Si la crate no tiene modelo elegido, usa el primero disponible.
    public static ItemStack buildUniqueKey(CrateConfig crate) {
        KeyModels.Entry entry = KeyModels.byId(crate.uniqueKeyModel);
        if (entry == null) {
            entry = KeyModels.first();
        }
        ItemStack stack = new ItemStack((ItemLike)ModRegistry.uniqueKey());
        String name = crate.uniqueKeyName != null && !crate.uniqueKeyName.isBlank()
                ? crate.uniqueKeyName
                : (entry != null ? entry.defaultName : "\u2726 Llave de Crate \u2726");
        CompoundTag root = new CompoundTag();
        root.putString("keyModel", entry != null ? entry.id : "");
        root.putString("crateId", crate.id == null ? "" : crate.id);
        root.putString("crateName", crate.displayName == null ? "" : crate.displayName);
        root.putString("keyName", name);
        stack.getOrCreateTag().put(TAG_ROOT, (Tag)root);
        if (entry != null) {
            stack.getOrCreateTag().putInt("CustomModelData", entry.cmd);
        }
        MutableComponent hover = Component.literal((String)name.replace('&', '\u00a7'));
        stack.setHoverName((Component)hover);
        return stack;
    }

    public static boolean isUniqueKey(ItemStack stack) {
        return stack != null && stack.getItem() instanceof UniqueKeyItem;
    }

    public static String uniqueKeyCrateId(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return "";
        }
        return stack.getTag().getCompound(TAG_ROOT).getString("crateId");
    }

    public static ItemStack buildEditorWand() {
        return new ItemStack((ItemLike)ModRegistry.EDITOR_WAND.get());
    }

    public static boolean isEditorWand(ItemStack stack) {
        return stack != null && stack.getItem() instanceof EditorWandItem;
    }

    public static boolean isCrate(ItemStack stack) {
        return stack != null && stack.hasTag() && stack.getTag().getCompound(TAG_ROOT).getBoolean(TAG_IS_CRATE);
    }

    public static boolean isKey(ItemStack stack) {
        return stack != null && stack.getItem() instanceof KeyItem;
    }

    // (removido keyRarity: la Fantastic Key es universal y no tiene rareza propia)

    public static String crateId(ItemStack stack) {
        return stack != null && stack.hasTag() ? stack.getTag().getCompound(TAG_ROOT).getString(TAG_CRATE_ID) : "";
    }

    public static Rarity rarity(ItemStack stack) {
        return stack != null && stack.hasTag() ? Rarity.byName(stack.getTag().getCompound(TAG_ROOT).getString(TAG_RARITY)) : Rarity.COMMON;
    }

    public static CrateConfig readConfig(ItemStack stack) {
        if (!CrateItems.isCrate(stack)) {
            return null;
        }
        CompoundTag root = stack.getTag().getCompound(TAG_ROOT);
        return !root.contains(TAG_CONFIG) ? null : CrateConfig.load(root.getCompound(TAG_CONFIG));
    }

    private static void applyLore(ItemStack stack, String ... lines) {
        ListTag lore = new ListTag();
        for (String line : lines) {
            if (line == null) continue;
            MutableComponent c = Component.literal((String)line);
            lore.add(StringTag.valueOf((String)Component.Serializer.toJson((Component)c)));
        }
        CompoundTag display = stack.getOrCreateTagElement("display");
        display.put("Lore", (Tag)lore);
    }
}

