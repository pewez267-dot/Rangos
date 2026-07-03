package com.fscrates;

import com.fscrates.client.ClientSetup;
import com.fscrates.command.FSCrateCommand;
import com.fscrates.network.FSNetwork;
import com.fscrates.registry.ModRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(value="fscrates")
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

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(FSNetwork::register);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetup::init);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        FSCrateCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
    }
}

