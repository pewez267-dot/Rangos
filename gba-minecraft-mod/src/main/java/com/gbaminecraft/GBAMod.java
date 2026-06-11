package com.gbaminecraft;

import com.gbaminecraft.minecraft.registry.ModBlocks;
import com.gbaminecraft.minecraft.registry.ModCreativeTabs;
import com.gbaminecraft.minecraft.registry.ModItems;
import com.gbaminecraft.minecraft.registry.ModMenuTypes;
import com.gbaminecraft.minecraft.registry.ModTileEntities;
import com.gbaminecraft.minecraft.network.GBANetworkHandler;
import com.gbaminecraft.minecraft.command.GBACommand;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GBAMod.MOD_ID)
public class GBAMod {

    public static final String MOD_ID = "gbaminecraft";
    public static final Logger LOGGER = LogManager.getLogger();

    public GBAMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModTileEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            GBANetworkHandler.registerPackets();
            LOGGER.info("GBA Minecraft Mod initialized — emulator ready.");
            LOGGER.info("Commands: /gba give | /gba cartridge | /gba load <file.gba> | /gba info");
        });
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("GBA Mod client setup complete.");
        }
    }
}
