package com.fantastic.kits.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Read-only helpers used by the client screens to populate the ScrollSelector
 * lists. Mirrors the helper of the same name in FantasticCrates.
 */
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

    public static List<ResourceLocation> enchantments() {
        List<ResourceLocation> list = new ArrayList<>();
        for (Enchantment e : ForgeRegistries.ENCHANTMENTS.getValues()) {
            ResourceLocation rl = ForgeRegistries.ENCHANTMENTS.getKey(e);
            if (rl != null) list.add(rl);
        }
        list.sort(Comparator.comparing(ResourceLocation::toString));
        return list;
    }

    public static List<ResourceLocation> attributes() {
        List<ResourceLocation> list = new ArrayList<>();
        for (Attribute a : ForgeRegistries.ATTRIBUTES.getValues()) {
            ResourceLocation rl = ForgeRegistries.ATTRIBUTES.getKey(a);
            if (rl != null) list.add(rl);
        }
        list.sort(Comparator.comparing(ResourceLocation::toString));
        return list;
    }
}
