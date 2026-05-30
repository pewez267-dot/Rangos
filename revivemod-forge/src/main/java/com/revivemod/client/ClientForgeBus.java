package com.revivemod.client;

import com.revivemod.RevivemodForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side FORGE-bus events: per-tick key polling, blocking the inventory screen while downed,
 * and resetting local state on (dis)connect.
 */
@Mod.EventBusSubscriber(modid = RevivemodForge.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeBus {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RevivemodClient.onClientTick(Minecraft.getInstance());
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (RevivemodClient.LOCAL_DOWNED && event.getNewScreen() instanceof AbstractContainerScreen) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        RevivemodClient.reset();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RevivemodClient.reset();
    }
}
