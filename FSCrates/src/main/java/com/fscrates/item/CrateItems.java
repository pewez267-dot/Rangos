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
 * Builds and identifies the physical crate (placeable block item) and the tier
 * keys. The full {@link CrateConfig} is embedded in the crate's NBT so crates
 * are fully portable and persistent. Keys are tier items ({@link KeyItem}) and
 * carry no per-crate data — a tier key opens any crate of that tier.
 */
public final class CrateItems {

    private CrateItems() {}

    public static final String TAG_ROOT = "fscrates";
    public static final String TAG_IS_CRATE = "isCrate";
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

        Component name = Component.literal(crate.displayName.isEmpty()
                ? "\u2726 Crate " + crate.rarity.displayName() + " \u2726" : crate.displayName)
                .withStyle(crate.rarity.color());
        stack.setHoverName(name);

        if (crate.glow) {
            EnchantmentHelper.setEnchantments(java.util.Map.of(Enchantments.UNBREAKING, 1), stack);
            stack.getOrCreateTag().putInt("HideFlags", 1);
        }

        applyLore(stack,
                "\u00A77Tier: " + crate.rarity.color() + crate.rarity.displayName(),
                "\u00A77Colocala y abrela con su \u00A7ellave " + crate.rarity.color()
                        + crate.rarity.displayName() + "\u00A77.",
                crate.cooldownSeconds > 0 ? "\u00A78Cooldown: " + crate.cooldownSeconds + "s" : null);
        return stack;
    }

    // ------------------------------------------------------------------
    // Key item (tier item)
    // ------------------------------------------------------------------

    public static ItemStack buildKey(Rarity rarity) {
        return new ItemStack(ModRegistry.key(rarity));
    }

    // ------------------------------------------------------------------
    // Identification / reading
    // ------------------------------------------------------------------

    public static boolean isCrate(ItemStack stack) {
        return stack != null && stack.hasTag()
                && stack.getTag().getCompound(TAG_ROOT).getBoolean(TAG_IS_CRATE);
    }

    public static boolean isKey(ItemStack stack) {
        return stack != null && stack.getItem() instanceof KeyItem;
    }

    /** Tier of a key stack, or null if the stack is not a key. */
    public static Rarity keyRarity(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof KeyItem key) {
            return key.getRarity();
        }
        return null;
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
}
