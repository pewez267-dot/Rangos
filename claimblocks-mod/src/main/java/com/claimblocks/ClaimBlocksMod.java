package com.claimblocks;

import com.claimblocks.block.ModBlocks;
import com.claimblocks.command.ClaimAdminCommands;
import com.claimblocks.command.ClaimCommands;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.GlobalFlags;
import com.claimblocks.event.BlockProtectionEvents;
import com.claimblocks.event.EntityProtectionEvents;
import com.claimblocks.event.PassiveEffectsManager;
import com.claimblocks.event.PlayerTracker;
import com.claimblocks.gui.ClaimMenuHandler;
import com.claimblocks.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side only entry point.
 *
 * v5.0 deliberately removes the client mod (and the in-game cube outline
 * preview that came with it). The mod is now installed exclusively on the
 * server: vanilla clients can join and use every feature - menus, /claim
 * commands, admin panel - via the normal chest-screen GUI which Minecraft
 * already knows how to render server-driven.
 */
public class ClaimBlocksMod implements ModInitializer {
    public static final String MOD_ID = "claimblocks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[ClaimBlocks] Inicializando v5.0.0 (server-side only)...");

        ModBlocks.register();
        ModItems.register();

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

        // Deliver any pending messages queued for offline owners
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ClaimManager.getInstance().flushPendingTo(handler.getPlayer());
        });

        // Per-tick: fire-extinction sweep + enter/exit detection + passive effects
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PlayerTracker.tick(server);
            BlockProtectionEvents.tickFireSweep(server);
            PassiveEffectsManager.tick(server);
        });

        LOGGER.info("[ClaimBlocks] Inicializacion completada (10 tiers, 26 flags, panel admin).");
    }
}
