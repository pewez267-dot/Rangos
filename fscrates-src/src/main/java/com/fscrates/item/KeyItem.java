package com.fscrates.item;

import com.fscrates.config.Rarity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class KeyItem
extends Item {
    private final Rarity rarity;

    public KeyItem(Rarity rarity) {
        super(new Item.Properties().stacksTo(64));
        this.rarity = rarity;
    }

    public Rarity getRarity() {
        return this.rarity;
    }

    public Component getName(ItemStack stack) {
        return Component.literal((String)("\u2726 Llave " + this.rarity.displayName() + " \u2726")).withStyle(this.rarity.color());
    }

    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add((Component)Component.literal((String)"Tier: ").withStyle(ChatFormatting.GRAY).append((Component)Component.literal((String)this.rarity.displayName()).withStyle(this.rarity.color())));
        tooltip.add((Component)Component.literal((String)("Abre cualquier crate de tier " + this.rarity.displayName())).withStyle(ChatFormatting.GRAY));
        tooltip.add((Component)Component.literal((String)"Click derecho sobre la crate con la llave en la mano.").withStyle(ChatFormatting.DARK_GRAY));
    }
}

