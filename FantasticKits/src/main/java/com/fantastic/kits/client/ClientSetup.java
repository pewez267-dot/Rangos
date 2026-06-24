package com.fantastic.kits.client;

import com.fantastic.kits.Reference;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side bootstrap. Mirrors the FantasticCrates / FantasticSpawners
 * pattern: empty for now (Fantastic Kits has no custom MenuType to attach to a
 * Screen factory because it uses standalone {@link net.minecraft.client.gui.screens.Screen}
 * subclasses opened directly from network packets), but kept as a hook for
 * future client-only setup.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // No menu screen registrations needed: every Fantastic Kits screen is
        // opened directly via Minecraft.setScreen(...) from ClientPacketHandler.
    }
}
