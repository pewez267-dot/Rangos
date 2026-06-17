package com.claimblocks;

import com.claimblocks.block.ModBlocks;
import com.claimblocks.command.ClaimCommands;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.event.BlockProtectionEvents;
import com.claimblocks.event.EntityProtectionEvents;
import com.claimblocks.event.PlayerTracker;
import com.claimblocks.gui.ClaimMenuHandler;
import com.claimblocks.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClaimBlocksMod implements ModInitializer {
    public static final String MOD_ID = "claimblocks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[ClaimBlocks] Inicializando v3.0.0...");

        ModBlocks.register();
        ModItems.register();

        ClaimCommands.register();
        BlockProtectionEvents.register();
        EntityProtectionEvents.register();
        PlayerTracker.register();
        ClaimMenuHandler.registerChatListener();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ClaimManager.getInstance().load(server);
            LOGGER.info("[ClaimBlocks] Datos cargados.");
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ClaimManager.getInstance().save();
            LOGGER.info("[ClaimBlocks] Datos guardados al apagar.");
        });

        // Per-tick: fire-extinction sweep + enter/exit detection
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PlayerTracker.tick(server);
            BlockProtectionEvents.tickFireSweep(server);
        });

        LOGGER.info("[ClaimBlocks] Inicializacion completada (10 tiers, 16 flags).");
    }
}
