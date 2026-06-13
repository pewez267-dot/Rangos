package com.fspawner.item;

import com.fspawner.FSpawner;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registro de items del mod Fantastic Spawner.
 */
public final class FSItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, FSpawner.MOD_ID);

    // Varita del Editor de Spawners: click derecho sobre un spawner para editarlo.
    public static final RegistryObject<Item> SPAWNER_WAND =
        ITEMS.register("spawner_wand", () -> new SpawnerWandItem(new Item.Properties().stacksTo(1)));

    private FSItems() {
    }

    public static void register(final IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
