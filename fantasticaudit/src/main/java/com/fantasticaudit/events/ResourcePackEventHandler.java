package com.fantasticaudit.events;

import com.fantasticaudit.FantasticAudit;
import com.fantasticaudit.config.AuditConfig;
import com.fantasticaudit.logging.AuditLogger;
import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.net.SocketAddress;

/**
 * Captures the RESOURCE PACK category.
 *
 * <p><b>API note (Forge 1.20.1):</b> Forge does not expose a server-side resource-pack-status
 * event (the {@code PlayerResourcePackStatusEvent} that exists in Bukkit/Paper has no Forge
 * equivalent here). The "closest available" mechanism is the vanilla client→server packet
 * {@link ServerboundResourcePackPacket}. We intercept it with a Netty inbound handler installed
 * into the player's connection pipeline on login. This is pure Forge/Netty — no mixins, no
 * coremods — and degrades gracefully (logs a diagnostic and continues) if the channel cannot be
 * reached on an exotic platform.</p>
 *
 * <p>The connection's {@link Connection} and its {@link Channel} are obtained by <em>type</em>
 * via reflection rather than by hard-coded field names, so this remains correct regardless of
 * mapping/field-visibility differences and never risks a compile-time coupling to a private name.</p>
 */
@Mod.EventBusSubscriber(modid = FantasticAudit.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ResourcePackEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String HANDLER_NAME = "fantasticaudit_rp_sniffer";
    private static final String VANILLA_HANDLER_NAME = "packet_handler";

    private ResourcePackEventHandler() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!AuditConfig.LOG_RESOURCE_PACKS.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        installSniffer(player);
    }

    private static void installSniffer(ServerPlayer player) {
        Connection connection = connectionOf(player);
        if (connection == null) {
            AuditLogger.get().system("[RP_SNIFFER] could not access Connection for " + player.getUUID());
            return;
        }
        Channel channel = channelOf(connection);
        if (channel == null) {
            AuditLogger.get().system("[RP_SNIFFER] could not access Netty Channel for " + player.getUUID());
            return;
        }
        // Pipeline mutation must occur on the channel's own event loop to stay thread-safe.
        channel.eventLoop().execute(() -> {
            try {
                ChannelPipeline pipeline = channel.pipeline();
                if (pipeline.get(HANDLER_NAME) != null) {
                    return; // already installed for this connection
                }
                ResourcePackSniffer sniffer = new ResourcePackSniffer(player);
                if (pipeline.get(VANILLA_HANDLER_NAME) != null) {
                    pipeline.addBefore(VANILLA_HANDLER_NAME, HANDLER_NAME, sniffer);
                } else {
                    pipeline.addLast(HANDLER_NAME, sniffer);
                }
            } catch (RuntimeException e) {
                AuditLogger.get().system("[RP_SNIFFER] failed to install handler: " + e.getMessage());
                LOGGER.warn("[FantasticAudit] Failed to install resource-pack sniffer", e);
            }
        });
    }

    /**
     * Logs a captured resource-pack response. Invoked from the Netty thread; all logging is
     * enqueued asynchronously so this never blocks the network pipeline.
     */
    private static void handleResourcePackResponse(ServerPlayer player, ServerboundResourcePackPacket packet) {
        ServerboundResourcePackPacket.Action action = packet.getAction();
        String mappedAction = mapAction(action);

        String serverHash = AuditConfig.SERVER_RESOURCE_PACK_HASH.get();
        boolean serverRequiresPack = serverHash != null && !serverHash.isEmpty();

        // Xray-suspicion logic with what the client response packet actually exposes:
        //   - DECLINED / FAILED_DOWNLOAD while the server enforces a pack => suspicious.
        // Per-client pack hash comparison and "multiple packs active" are NOT carried by this
        // packet in 1.20.1 and therefore cannot be evaluated here; this is documented rather
        // than faked. The configured server hash is recorded for forensic cross-referencing.
        boolean suspicious = serverRequiresPack
                && (action == ServerboundResourcePackPacket.Action.DECLINED
                || action == ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD);

        String data = "action={" + mappedAction + "}"
                + " pack_hash={" + (serverHash == null ? "" : serverHash) + "}"
                + " pack_url={}"
                + " client_response={" + action.name() + "}"
                + " suspicious={" + suspicious + "}";

        AuditLogger.get().record(player.getUUID(), player.getGameProfile().getName(), "RESOURCE_PACK", data);
    }

    private static String mapAction(ServerboundResourcePackPacket.Action action) {
        return switch (action) {
            case ACCEPTED -> "accepted";
            case DECLINED -> "declined";
            case SUCCESSFULLY_LOADED -> "loaded";
            case FAILED_DOWNLOAD -> "failed";
        };
    }

    /**
     * Resolves the player's connection IP. Shared with {@link SessionEventHandler} so the IP is
     * captured consistently in one place.
     *
     * @return the remote address string, or {@code unknown} if it cannot be resolved
     */
    public static String remoteAddress(ServerPlayer player) {
        Connection connection = connectionOf(player);
        if (connection == null) {
            return "unknown";
        }
        SocketAddress address = connection.getRemoteAddress();
        return address != null ? address.toString() : "unknown";
    }

    // --- Reflection helpers (resolve by field TYPE, never by name) -----------------------------

    private static Connection connectionOf(ServerPlayer player) {
        ServerGamePacketListenerImpl listener = player.connection;
        if (listener == null) {
            return null;
        }
        Object value = findFieldValueByType(listener, Connection.class);
        return value instanceof Connection connection ? connection : null;
    }

    private static Channel channelOf(Connection connection) {
        Object value = findFieldValueByType(connection, Channel.class);
        return value instanceof Channel channel ? channel : null;
    }

    /**
     * Walks the class hierarchy of {@code target} and returns the value of the first field whose
     * declared type is assignable to {@code type}. Returns {@code null} when no such accessible
     * field exists.
     */
    private static Object findFieldValueByType(Object target, Class<?> type) {
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (type.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        return field.get(target);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        LOGGER.debug("[FantasticAudit] Could not read field {} of {}",
                                field.getName(), current.getName(), e);
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * The Netty inbound handler that observes (never modifies/cancels) resource-pack responses.
     */
    private static final class ResourcePackSniffer extends ChannelInboundHandlerAdapter {
        private final ServerPlayer player;

        private ResourcePackSniffer(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof ServerboundResourcePackPacket packet) {
                try {
                    handleResourcePackResponse(player, packet);
                } catch (RuntimeException e) {
                    // Never let auditing break the network read path; report and continue.
                    AuditLogger.get().system("[RP_SNIFFER] error handling response: " + e.getMessage());
                    LOGGER.warn("[FantasticAudit] Error handling resource-pack response", e);
                }
            }
            // Always forward the packet so vanilla handling proceeds untouched.
            super.channelRead(ctx, msg);
        }
    }
}
