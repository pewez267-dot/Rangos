package com.fshop.registry;

import com.fshop.FShop;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTab {
   public static final DeferredRegister<CreativeModeTab> TABS =
         DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, FShop.MOD_ID);

   public static final RegistryObject<CreativeModeTab> FSHOP_TAB = TABS.register("fshop_tab",
         () -> CreativeModeTab.builder()
               .withTabsBefore(CreativeModeTabs.COMBAT)
               .title(Component.translatable("itemGroup.fshop"))
               .icon(() -> new ItemStack(ModItems.MARKET_WAND.get()))
               .displayItems((params, output) -> output.accept(ModItems.MARKET_WAND.get()))
               .build());

   private ModCreativeTab() {
   }

   public static void register(IEventBus bus) {
      TABS.register(bus);
   }
}
