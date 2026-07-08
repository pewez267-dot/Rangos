package com.fantasticnametags;

import com.fantasticnametags.client.ClientNametagState;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Se dispara al principio del render del nombre de una entidad. Al desplazar el PoseStack aqui,
 * TODO lo que se dibuje despues en ese mismo render (nombre vanilla + lineas extra que inyectan
 * otros mods al final del metodo) queda desplazado por igual.
 *
 * Lee los valores de ClientNametagState, que el servidor actualiza en vivo por red.
 *
 * IMPORTANTE (fix sombra): el evento RenderNameTagEvent NO tiene un "despues", asi que el
 * translate que aplicamos aqui se queda pegado en el PoseStack. Ese mismo PoseStack lo reutiliza
 * EntityRenderDispatcher.render() para dibujar la SOMBRA del suelo (el circulito negro en los
 * pies) JUSTO despues del nombre. Si no revertimos el desplazamiento, la sombra sube junto con el
 * nombre y queda despegada del suelo (a la altura de las rodillas).
 *
 * Solucion: en LivingEntityRenderer.render() Forge dispara RenderLivingEvent.Post con el MISMO
 * PoseStack, justo despues del nombre (super.render) y ANTES de que el dispatcher dibuje la
 * sombra. Ahi revertimos exactamente el mismo desplazamiento -> el nombre queda arriba y la
 * sombra vuelve al suelo.
 */
@Mod.EventBusSubscriber(modid = FantasticNametags.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NametagClientEvents {

    // Desplazamiento aplicado a la entidad que se esta renderizando ahora mismo (para revertirlo
    // antes de la sombra). El render es de un solo hilo y los eventos Living Pre/NameTag/Post no
    // se anidan, asi que un campo estatico es seguro.
    private static double appliedOffset = 0.0;

    /** Reinicia el estado al empezar a renderizar una entidad viva (por si Pre lo cancela otro mod). */
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        appliedOffset = 0.0;
    }

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
        appliedOffset = off;
    }

    /**
     * Se dispara al final de LivingEntityRenderer.render(), despues del nombre y ANTES de que el
     * dispatcher dibuje la sombra del suelo. Revertimos el desplazamiento para que la sombra no
     * suba con el nombre.
     */
    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (appliedOffset != 0.0) {
            event.getPoseStack().translate(0.0, -appliedOffset, 0.0);
            appliedOffset = 0.0;
        }
    }

    private NametagClientEvents() {
    }
}
