package com.fantastickits;

import com.fantastickits.commands.FKitsCommand;
import com.fantastickits.config.FKConfig;
import com.fantastickits.data.GroupCommandStore;
import com.fantastickits.integration.LuckPermsIntegration;
import com.fantastickits.network.FKNetwork;
import com.fantastickits.security.CommandGuard;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point for Fantastic Kits.
 *
 * <p>Architecture mirrors the FantasticCrates / FantasticSpawners family: GUIs are
 * client-side {@code Screen}s opened by a server packet; every mutation is validated
 * and persisted server-side; the client is never trusted as a source of truth.</p>
 */
@Mod(FantasticKits.MOD_ID)
public final class FantasticKits {

    public static final String MOD_ID = "fantastickits";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FantasticKits() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // General config -> config/fantastickits/config.toml (global COMMON config so it
        // lives in the config/ directory rather than per-world serverconfig/).
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FKConfig.SPEC, "fantastickits/config.toml");
        modBus.addListener(this::onConfigLoad);
        modBus.addListener(this::onConfigReload);
        modBus.addListener(this::commonSetup);

        // Server-side gameplay hooks live on the Forge event bus.
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(CommandGuard::onCommand);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);

        LOGGER.info("[FantasticKits] Inicializando Fantastic Kits");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(FKNetwork::register);
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == FKConfig.SPEC) {
            FKConfig.bake();
        }
    }

    private void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == FKConfig.SPEC) {
            FKConfig.bake();
        }
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        FKitsCommand.register(event.getDispatcher());
    }

    /**
     * On server start, push the stored per-group command lists into LuckPerms so the permission
     * nodes stay consistent with {@code group_commands.json} after restarts or manual edits.
     * Only our {@code fantastickits.command.*} namespace is touched.
     */
    private void onServerStarted(final ServerStartedEvent event) {
        if (!FKConfig.manageLuckPermsPermissions() || !LuckPermsIntegration.isAvailable()) {
            return;
        }
        final GroupCommandStore store = GroupCommandStore.get();
        for (final String group : store.allGroups()) {
            LuckPermsIntegration.syncGroupCommandNodes(group, store.commandsFor(group));
        }
        LOGGER.info("[FantasticKits] Permisos de comandos sincronizados con LuckPerms.");
    }
}
