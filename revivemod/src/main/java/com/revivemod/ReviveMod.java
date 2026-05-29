package com.revivemod;

import com.revivemod.command.ReviveCommands;
import com.revivemod.config.ReviveConfig;
import com.revivemod.event.ConnectionHandler;
import com.revivemod.event.DamageHandler;
import com.revivemod.event.DownTicker;
import com.revivemod.event.InteractionHandler;
import com.revivemod.event.RestrictionHandler;
import com.revivemod.state.DownManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Main entrypoint for ReviveMod (server-side only).
 *
 * Provides a knock-out / revival system similar to "Hardcore Revival" but
 * implemented entirely server-side using bossbars + status effects + Fabric API
 * events. No mixins, no client mod required.
 */
public final class ReviveMod implements ModInitializer {

    public static final String MOD_ID = "revivemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ReviveConfig CONFIG = new ReviveConfig();
    private static Path CONFIG_PATH;

    @Override
    public void onInitialize() {
        CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
        CONFIG = ReviveConfig.load(CONFIG_PATH);

        DamageHandler.register();
        DownTicker.register();
        ConnectionHandler.register();
        InteractionHandler.register();
        RestrictionHandler.register();
        ReviveCommands.register();

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("[{}] Saving config and clearing down state...", MOD_ID);
            saveConfig();
            DownManager.clearAll(server);
        });

        LOGGER.info("[{}] ReviveMod initialised. downTime={}s, reviveDistance={} blocks, reviveTime={} ticks",
                MOD_ID, CONFIG.downTimeSeconds, CONFIG.reviveDistance, CONFIG.reviveTimeTicks);
    }

    public static ReviveConfig getConfig() {
        return CONFIG;
    }

    public static void saveConfig() {
        if (CONFIG_PATH != null) CONFIG.save(CONFIG_PATH);
    }

    public static void reloadConfig() {
        if (CONFIG_PATH != null) CONFIG = ReviveConfig.load(CONFIG_PATH);
    }
}
