package com.revivemod.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "revivemod", bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public final class ClientModBus {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("revive_hud",
                (gui, guiGraphics, partialTick, screenWidth, screenHeight) ->
                        RevivemodClient.onHud(guiGraphics, screenWidth));
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(RevivemodKeybinds.SURRENDER);
        event.register(RevivemodKeybinds.SELF_REVIVE);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Habilita el boton "Configuracion" dentro de Mods > Revive Mod, que abre
        // nuestra pantalla para cambiar las teclas dentro del juego.
        event.enqueueWork(() -> ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> new ReviveConfigScreen(parent))));
    }
}
