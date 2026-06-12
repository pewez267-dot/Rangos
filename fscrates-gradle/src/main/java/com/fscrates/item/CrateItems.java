// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.item;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import com.fscrates.config.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.util.Map;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import com.fscrates.registry.ModRegistry;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import com.fscrates.config.CrateConfig;

public final class CrateItems
{
    public static final String TAG_ROOT = "fscrates";
    public static final String TAG_IS_CRATE = "isCrate";
    public static final String TAG_CRATE_ID = "crateId";
    public static final String TAG_RARITY = "rarity";
    public static final String TAG_CONFIG = "config";
    public static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
    
    private CrateItems() {
    }
    
    public static ItemStack buildCrate(final CrateConfig crate) {
        final ItemStack stack = new ItemStack((ItemLike)ModRegistry.CRATE_ITEM.get());
        final CompoundTag root = new CompoundTag();
        root.putBoolean("isCrate", true);
        root.putString("crateId", crate.id);
        root.putString("rarity", crate.rarity.name());
        root.put("config", (Tag)crate.save());
        stack.getOrCreateTag().put("fscrates", (Tag)root);
        final CompoundTag beTag = new CompoundTag();
        beTag.put("config", (Tag)crate.save());
        stack.getOrCreateTag().put("BlockEntityTag", (Tag)beTag);
        final Component name = (Component)Component.literal(crate.displayName.isEmpty() ? ("\u2726 Crate " + crate.rarity.displayName() + " \u2726") : crate.displayName).withStyle(crate.rarity.color());
        stack.setHoverName(name);
        if (crate.glow) {
            EnchantmentHelper.setEnchantments((Map)Map.of(Enchantments.UNBREAKING, 1), stack);
            stack.getOrCreateTag().putInt("HideFlags", 1);
        }
        applyLore(stack, "§7Tier: " + String.valueOf(crate.rarity.color()) + crate.rarity.displayName(), "§7Colocala y abrela con su §ellave " + String.valueOf(crate.rarity.color()) + crate.rarity.displayName() + "§7.", (crate.cooldownSeconds > 0) ? ("§8Cooldown: " + crate.cooldownSeconds) : null);
        return stack;
    }
    
    public static ItemStack buildKey(final Rarity rarity) {
        return new ItemStack((ItemLike)ModRegistry.key(rarity));
    }
    
    public static boolean isCrate(final ItemStack stack) {
        return stack != null && stack.hasTag() && stack.getTag().getCompound("fscrates").getBoolean("isCrate");
    }
    
    public static boolean isKey(final ItemStack stack) {
        return stack != null && stack.getItem() instanceof KeyItem;
    }
    
    public static Rarity keyRarity(final ItemStack stack) {
        if (stack != null) {
            final Item item0 = stack.getItem();
            if (item0 instanceof final KeyItem key) {
                return key.getRarity();
            }
        }
        return null;
    }
    
    public static String crateId(final ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return "";
        }
        return stack.getTag().getCompound("fscrates").getString("crateId");
    }
    
    public static Rarity rarity(final ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return Rarity.COMMON;
        }
        return Rarity.byName(stack.getTag().getCompound("fscrates").getString("rarity"));
    }
    
    public static CrateConfig readConfig(final ItemStack stack) {
        if (!isCrate(stack)) {
            return null;
        }
        final CompoundTag root = stack.getTag().getCompound("fscrates");
        if (!root.contains("config")) {
            return null;
        }
        return CrateConfig.load(root.getCompound("config"));
    }
    
    private static void applyLore(final ItemStack stack, final String... lines) {
        final ListTag lore = new ListTag();
        for (final String line : lines) {
            if (line != null) {
                final Component c = (Component)Component.literal(line);
                lore.add((Object)StringTag.valueOf(Component.Serializer.toJson(c)));
            }
        }
        final CompoundTag display = stack.getOrCreateTagElement("display");
        display.put("Lore", (Tag)lore);
    }
}
