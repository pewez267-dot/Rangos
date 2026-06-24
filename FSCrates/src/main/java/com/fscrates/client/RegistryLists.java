package com.fscrates.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pulls live item lists from the Forge registries (supports any mod). */
public final class RegistryLists {

    private RegistryLists() {}

    public static List<Item> items() {
        List<Item> list = new ArrayList<>(ForgeRegistries.ITEMS.getValues());
        list.sort(Comparator.comparing(RegistryLists::itemId));
        return list;
    }

    public static String itemId(Item item) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        return rl == null ? "minecraft:air" : rl.toString();
    }

    public static String itemName(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }
}
