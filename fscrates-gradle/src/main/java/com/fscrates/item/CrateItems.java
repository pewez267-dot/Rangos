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
        root.m_128379_("isCrate", true);
        root.m_128359_("crateId", crate.id);
        root.m_128359_("rarity", crate.rarity.name());
        root.m_128365_("config", (Tag)crate.save());
        stack.m_41784_().m_128365_("fscrates", (Tag)root);
        final CompoundTag beTag = new CompoundTag();
        beTag.m_128365_("config", (Tag)crate.save());
        stack.m_41784_().m_128365_("BlockEntityTag", (Tag)beTag);
        final Component name = (Component)Component.m_237113_(crate.displayName.isEmpty() ? ("\u2726 Crate " + crate.rarity.displayName() + " \u2726") : crate.displayName).m_130940_(crate.rarity.color());
        stack.m_41714_(name);
        if (crate.glow) {
            EnchantmentHelper.m_44865_((Map)Map.of(Enchantments.f_44986_, 1), stack);
            stack.m_41784_().m_128405_("HideFlags", 1);
        }
        applyLore(stack, "§7Tier: " + String.valueOf(crate.rarity.color()) + crate.rarity.displayName(), "§7Colocala y abrela con su §ellave " + String.valueOf(crate.rarity.color()) + crate.rarity.displayName() + "§7.", (crate.cooldownSeconds > 0) ? ("§8Cooldown: " + crate.cooldownSeconds) : null);
        return stack;
    }
    
    public static ItemStack buildKey(final Rarity rarity) {
        return new ItemStack((ItemLike)ModRegistry.key(rarity));
    }
    
    public static boolean isCrate(final ItemStack stack) {
        return stack != null && stack.m_41782_() && stack.m_41783_().m_128469_("fscrates").m_128471_("isCrate");
    }
    
    public static boolean isKey(final ItemStack stack) {
        return stack != null && stack.m_41720_() instanceof KeyItem;
    }
    
    public static Rarity keyRarity(final ItemStack stack) {
        if (stack != null) {
            final Item 41720_ = stack.m_41720_();
            if (41720_ instanceof final KeyItem key) {
                return key.getRarity();
            }
        }
        return null;
    }
    
    public static String crateId(final ItemStack stack) {
        if (stack == null || !stack.m_41782_()) {
            return "";
        }
        return stack.m_41783_().m_128469_("fscrates").m_128461_("crateId");
    }
    
    public static Rarity rarity(final ItemStack stack) {
        if (stack == null || !stack.m_41782_()) {
            return Rarity.COMMON;
        }
        return Rarity.byName(stack.m_41783_().m_128469_("fscrates").m_128461_("rarity"));
    }
    
    public static CrateConfig readConfig(final ItemStack stack) {
        if (!isCrate(stack)) {
            return null;
        }
        final CompoundTag root = stack.m_41783_().m_128469_("fscrates");
        if (!root.m_128441_("config")) {
            return null;
        }
        return CrateConfig.load(root.m_128469_("config"));
    }
    
    private static void applyLore(final ItemStack stack, final String... lines) {
        final ListTag lore = new ListTag();
        for (final String line : lines) {
            if (line != null) {
                final Component c = (Component)Component.m_237113_(line);
                lore.add((Object)StringTag.m_129297_(Component.Serializer.m_130703_(c)));
            }
        }
        final CompoundTag display = stack.m_41698_("display");
        display.m_128365_("Lore", (Tag)lore);
    }
}
