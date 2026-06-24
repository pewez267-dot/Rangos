package com.fscrates;

import com.fscrates.client.ClientSetup;
import com.fscrates.command.FSCrateCommand;
import com.fscrates.network.FSNetwork;
import com.fscrates.registry.ModRegistry;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Fantastic Crates (FSCrates) — advanced, GUI-editable crate system for
 * Forge 1.20.1. (c) Pewez. Todos los derechos reservados.
 *
 * <p>The crate is a real placeable block backed by a BlockEntity (like a
 * chest): place it in the world and right-click it with the matching key to
 * open it. Keys are a plain item that never places anything.
 */
@Mod(FSCrates.MOD_ID)
public class FSCrates {

    public static final String MOD_ID = "fscrates";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FSCrates() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModRegistry.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

        LOGGER.info("[FSCrates] Initializing Fantastic Crates");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(FSNetwork::register);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetup::init);
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        FSCrateCommand.register(event.getDispatcher());
    }
}
