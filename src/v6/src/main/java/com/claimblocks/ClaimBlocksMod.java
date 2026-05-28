/*
 * ClaimBlocksMod v6.0.0 - 100% server-side, sin bloques custom.
 * No registra nada en class_7923 (block/item registry); usa concretos vanilla
 * + NBT custom_data para identificar Claim Stones.
 */
package com.claimblocks;

import com.claimblocks.command.ClaimAdminCommands;
import com.claimblocks.command.ClaimCommands;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.GlobalFlags;
import com.claimblocks.event.BlockProtectionEvents;
import com.claimblocks.event.ClaimGlintParticles;
import com.claimblocks.event.EntityProtectionEvents;
import com.claimblocks.event.PassiveEffectsManager;
import com.claimblocks.event.PlayerTracker;
import com.claimblocks.gui.ClaimMenuHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClaimBlocksMod implements ModInitializer {
    public static final String MOD_ID = "claimblocks";
    public static final Logger LOGGER = LoggerFactory.getLogger("claimblocks");

    @Override
    public void onInitialize() {
        LOGGER.info("[ClaimBlocks] Inicializando v6.0.0 (100% vanilla, sin registros custom)...");
        ClaimCommands.register();
        ClaimAdminCommands.register();
        BlockProtectionEvents.register();
        EntityProtectionEvents.register();
        PlayerTracker.register();
        ClaimMenuHandler.registerChatListener();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ClaimManager.getInstance().load(server);
            GlobalFlags.getInstance().load(server);
            LOGGER.info("[ClaimBlocks] Datos cargados.");
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ClaimManager.getInstance().save();
            GlobalFlags.getInstance().save(server);
            LOGGER.info("[ClaimBlocks] Datos guardados al apagar.");
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ClaimManager.getInstance().flushPendingTo(handler.field_14140));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PlayerTracker.tick(server);
            BlockProtectionEvents.tickFireSweep(server);
            PassiveEffectsManager.tick(server);
            ClaimGlintParticles.tick(server);
        });
        LOGGER.info("[ClaimBlocks] Inicializacion completada (10 tiers, 26 flags, panel admin).");
    }
}
