package com.fantasticpass.events;

import com.fantasticpass.nametag.ClientNametagCache;
import com.fantasticpass.client.PassPlaylistManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "fantasticpass",
   value = {Dist.CLIENT},
   bus = Bus.FORGE
)
public final class ClientEvents {
   private ClientEvents() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         PassPlaylistManager.clientTick();
      }
   }

   @SubscribeEvent
   public static void onLoggingOut(LoggingOut event) {
      ClientNametagCache.clear();
      PassPlaylistManager.stop();
   }
}
