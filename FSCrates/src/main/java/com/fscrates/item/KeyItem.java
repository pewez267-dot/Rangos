package com.fscrates.item;

import com.fscrates.config.Rarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A tier key. There is exactly one key item per {@link Rarity}; a key opens ANY
 * crate whose tier matches the key's tier. Keys are never crate-specific, so an
 * admin can hand out, say, "Legendary keys" that open every legendary crate.
 *
 * <p>The key carries its own professional look: a tier colour, a tier name and a
 * descriptive tooltip, plus an enchant glint.
 */
public class KeyItem extends Item {

    private final Rarity rarity;

    public KeyItem(Rarity rarity) {
        super(new Properties().stacksTo(64));
        this.rarity = rarity;
    }

    public Rarity getRarity() {
        return rarity;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("\u2726 Llave " + rarity.displayName() + " \u2726")
                .withStyle(rarity.color());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // always glints, like an enchanted key
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(rarity.displayName()).withStyle(rarity.color())));
        tooltip.add(Component.literal("Abre cualquier crate de tier " + rarity.displayName() + ".")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Click derecho sobre la crate con la llave en la mano.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
