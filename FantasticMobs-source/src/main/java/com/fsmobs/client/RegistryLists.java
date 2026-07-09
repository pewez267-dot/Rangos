package com.fsmobs.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Listado de mobs del registro (vanilla + todos los mods) para la GUI. */
public final class RegistryLists {

    private RegistryLists() {}

    /** Todos los tipos de entidad que son mobs vivos (excluye items, proyectiles, etc.). */
    public static List<EntityType<?>> mobs() {
        List<EntityType<?>> list = new ArrayList<>();
        for (EntityType<?> t : ForgeRegistries.ENTITY_TYPES.getValues()) {
            if (t.getCategory() != MobCategory.MISC) {
                list.add(t);
            }
        }
        list.sort(Comparator.comparing(RegistryLists::id));
        return list;
    }

    public static String id(EntityType<?> type) {
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return rl == null ? "minecraft:pig" : rl.toString();
    }

    public static String name(EntityType<?> type) {
        return type.getDescription().getString();
    }

    /** Icono: el huevo de aparicion del mob si existe, si no vacio. */
    public static ItemStack icon(EntityType<?> type) {
        SpawnEggItem egg = SpawnEggItem.byId(type);
        return egg == null ? ItemStack.EMPTY : new ItemStack(egg);
    }
}
