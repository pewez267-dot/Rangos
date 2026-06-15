package com.pewez.fantasticshortcuts.server;

import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import com.pewez.fantasticshortcuts.audit.AuditEvent;
import com.pewez.fantasticshortcuts.audit.AuditLog;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server lifecycle hooks: persistence and audit bookkeeping.
 */
@Mod.EventBusSubscriber(modid = FantasticShortcutsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerLifecycle {

    private ServerLifecycle() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        AuditLog.record(AuditEvent.EXECUTE_SHORTCUT, "system",
                "Server started with " + ShortcutManager.get().all().size() + " shortcuts loaded");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ShortcutManager.get().save();
    }
}
