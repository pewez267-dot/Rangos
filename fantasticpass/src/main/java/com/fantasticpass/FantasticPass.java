package com.fantasticpass;

import com.fantasticpass.capability.CapabilityEvents;
import com.fantasticpass.config.PassConfig;
import com.fantasticpass.events.ServerEvents;
import com.fantasticpass.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Fantastic Pass entry point. Wires configuration, networking, capabilities, and the
 * gameplay event handlers on the correct event buses.
 */
@Mod(FantasticPass.MOD_ID)
public final class FantasticPass {

    public static final String MOD_ID = "fantasticpass";

    public FantasticPass() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener(this::commonSetup);

        // Capability registration is on the mod bus; attach/clone are on the forge bus.
        CapabilityEvents capabilityEvents = new CapabilityEvents();
        modBus.register(capabilityEvents);
        MinecraftForge.EVENT_BUS.register(capabilityEvents);

        // Gameplay events (tick, anti-AFK interactions, commands, login sync).
        MinecraftForge.EVENT_BUS.register(new ServerEvents());

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PassConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }
}
