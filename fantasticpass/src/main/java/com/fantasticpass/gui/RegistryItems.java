package com.fantasticpass.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side helper that exposes the full item registry for the admin item picker,
 * plus display-name / id helpers used for rendering and search filtering.
 */
public final class RegistryItems {

    private static List<Item> cached;

    private RegistryItems() {
    }

    public static List<Item> all() {
        if (cached == null) {
            List<Item> list = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item != Items.AIR) {
                    list.add(item);
                }
            }
            cached = list;
        }
        return cached;
    }

    public static String name(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    public static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }
}
