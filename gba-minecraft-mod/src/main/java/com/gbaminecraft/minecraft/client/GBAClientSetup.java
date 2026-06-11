package com.gbaminecraft.minecraft.client;

import com.gbaminecraft.minecraft.gui.GBAMenu;
import com.gbaminecraft.minecraft.gui.GBAScreen;
import com.gbaminecraft.minecraft.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.gbaminecraft.GBAMod;

@Mod.EventBusSubscriber(modid = GBAMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GBAClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.GBA_MENU.get(), GBAScreen::new);
        });
    }
}
