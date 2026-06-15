package com.pewez.fantasticshortcuts.server;

import com.pewez.fantasticshortcuts.FantasticShortcuts;
import com.pewez.fantasticshortcuts.commands.FShortcutsCommand;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Eventos del servidor (bus de Forge).
 *
 * <ul>
 *     <li>{@code RegisterCommandsEvent}: registra {@code /fshortcuts} y reconstruye los nodos de
 *     todos los atajos en el dispatcher recién creado.</li>
 *     <li>{@code ServerStartingEvent}: asocia el servidor al manager para la sincronización en vivo.</li>
 *     <li>{@code ServerStoppingEvent}: libera el servidor y el estado de ocultación.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = FantasticShortcuts.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerEvents {

    private ServerEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        FShortcutsCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        ShortcutManager.get().attachServer(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ShortcutManager.get().shutdown();
    }
}
