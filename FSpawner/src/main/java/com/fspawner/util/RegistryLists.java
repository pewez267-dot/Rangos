package com.fspawner.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pulls the live entity / item / effect lists directly from the Forge
 * registries so every modded entry is supported automatically (no fixed lists).
 */
public final class RegistryLists {

    private RegistryLists() {}

    public static List<EntityType<?>> entities() {
        List<EntityType<?>> list = new ArrayList<>(ForgeRegistries.ENTITY_TYPES.getValues());
        list.sort(Comparator.comparing(RegistryLists::entityId));
        return list;
    }

    public static List<Item> items() {
        List<Item> list = new ArrayList<>(ForgeRegistries.ITEMS.getValues());
        list.sort(Comparator.comparing(RegistryLists::itemId));
        return list;
    }

    public static List<MobEffect> effects() {
        List<MobEffect> list = new ArrayList<>(ForgeRegistries.MOB_EFFECTS.getValues());
        list.sort(Comparator.comparing(RegistryLists::effectId));
        return list;
    }

    public static String entityId(EntityType<?> type) {
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return rl == null ? "minecraft:pig" : rl.toString();
    }

    public static String entityName(EntityType<?> type) {
        return type.getDescription().getString();
    }

    public static String itemId(Item item) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        return rl == null ? "minecraft:air" : rl.toString();
    }

    public static String itemName(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    public static String effectId(MobEffect effect) {
        ResourceLocation rl = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return rl == null ? "minecraft:luck" : rl.toString();
    }

    public static String effectName(MobEffect effect) {
        return effect.getDisplayName().getString();
    }
}
