package com.fantasticwatch;

import com.fantasticwatch.config.WatchConfig;
import com.fantasticwatch.logging.AliasTracker;
import com.fantasticwatch.logging.WatchLogger;
import com.fantasticwatch.tracking.LifecycleManager;
import com.fantasticwatch.tracking.TrackingIndex;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Fantastic Watch — forensic, persistent tracking of every item that leaves an operator's
 * creative inventory, for Forge 1.20.1.
 *
 * <p>On boot the mod starts its async logging subsystem, loads the global tracking index, and
 * runs the weekly (Monday-to-Monday by default) purge on a background thread. On shutdown it
 * flushes the index atomically and drains the log writer. All gameplay capture lives in the
 * {@code events} package, registered statically on the Forge event bus.</p>
 */
@Mod(FantasticWatch.MODID)
public final class FantasticWatch {

    public static final String MODID = "fantasticwatch";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FantasticWatch() {
        // Registers config/fantasticwatch/config.toml using the Forge Config API.
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WatchConfig.SPEC, MODID + "/config.toml");

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[FantasticWatch] Constructed; awaiting server start to initialise tracking.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        Path configDir = FMLPaths.CONFIGDIR.get();
        WatchLogger logger = WatchLogger.get();
        logger.init(configDir);

        // Load the global index before any tracking events can occur.
        TrackingIndex.get().load(logger.indexFile());

        // Username-change tracking so renamed operators' log files can be cross-linked.
        if (logger.baseDir() != null) {
            AliasTracker.get().init(logger.baseDir());
        }

        // Weekly purge runs off-thread so a large history never delays server start.
        LifecycleManager.runWeeklyPurgeAsync(logger.opsDir());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Persist the index atomically, then drain the log writer.
        TrackingIndex.get().shutdownAndFlush();
        WatchLogger.get().shutdown();
    }
}
