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
        super(new Item.Properties().m_41487_(64));
        this.rarity = rarity;
    }
    
    public Rarity getRarity() {
        return this.rarity;
    }
    
    public Component m_7626_(final ItemStack stack) {
        return (Component)Component.m_237113_("\u2726 Llave " + this.rarity.displayName() + " \u2726").m_130940_(this.rarity.color());
    }
    
    public boolean m_5812_(final ItemStack stack) {
        return true;
    }
    
    public void m_7373_(final ItemStack stack, @Nullable final Level level, final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add((Component)Component.m_237113_("Tier: ").m_130940_(ChatFormatting.GRAY).m_7220_((Component)Component.m_237113_(this.rarity.displayName()).m_130940_(this.rarity.color())));
        tooltip.add((Component)Component.m_237113_("Abre cualquier crate de tier " + this.rarity.displayName()).m_130940_(ChatFormatting.GRAY));
        tooltip.add((Component)Component.m_237113_("Click derecho sobre la crate con la llave en la mano.").m_130940_(ChatFormatting.DARK_GRAY));
    }
}
