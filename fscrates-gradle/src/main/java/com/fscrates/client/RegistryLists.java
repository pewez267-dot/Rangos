// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.client;

import java.util.Iterator;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceKey;
import java.util.Map;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Function;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.Item;
import java.util.List;

public final class RegistryLists
{
    private RegistryLists() {
    }
    
    public static List<Item> items() {
        final List<Item> list = new ArrayList<Item>(ForgeRegistries.ITEMS.getValues());
        list.sort(Comparator.comparing((Function<? super Item, ? extends Comparable>)RegistryLists::itemId));
        return list;
    }
    
    public static String itemId(final Item item) {
        final ResourceLocation rl = ForgeRegistries.ITEMS.getKey((Object)item);
        return (rl == null) ? "minecraft:air" : rl.toString();
    }
    
    public static String itemName(final Item item) {
        return new ItemStack((ItemLike)item).getHoverName().getString();
    }
    
    public static List<ResourceLocation> particles() {
        final List<ResourceLocation> list = new ArrayList<ResourceLocation>();
        list.add(new ResourceLocation("minecraft", "dust"));
        for (final Map.Entry<ResourceKey<ParticleType<?>>, ParticleType<?>> e : ForgeRegistries.PARTICLE_TYPES.getEntries()) {
            final ParticleType<?> type = e.getValue();
            if (type instanceof SimpleParticleType) {
                final ResourceLocation key = e.getKey().location();
                if (key.toString().equals("minecraft:dust")) {
                    continue;
                }
                list.add(key);
            }
        }
        list.sort(Comparator.comparing((Function<? super ResourceLocation, ? extends Comparable>)ResourceLocation::toString));
        return list;
    }
}
