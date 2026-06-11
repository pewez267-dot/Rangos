package com.gbaminecraft.minecraft.registry;

import com.gbaminecraft.GBAMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GBAMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> GBA_TAB = CREATIVE_TABS.register(
            "gbaminecraft_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gbaminecraft"))
                    .icon(() -> new ItemStack(ModItems.FANTASTIC_BOY.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.FANTASTIC_BOY.get());
                        output.accept(ModItems.GBA_CONSOLE.get());
                        output.accept(ModItems.GBA_CARTRIDGE.get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
