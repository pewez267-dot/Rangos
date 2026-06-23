package com.fantasticranks;

import com.fantasticranks.capability.CapabilityEvents;
import com.fantasticranks.config.RanksConfig;
import com.fantasticranks.events.ServerEvents;
import com.fantasticranks.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Fantastic Ranks entry point. Wires configuration, networking, capabilities, and the
 * gameplay event handlers on the correct event buses.
 */
@Mod(FantasticRanks.MOD_ID)
public final class FantasticRanks {

    public static final String MOD_ID = "fantasticranks";

    public FantasticRanks() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener(this::commonSetup);

        // RegisterCapabilitiesEvent is a mod-bus event; attach/clone are forge-bus events.
        // They must be registered on their respective buses separately.
        modBus.addListener(CapabilityEvents::registerCapabilities);
        MinecraftForge.EVENT_BUS.register(new CapabilityEvents());

        MinecraftForge.EVENT_BUS.register(new ServerEvents());

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RanksConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }
}
