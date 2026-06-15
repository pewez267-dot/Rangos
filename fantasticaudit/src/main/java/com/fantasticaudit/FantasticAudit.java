package com.fantasticaudit;

import com.fantasticaudit.config.AuditConfig;
import com.fantasticaudit.logging.AuditLogger;
import com.fantasticaudit.logging.LogCleaner;
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
 * Fantastic Audit — passive, server-side player action auditing for Forge 1.20.1.
 *
 * <p>The mod registers its config, starts the asynchronous logging subsystem when the server
 * boots, runs log retention cleanup on a background thread, and flushes everything cleanly on
 * shutdown. All gameplay capture lives in the {@code events} package, registered statically via
 * {@code @Mod.EventBusSubscriber} on the Forge event bus.</p>
 */
@Mod(FantasticAudit.MODID)
public final class FantasticAudit {

    public static final String MODID = "fantasticaudit";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FantasticAudit() {
        // Registers config/fantasticaudit/config.toml using the Forge Config API.
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AuditConfig.SPEC, MODID + "/config.toml");

        // Server lifecycle hooks live on this instance on the Forge event bus.
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[FantasticAudit] Constructed; awaiting server start to initialise audit logging.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        Path configDir = FMLPaths.CONFIGDIR.get();
        AuditLogger logger = AuditLogger.get();
        logger.init(configDir);

        // Retention cleanup runs off-thread so a large log directory never delays server start.
        Path playersDir = logger.playersDir();
        if (playersDir != null) {
            LogCleaner.runAsync(playersDir, logger.retentionDays());
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        AuditLogger.get().shutdown();
    }
}
