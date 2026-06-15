package com.pewez.fantasticshortcuts;

import com.mojang.brigadier.CommandDispatcher;
import com.pewez.fantasticshortcuts.audit.AuditLog;
import com.pewez.fantasticshortcuts.brigadier.ShortcutCommandRegistrar;
import com.pewez.fantasticshortcuts.commands.FShortcutsCommand;
import com.pewez.fantasticshortcuts.config.ModConfig;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Fantastic Shortcuts - advanced, fully editable global command shortcuts for Forge 1.20.1.
 *
 * Server-side administrative mod. It translates short aliases into real game commands while strictly
 * respecting the existing permission system (vanilla / mods / LuckPerms). It never elevates
 * permissions and never runs commands as the console.
 */
@Mod(FantasticShortcutsMod.MOD_ID)
@Mod.EventBusSubscriber(modid = FantasticShortcutsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FantasticShortcutsMod {

    public static final String MOD_ID = "fantasticshortcuts";
    public static final Logger LOGGER = LoggerFactory.getLogger("FantasticShortcuts");

    private static CommandDispatcher<CommandSourceStack> dispatcher;
    private static boolean initialised;

    public FantasticShortcutsMod() {
        // config/fantasticshortcuts/config.toml
        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC, MOD_ID + "/config.toml");
        LOGGER.info("Fantastic Shortcuts constructing");
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get().resolve(MOD_ID);
    }

    private static void ensureInitialised() {
        if (initialised) {
            return;
        }
        Path dir = configDir();
        AuditLog.init(dir);
        ShortcutManager.get().init(dir);
        initialised = true;
        LOGGER.info("Fantastic Shortcuts initialised with {} shortcuts", ShortcutManager.get().all().size());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ensureInitialised();
        dispatcher = event.getDispatcher();
        // Reload from disk so edits to shortcuts.json apply on server start and on /reload.
        ShortcutManager.get().reload();
        ShortcutCommandRegistrar.registerAll(dispatcher);
        FShortcutsCommand.register(dispatcher);
    }

    /**
     * Register newly added shortcuts into the live dispatcher and resync the command tree to all
     * connected players, so runtime changes take effect without a restart.
     */
    public static void liveSync(MinecraftServer server) {
        if (dispatcher == null || server == null) {
            return;
        }
        ShortcutCommandRegistrar.registerMissing(dispatcher);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    public static CommandDispatcher<CommandSourceStack> dispatcher() {
        return dispatcher;
    }
}
