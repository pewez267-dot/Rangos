package com.fscrates.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

// Llave UNIVERSAL "Fantastic Key". Reemplaza a las 5 llaves por rareza: una sola llave
// abre CUALQUIER crate y la rareza del premio se decide al abrir (segun la tabla de
// probabilidad de rarezas de esa crate). Textura/modelo = w6_ultimate (oro + gema morada).
public class KeyItem
extends Item {
    public KeyItem() {
        super(new Item.Properties().stacksTo(64));
    }

    public Component getName(ItemStack stack) {
        return Component.literal((String)"\u2726 Fantastic Key \u2726").withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add((Component)Component.literal((String)"Llave universal").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add((Component)Component.literal((String)"Abre CUALQUIER crate.").withStyle(ChatFormatting.GRAY));
        tooltip.add((Component)Component.literal((String)"La rareza del premio se decide al abrir.").withStyle(ChatFormatting.GRAY));
        tooltip.add((Component)Component.literal((String)"Click derecho sobre la crate con la llave en la mano.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
