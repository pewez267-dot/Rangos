// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.item;

import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class EditorWandItem extends Item
{
    public EditorWandItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }
    
    public Component getName(final ItemStack stack) {
        return (Component)Component.literal("\u2726 Varita del Editor \u2726").withStyle(ChatFormatting.LIGHT_PURPLE);
    }
    
    public boolean isFoil(final ItemStack stack) {
        return true;
    }
    
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add((Component)Component.literal("\u00dasala en un cofre para editarlo.").withStyle(ChatFormatting.GRAY));
    }
}
