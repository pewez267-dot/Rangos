// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.CommandDispatcher;
import com.fspawner.command.FSpawnerCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import com.fspawner.client.ClientSetup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.fspawner.network.FSNetwork;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import com.fspawner.event.FSpawnerEvents;
import net.minecraftforge.common.MinecraftForge;
import java.util.function.Consumer;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import net.minecraftforge.fml.common.Mod;

@Mod("fspawner")
public class FSpawner
{
    public static final String MOD_ID = "fspawner";
    public static final Logger LOGGER;
    
    public FSpawner() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        com.fspawner.item.FSItems.register(modBus);
        modBus.addListener((Consumer)this::commonSetup);
        modBus.addListener((Consumer)this::clientSetup);
        MinecraftForge.EVENT_BUS.register((Object)FSpawnerEvents.class);
        MinecraftForge.EVENT_BUS.addListener((Consumer)this::registerCommands);
        FSpawner.LOGGER.info("[FSpawner] Initializing Fantastic Spawner");
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(FSNetwork::register);
    }
    
    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetup::init);
    }
    
    private void registerCommands(final RegisterCommandsEvent event) {
        FSpawnerCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
    }
    
    static {
        LOGGER = LogUtils.getLogger();
    }
}
