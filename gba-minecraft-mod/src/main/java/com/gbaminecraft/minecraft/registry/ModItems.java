package com.gbaminecraft.minecraft.registry;

import com.gbaminecraft.GBAMod;
import com.gbaminecraft.minecraft.item.GBACartridgeItem;
import com.gbaminecraft.minecraft.item.FantasticBoyItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GBAMod.MOD_ID);

    // GBA Console block item
    public static final RegistryObject<Item> GBA_CONSOLE = ITEMS.register(
            "gba_console",
            () -> new BlockItem(ModBlocks.GBA_CONSOLE.get(), new Item.Properties().stacksTo(1))
    );

    // GBA Cartridge item (holds ROM data as NBT)
    public static final RegistryObject<Item> GBA_CARTRIDGE = ITEMS.register(
            "gba_cartridge",
            () -> new GBACartridgeItem(new Item.Properties().stacksTo(1))
    );

    // Fantastic Boy Advance — the handheld console item (right-click to play)
    public static final RegistryObject<Item> FANTASTIC_BOY = ITEMS.register(
            "fantastic_boy_advance",
            () -> new FantasticBoyItem(new Item.Properties().stacksTo(1))
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
