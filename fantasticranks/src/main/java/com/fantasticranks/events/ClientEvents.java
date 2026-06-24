package com.fantasticranks.events;

import com.fantasticranks.FantasticRanks;
import com.fantasticranks.nametag.ClientNametagCache;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only forge-bus handlers. Clears the nametag cache on disconnect so stale data is
 * not shown after switching worlds/servers.
 */
@Mod.EventBusSubscriber(modid = FantasticRanks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientNametagCache.clear();
    }
}
