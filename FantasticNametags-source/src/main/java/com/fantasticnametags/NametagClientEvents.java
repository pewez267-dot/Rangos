package com.fantasticnametags;

import com.fantasticnametags.client.ClientNametagState;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Se dispara al principio del render del nombre de una entidad. Al desplazar el PoseStack aqui,
 * TODO lo que se dibuje despues en ese mismo render (nombre vanilla + lineas extra que inyectan
 * otros mods al final del metodo) queda desplazado por igual.
 *
 * Lee los valores de ClientNametagState, que el servidor actualiza en vivo por red.
 */
@Mod.EventBusSubscriber(modid = FantasticNametags.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NametagClientEvents {

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        double off = ClientNametagState.heightOffset();
        if (off == 0.0) {
            return;
        }
        if (ClientNametagState.playersOnly() && !(event.getEntity() instanceof Player)) {
            return;
        }
        event.getPoseStack().translate(0.0, off, 0.0);
    }

    private NametagClientEvents() {
    }
}
