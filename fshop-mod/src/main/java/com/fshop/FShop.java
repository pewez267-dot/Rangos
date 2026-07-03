package com.fshop;

import com.fshop.config.FShopConfig;
import com.fshop.network.PacketHandler;
import com.fshop.registry.ModCreativeTab;
import com.fshop.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FShop.MOD_ID)
public final class FShop {
   public static final String MOD_ID = "fshop";

   public FShop() {
      IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
      ModItems.register(modBus);
      ModCreativeTab.register(modBus);
      modBus.addListener(this::commonSetup);
      ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FShopConfig.SPEC);
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(PacketHandler::register);
   }
}
