package com.fantasticpass;

import com.fantasticpass.capability.CapabilityEvents;
import com.fantasticpass.config.PassConfig;
import com.fantasticpass.events.ServerEvents;
import com.fantasticpass.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("fantasticpass")
public final class FantasticPass {
   public static final String MOD_ID = "fantasticpass";

   public FantasticPass() {
      IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
      modBus.addListener(this::commonSetup);
      modBus.addListener(CapabilityEvents::registerCapabilities);
      MinecraftForge.EVENT_BUS.register(new CapabilityEvents());
      MinecraftForge.EVENT_BUS.register(new ServerEvents());
      ModLoadingContext.get().registerConfig(Type.COMMON, PassConfig.SPEC);
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(PacketHandler::register);
   }
}
