package com.fantastic.kits.events;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.Reference;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Re-runs command discovery whenever the data-pack pipeline refreshes the
 * Brigadier dispatcher. This keeps the Command Manager GUI consistent with
 * the live command registry without restarting the server.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class ServerReloadListener {

    private ServerReloadListener() {}

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        try {
            if (FantasticKits.commands() != null) {
                FantasticKits.commands().rebuild();
            }
        } catch (Throwable t) {
            FantasticKits.LOGGER.error("Datapack sync rebuild failed", t);
        }
    }
}
