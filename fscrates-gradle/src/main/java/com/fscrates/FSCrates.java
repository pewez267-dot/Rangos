// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.CommandDispatcher;
import com.fscrates.command.FSCrateCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import com.fscrates.client.ClientSetup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.fscrates.network.FSNetwork;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import java.util.function.Consumer;
import com.fscrates.registry.ModRegistry;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import net.minecraftforge.fml.common.Mod;

@Mod("fscrates")
public class FSCrates
{
    public static final String MOD_ID = "fscrates";
    public static final Logger LOGGER;
    
    public FSCrates() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(modBus);
        modBus.addListener((Consumer)this::commonSetup);
        modBus.addListener((Consumer)this::clientSetup);
        MinecraftForge.EVENT_BUS.addListener((Consumer)this::registerCommands);
        FSCrates.LOGGER.info("[FSCrates] Initializing Fantastic Crates");
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(FSNetwork::register);
    }
    
    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetup::init);
    }
    
    private void registerCommands(final RegisterCommandsEvent event) {
        FSCrateCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
    }
    
    static {
        LOGGER = LogUtils.getLogger();
    }
}
