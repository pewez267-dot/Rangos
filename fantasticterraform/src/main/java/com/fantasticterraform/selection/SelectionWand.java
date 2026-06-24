package com.fantasticterraform.selection;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * Varita de seleccion. No es craftable: se entrega automaticamente al entrar al
 * modo editor. Lleva el NBT {@code fantasticterraform_wand = 1b} para identificarla
 * de forma inequivoca. La interaccion real ocurre mediante raycasting manual del
 * lado cliente (en modo espectador los eventos de interaccion de bloque no se
 * disparan), por lo que esta clase no implementa logica de uso.
 */
public final class SelectionWand extends Item {

    public static final String WAND_TAG = "fantasticterraform_wand";

    public SelectionWand() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /** Marca un stack como varita valida. */
    public static ItemStack tagged(ItemStack stack) {
        stack.getOrCreateTag().putByte(WAND_TAG, (byte) 1);
        return stack;
    }

    public static boolean isWand(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrCreateTag().getByte(WAND_TAG) == (byte) 1;
    }
}
