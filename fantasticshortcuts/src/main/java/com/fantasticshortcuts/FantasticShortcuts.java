package com.fantasticshortcuts;

import com.fantasticshortcuts.audit.AuditLogger;
import com.fantasticshortcuts.brigadier.ClientTreeModifier;
import com.fantasticshortcuts.commands.ShortcutExecutor;
import com.fantasticshortcuts.commands.ShortcutsCommand;
import com.fantasticshortcuts.config.ShortcutsConfig;
import com.fantasticshortcuts.data.ShortcutManager;
import com.fantasticshortcuts.gui.ModMenus;
import com.fantasticshortcuts.network.FSNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point for Fantastic Shortcuts.
 *
 * <p>Architecture: GUIs are client-side {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen}s
 * driven by {@link net.minecraft.world.inventory.AbstractContainerMenu} ({@code MenuType}),
 * opened from the server; every mutation and every shortcut execution is validated
 * server-side. Aliases run in the player's own command context — never as console or OP —
 * so permissions are identical to typing the original command.</p>
 */
@Mod(FantasticShortcuts.MOD_ID)
public final class FantasticShortcuts {

    public static final String MOD_ID = "fantasticshortcuts";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Vanilla permission level required to manage shortcuts (open GUI, create/edit/delete). */
    public static final int ADMIN_PERMISSION_LEVEL = 2;

    public FantasticShortcuts() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ShortcutsConfig.SPEC, "fantasticshortcuts/config.toml");
        ModMenus.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::onConfigLoad);
        modBus.addListener(this::onConfigReload);

        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(ShortcutExecutor::onCommand);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);

        LOGGER.info("[FantasticShortcuts] Inicializando Fantastic Shortcuts");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(FSNetwork::register);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(com.fantasticshortcuts.gui.ClientScreens::register);
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ShortcutsConfig.SPEC) {
            ShortcutsConfig.bake();
        }
    }

    private void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ShortcutsConfig.SPEC) {
            ShortcutsConfig.bake();
            if (ShortcutsConfig.cacheRefreshOnReload()) {
                ShortcutManager.get().reload();
            }
        }
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        ShortcutsCommand.register(event.getDispatcher());
    }

    private void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClientTreeModifier.sendModifiedTree(player);
        }
    }

    private void onPlayerRespawn(final PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClientTreeModifier.sendModifiedTree(player);
        }
    }

    private void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClientTreeModifier.sendModifiedTree(player);
        }
    }

    private void onServerStopping(final ServerStoppingEvent event) {
        AuditLogger.shutdown();
        ShortcutManager.get().shutdown();
    }
}
