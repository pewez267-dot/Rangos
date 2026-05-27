package com.claimblocks;

import com.claimblocks.block.ModBlocks;
import com.claimblocks.command.ClaimCommands;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.event.BlockProtectionEvents;
import com.claimblocks.event.ClaimEntryTracker;
import com.claimblocks.gui.ClaimMenuScreen;
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
        LOGGER.info("Initializing Claim Blocks Mod (Admin Edition v2.1 - MENU)");

        ModBlocks.registerModBlocks();
        ModItems.registerModItems();
        ClaimMenuScreen.registerScreenHandler();

        ClaimCommands.register();
        BlockProtectionEvents.register();
        ClaimEntryTracker.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ClaimManager.getInstance().loadClaims(server);
            LOGGER.info("Claims loaded from world data");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ClaimManager.getInstance().saveClaims(server);
            LOGGER.info("Claims saved to world data");
        });

        // Auto-save every 5 minutes (6000 ticks)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 6000 == 0 && ClaimManager.getInstance().isDirty()) {
                ClaimManager.getInstance().saveClaims(server);
            }
        });

        LOGGER.info("Claim Blocks Mod initialized successfully!");
    }
}
