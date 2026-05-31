package com.revivemod.client;

import com.revivemod.RevivemodForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side MOD-bus setup: registers the downed HUD overlay.
 */
@Mod.EventBusSubscriber(modid = RevivemodForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModBus {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("revive_hud",
                (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> RevivemodClient.onHud(guiGraphics, screenWidth));
    }
}
