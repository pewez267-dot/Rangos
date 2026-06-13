// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.util;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import java.util.function.Function;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.EntityType;
import java.util.List;

public final class RegistryLists
{
    private RegistryLists() {
    }
    
    public static List<EntityType<?>> entities() {
        final List<EntityType<?>> list = new ArrayList<EntityType<?>>(ForgeRegistries.ENTITY_TYPES.getValues());
        list.sort(Comparator.comparing((Function<? super EntityType<?>, ? extends Comparable>)RegistryLists::entityId));
        return list;
    }
    
    public static List<Item> items() {
        final List<Item> list = new ArrayList<Item>(ForgeRegistries.ITEMS.getValues());
        list.sort(Comparator.comparing((Function<? super Item, ? extends Comparable>)RegistryLists::itemId));
        return list;
    }
    
    public static List<MobEffect> effects() {
        final List<MobEffect> list = new ArrayList<MobEffect>(ForgeRegistries.MOB_EFFECTS.getValues());
        list.sort(Comparator.comparing((Function<? super MobEffect, ? extends Comparable>)RegistryLists::effectId));
        return list;
    }
    
    public static String entityId(final EntityType<?> type) {
        final ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey((Object)type);
        return (rl == null) ? "minecraft:pig" : rl.toString();
    }
    
    public static String entityName(final EntityType<?> type) {
        return type.getDescription().getString();
    }
    
    public static String itemId(final Item item) {
        final ResourceLocation rl = ForgeRegistries.ITEMS.getKey((Object)item);
        return (rl == null) ? "minecraft:air" : rl.toString();
    }
    
    public static String itemName(final Item item) {
        return new ItemStack((ItemLike)item).getHoverName().getString();
    }
    
    public static String effectId(final MobEffect effect) {
        final ResourceLocation rl = ForgeRegistries.MOB_EFFECTS.getKey((Object)effect);
        return (rl == null) ? "minecraft:luck" : rl.toString();
    }
    
    public static String effectName(final MobEffect effect) {
        return effect.getDisplayName().getString();
    }
}
