package com.fspawner;

import com.fspawner.client.ClientSetup;
import com.fspawner.command.FSpawnerCommand;
import com.fspawner.event.FSpawnerEvents;
import com.fspawner.network.FSNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Fantastic Spawner (FSpawner) main entry point.
 *
 * FSpawner does NOT add a new block; it supercharges the vanilla
 * {@code minecraft:spawner} through NBT and Forge events, and exposes a modern
 * GUI to OP level 4 admins via {@code /fspawner}.
 */
@Mod(FSpawner.MOD_ID)
public class FSpawner {

    public static final String MOD_ID = "fspawner";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FSpawner() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);

        // Gameplay events (tooltips, spawning, drops) live on the Forge bus.
        MinecraftForge.EVENT_BUS.register(FSpawnerEvents.class);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

        LOGGER.info("[FSpawner] Initializing Fantastic Spawner");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(FSNetwork::register);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetup::init);
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        FSpawnerCommand.register(event.getDispatcher());
    }
}
