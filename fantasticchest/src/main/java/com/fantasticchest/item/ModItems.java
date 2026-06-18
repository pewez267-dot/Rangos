package com.fantasticchest.item;

import com.fantasticchest.FantasticChest;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registers the chest item, the editor wand and the creative tab. */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FantasticChest.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(ForgeRegistries.CREATIVE_MODE_TABS, FantasticChest.MOD_ID);

    public static final RegistryObject<Item> CHEST_ITEM =
            ITEMS.register("fantastic_chest", () -> new ChestItem(new Item.Properties()));

    public static final RegistryObject<Item> EDITOR_WAND =
            ITEMS.register("editor_wand", () -> new EditorWandItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("fantasticchest",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fantasticchest"))
                    .icon(() -> new ItemStack(EDITOR_WAND.get()))
                    .displayItems((params, output) -> {
                        output.accept(EDITOR_WAND.get());
                        output.accept(CHEST_ITEM.get());
                    })
                    .build());

    private ModItems() {
    }

    public static void register(final IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
