/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits;

import com.mojang.logging.LogUtils;
import com.pewez777.fantastickits.commands.FKitsCommand;
import com.pewez777.fantastickits.commandsystem.CommandBarrier;
import com.pewez777.fantastickits.config.FantasticKitsConfig;
import com.pewez777.fantastickits.kits.KitManager;
import com.pewez777.fantastickits.luckperms.LuckPermsHook;
import com.pewez777.fantastickits.network.NetworkHandler;
import com.pewez777.fantastickits.security.AuditLogger;
import com.pewez777.fantastickits.security.SecurityEventLogger;
import com.pewez777.fantastickits.storage.StoragePaths;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;

/**
 * Entry point for the proprietary Fantastic Kits mod.
 *
 * <p>Owner: Pewez777. This mod focuses EXCLUSIVELY on premium rank-kit
 * management with deep LuckPerms integration; it deliberately implements no
 * economy, crates, spawners, shops, teleports, claims, quests or social
 * systems.</p>
 */
@Mod(Reference.MOD_ID)
public final class FantasticKits {

    private static final Logger LOGGER = LogUtils.getLogger();

    public FantasticKits() {
        // Ensure the on-disk directory tree exists before the config is loaded.
        StoragePaths.ensureDirectories();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onCommonSetup);

        // Server-side configuration at config/fantastickits/config.toml.
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,
                FantasticKitsConfig.SPEC, "fantastickits/config.toml");

        // Forge event bus subscriptions (commands, lifecycle, command barrier).
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new CommandBarrier());

        printStartupBanner();
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        FKitsCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        StoragePaths.ensureDirectories();
        AuditLogger.initialize();
        SecurityEventLogger.initialize();

        // Soft-dependency detection: never crashes when LuckPerms is absent.
        LuckPermsHook.initialize();

        // Load persisted kits into memory.
        KitManager.get().reload();

        LOGGER.info("[F-Kits] Server systems ready. LuckPerms integration: {}.",
                LuckPermsHook.isAvailable() ? "ENABLED" : "DISABLED (rank features off)");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        KitManager.get().players().clearCache();
        LOGGER.info("[F-Kits] Server stopping - player cache cleared.");
    }

    private void printStartupBanner() {
        LOGGER.info("============================================================");
        LOGGER.info("  {} v{}", Reference.MOD_NAME, Reference.VERSION);
        LOGGER.info("  Author / Owner: {}", Reference.AUTHOR);
        LOGGER.info("  {}", Reference.COPYRIGHT);
        LOGGER.info("============================================================");
    }
}
