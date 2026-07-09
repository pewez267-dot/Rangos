package com.fsrecipes.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Utilidades de listado del registro para la GUI (todos los items de todos los mods). */
public final class RegistryLists {

    private RegistryLists() {}

    public static List<Item> items() {
        List<Item> list = new ArrayList<>(ForgeRegistries.ITEMS.getValues());
        list.remove(Items.AIR);
        list.sort(Comparator.comparing(RegistryLists::itemId));
        return list;
    }

    /** Items visibles en una pestana creativa (para banear por categoria). */
    public static List<Item> itemsOfTab(ResourceKey<CreativeModeTab> key) {
        Set<Item> set = new LinkedHashSet<>();
        try {
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
            if (tab != null) {
                for (ItemStack stack : tab.getDisplayItems()) {
                    if (stack != null && !stack.isEmpty()) {
                        set.add(stack.getItem());
                    }
                }
            }
        } catch (Throwable ignored) {
            // Si la pestana aun no se construyo, devolvemos lo que haya (posiblemente vacio).
        }
        return new ArrayList<>(set);
    }

    public static String itemId(Item item) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        return rl == null ? "minecraft:air" : rl.toString();
    }

    public static ResourceLocation id(Item item) {
        return ForgeRegistries.ITEMS.getKey(item);
    }

    public static String itemName(Item item) {
        return new ItemStack((ItemLike) item).getHoverName().getString();
    }
}
