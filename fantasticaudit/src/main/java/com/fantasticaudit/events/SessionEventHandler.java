package com.fantasticaudit.events;

import com.fantasticaudit.FantasticAudit;
import com.fantasticaudit.config.AuditConfig;
import com.fantasticaudit.logging.AliasTracker;
import com.fantasticaudit.logging.AuditLogger;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Captures the SESSION category: connect and disconnect.
 *
 * <p>Notes on Forge 1.20.1 limitations (documented rather than faked):</p>
 * <ul>
 *   <li><b>version</b>: the per-client protocol version is not exposed server-side through a
 *       stable public API. Because the client and server must share a protocol to connect, the
 *       server's game version ({@link SharedConstants}) is the correct negotiated value.</li>
 *   <li><b>brand</b>: the client brand ("fabric"/"forge"/"vanilla") arrives as a raw
 *       {@code minecraft:brand} custom payload and is not surfaced by any stable Forge API in
 *       1.20.1, so it is reported as {@code unknown} unless future detection is added.</li>
 *   <li><b>reason</b> on disconnect: {@code PlayerLoggedOutEvent} does not carry the disconnect
 *       reason in 1.20.1, so {@code quit} is reported as the default terminal reason.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = FantasticAudit.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SessionEventHandler {

    private SessionEventHandler() {
    }

    private static final ConcurrentHashMap<UUID, Long> SESSION_START_MILLIS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SESSION_START_MILLIS.put(player.getUUID(), System.currentTimeMillis());

        if (!AuditConfig.LOG_SESSIONS.get()) {
            return;
        }

        String name = player.getGameProfile().getName();

        // Detect a username change for this UUID and cross-link the old/new files.
        String previousName = AliasTracker.get().recordAndGetPrevious(player.getUUID(), name);
        if (previousName != null && !previousName.equals(name)) {
            AuditLogger.get().record(player.getUUID(), name, "NAME_CHANGE",
                    "previous=" + previousName + " uuid=" + player.getUUID());
            // Also drop a pointer into the old username's file so it links forward.
            AuditLogger.get().record(player.getUUID(), previousName, "NAME_CHANGE",
                    "renamed_to=" + name + " uuid=" + player.getUUID());
        }

        String ip = ResourcePackEventHandler.remoteAddress(player);
        String version = SharedConstants.getCurrentVersion().getName();

        // The player name is the file name; the UUID is written here (once per session) as the
        // stable forensic anchor so the username log can always be tied back to the real account.
        String data = "player=" + name
                + " uuid=" + player.getUUID()
                + " ip=" + ip
                + " ver=" + version
                + " brand=unknown";

        AuditLogger.get().record(player.getUUID(), name, "SESSION_START", data);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Long start = SESSION_START_MILLIS.remove(player.getUUID());

        if (!AuditConfig.LOG_SESSIONS.get()) {
            return;
        }

        long durationSeconds = start != null ? Math.max(0L, (System.currentTimeMillis() - start) / 1000L) : -1L;

        String data = "dur=" + durationSeconds + "s reason=quit";

        AuditLogger.get().record(player.getUUID(), player.getGameProfile().getName(), "SESSION_END", data);
    }
}
