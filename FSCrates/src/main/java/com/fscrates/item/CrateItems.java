package com.fscrates.item;

import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Builds and identifies the physical crate (placeable block item) and key
 * (plain item) ItemStacks. Crates use the registered {@code fscrates:crate}
 * BlockItem so they place a real crate block in the world; keys use the
 * registered {@code fscrates:key} item so they never place anything.
 *
 * <p>All data needed to open a crate (the full {@link CrateConfig}) is embedded
 * in the crate's NBT, so crates are fully portable and persistent.
 */
public final class CrateItems {

    private CrateItems() {}

    public static final String TAG_ROOT = "fscrates";
    public static final String TAG_IS_CRATE = "isCrate";
    public static final String TAG_IS_KEY = "isKey";
    public static final String TAG_CRATE_ID = "crateId";
    public static final String TAG_RARITY = "rarity";
    public static final String TAG_CONFIG = "config";
    /** Vanilla key Forge copies into the placed BlockEntity. */
    public static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    // ------------------------------------------------------------------
    // Crate item (placeable block item)
    // ------------------------------------------------------------------

    public static ItemStack buildCrate(CrateConfig crate) {
        ItemStack stack = new ItemStack(ModRegistry.CRATE_ITEM.get());
        CompoundTag root = new CompoundTag();
        root.putBoolean(TAG_IS_CRATE, true);
        root.putString(TAG_CRATE_ID, crate.id);
        root.putString(TAG_RARITY, crate.rarity.name());
        root.put(TAG_CONFIG, crate.save());
        stack.getOrCreateTag().put(TAG_ROOT, root);

        // When placed, Forge copies BlockEntityTag into the CrateBlockEntity.
        CompoundTag beTag = new CompoundTag();
        beTag.put("config", crate.save());
        stack.getOrCreateTag().put(BLOCK_ENTITY_TAG, beTag);

        stack.getOrCreateTag().putInt("CustomModelData", crate.rarity.crateModelData());

        Component name = Component.literal(crate.displayName.isEmpty()
                ? "\u2726 Crate " + crate.rarity.displayName() + " \u2726" : crate.displayName)
                .withStyle(crate.rarity.color());
        stack.setHoverName(name);

        if (crate.glow) {
            EnchantmentHelper.setEnchantments(java.util.Map.of(Enchantments.UNBREAKING, 1), stack);
            stack.getOrCreateTag().putInt("HideFlags", 1);
        }

        applyLore(stack,
                "\u00A77Rareza: " + crate.rarity.color() + crate.rarity.displayName(),
                "\u00A77Col\u00f3cala y usa su \u00A7ellave\u00A77 con clic derecho.",
                crate.cooldownSeconds > 0 ? "\u00A78Cooldown: " + crate.cooldownSeconds + "s" : null);
        return stack;
    }

    // ------------------------------------------------------------------
    // Key item (plain item)
    // ------------------------------------------------------------------

    public static ItemStack buildKey(CrateConfig crate, Rarity rarity) {
        ItemStack stack = new ItemStack(ModRegistry.KEY_ITEM.get());
        CompoundTag root = new CompoundTag();
        root.putBoolean(TAG_IS_KEY, true);
        root.putString(TAG_CRATE_ID, crate.id);
        root.putString(TAG_RARITY, rarity.name());
        stack.getOrCreateTag().put(TAG_ROOT, root);

        stack.getOrCreateTag().putInt("CustomModelData", rarity.keyModelData());

        Component name = Component.literal(crate.keyName.isEmpty()
                ? "\u2726 Llave " + rarity.displayName() + " \u2726" : crate.keyName)
                .withStyle(rarity.color());
        stack.setHoverName(name);

        if (crate.keyGlint) {
            EnchantmentHelper.setEnchantments(java.util.Map.of(Enchantments.UNBREAKING, 1), stack);
            stack.getOrCreateTag().putInt("HideFlags", 1);
        }

        applyLore(stack,
                "\u00A77Llave de: " + crate.rarity.color() + crate.displayName,
                crate.keyLore.isEmpty() ? null : "\u00A78" + crate.keyLore);
        return stack;
    }

    private static void applyLore(ItemStack stack, String... lines) {
        ListTag lore = new ListTag();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            Component c = Component.literal(line);
            lore.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        }
        CompoundTag display = stack.getOrCreateTagElement("display");
        display.put("Lore", lore);
    }

    // ------------------------------------------------------------------
    // Identification / reading
    // ------------------------------------------------------------------

    public static boolean isCrate(ItemStack stack) {
        return stack != null && stack.hasTag()
                && stack.getTag().getCompound(TAG_ROOT).getBoolean(TAG_IS_CRATE);
    }

    public static boolean isKey(ItemStack stack) {
        return stack != null && stack.hasTag()
                && stack.getTag().getCompound(TAG_ROOT).getBoolean(TAG_IS_KEY);
    }

    public static String crateId(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return "";
        }
        return stack.getTag().getCompound(TAG_ROOT).getString(TAG_CRATE_ID);
    }

    public static Rarity rarity(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return Rarity.COMMON;
        }
        return Rarity.byName(stack.getTag().getCompound(TAG_ROOT).getString(TAG_RARITY));
    }

    /** Reads the embedded crate config from a crate ItemStack, or null. */
    public static CrateConfig readConfig(ItemStack stack) {
        if (!isCrate(stack)) {
            return null;
        }
        CompoundTag root = stack.getTag().getCompound(TAG_ROOT);
        if (!root.contains(TAG_CONFIG)) {
            return null;
        }
        return CrateConfig.load(root.getCompound(TAG_CONFIG));
    }
}
