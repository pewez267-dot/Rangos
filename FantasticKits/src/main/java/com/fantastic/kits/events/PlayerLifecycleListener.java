package com.fantastic.kits.events;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.Reference;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Player lifecycle observer. Refreshes per-player bookkeeping when a player
 * connects or disconnects so anti-exploit counters never persist beyond a
 * session and player files are warmed up the moment they arrive.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class PlayerLifecycleListener {

    private PlayerLifecycleListener() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        FantasticKits.antiExploit().clear(sp.getUUID());
        FantasticKits.players().get(sp.getUUID(), sp.getGameProfile().getName());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        FantasticKits.antiExploit().clear(sp.getUUID());
    }

    /**
     * Tracks crude packet rates against right-click interactions so a chain
     * of macro spam is detected even outside of the GUI flow.
     */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            FantasticKits.antiExploit().isPacketSpam(sp.getUUID());
        }
    }
}
