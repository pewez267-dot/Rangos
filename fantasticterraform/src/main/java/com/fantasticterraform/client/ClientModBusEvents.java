package com.fantasticterraform.client;

import com.fantasticterraform.FantasticTerraform;
import com.fantasticterraform.client.hud.TerraformHudOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Eventos del mod-bus del lado cliente: registra el overlay del HUD y las teclas.
 */
@Mod.EventBusSubscriber(modid = FantasticTerraform.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModBusEvents {

    private ClientModBusEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("terraform_hud", TerraformHudOverlay.INSTANCE);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.OPEN_PANELS);
        event.register(Keybinds.TOGGLE_WAND);
    }
}
