// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.item;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.fscrates.config.Rarity;
import net.minecraft.world.item.Item;

public class KeyItem extends Item
{
    private final Rarity rarity;
    
    public KeyItem(final Rarity rarity) {
        super(new Item.Properties().stacksTo(64));
        this.rarity = rarity;
    }
    
    public Rarity getRarity() {
        return this.rarity;
    }
    
    public Component getName(final ItemStack stack) {
        return (Component)Component.literal("\u2726 Llave " + this.rarity.displayName() + " \u2726").withStyle(this.rarity.color());
    }
    
    public boolean isFoil(final ItemStack stack) {
        return true;
    }
    
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add((Component)Component.literal("Tier: ").withStyle(ChatFormatting.GRAY).append((Component)Component.literal(this.rarity.displayName()).withStyle(this.rarity.color())));
        tooltip.add((Component)Component.literal("Abre cualquier crate de tier " + this.rarity.displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add((Component)Component.literal("Click derecho sobre la crate con la llave en la mano.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
