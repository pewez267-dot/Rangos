package com.fantasticchest;

import com.fantasticchest.block.ModBlocks;
import com.fantasticchest.commands.FsChestCommand;
import com.fantasticchest.config.ChestConfig;
import com.fantasticchest.data.ChestRegistry;
import com.fantasticchest.gui.ModMenus;
import com.fantasticchest.item.ModItems;
import com.fantasticchest.network.PacketHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point for Fantastic Chest.
 *
 * <p>No tick handlers of any kind are registered: all logic is event-driven (GUI open,
 * extraction, OP edit). Persistence is asynchronous and flushed on shutdown.</p>
 */
@Mod(FantasticChest.MOD_ID)
public final class FantasticChest {

    public static final String MOD_ID = "fantasticchest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FantasticChest() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModMenus.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ChestConfig.SPEC, "fantasticchest/config.toml");

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::onConfigLoad);
        modBus.addListener(this::onConfigReload);

        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);

        LOGGER.info("[FantasticChest] Inicializando Fantastic Chest");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(com.fantasticchest.gui.ClientSetup::register);
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ChestConfig.SPEC) {
            ChestConfig.bake();
        }
    }

    private void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ChestConfig.SPEC) {
            ChestConfig.bake();
        }
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        FsChestCommand.register(event.getDispatcher());
    }

    private void onServerStarting(final ServerStartingEvent event) {
        // Load chests.json into memory exactly once. Never read from disk during gameplay.
        ChestRegistry.get().load();
    }

    private void onServerStopping(final ServerStoppingEvent event) {
        // Guarantee a final async flush before shutdown completes.
        ChestRegistry.get().flush();
    }
}
