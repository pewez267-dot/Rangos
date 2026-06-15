package com.fantastic.kits.audit;

import com.fantastic.kits.kits.Kit;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable description of a single audited action. Built by callers, consumed
 * by {@link AuditLogger}. Construction is guarded so the recorded fields are
 * never null even when the originating player is the console.
 */
public final class AuditEvent {

    private final long timestamp;
    private final AuditEventType type;
    private final UUID playerId;
    private final String playerName;
    private final String address;
    private final String kitId;
    private final String kitName;
    private final String group;
    private final String result;
    private final String details;

    public AuditEvent(AuditEventType type, ServerPlayer player, Kit kit, String result, String details) {
        this.timestamp = Instant.now().toEpochMilli();
        this.type = type;
        if (player != null) {
            this.playerId = player.getUUID();
            this.playerName = player.getGameProfile().getName();
            String addr = player.connection != null && player.connection.connection != null
                    ? player.connection.connection.getRemoteAddress() != null
                        ? player.connection.connection.getRemoteAddress().toString()
                        : "" : "";
            // Strip the "/host:port" scheme into a stable host portion.
            this.address = addr.startsWith("/") ? addr.substring(1) : addr;
        } else {
            this.playerId = null;
            this.playerName = "CONSOLE";
            this.address = "local";
        }
        this.kitId = kit == null ? "" : kit.id();
        this.kitName = kit == null ? "" : kit.displayName();
        this.group = kit == null ? "" : kit.ownerGroup();
        this.result = result == null ? "" : result;
        this.details = details == null ? "" : details;
    }

    public long timestamp() { return timestamp; }
    public AuditEventType type() { return type; }
    public UUID playerId() { return playerId; }
    public String playerName() { return playerName; }
    public String address() { return address; }
    public String kitId() { return kitId; }
    public String kitName() { return kitName; }
    public String group() { return group; }
    public String result() { return result; }
    public String details() { return details; }
}
