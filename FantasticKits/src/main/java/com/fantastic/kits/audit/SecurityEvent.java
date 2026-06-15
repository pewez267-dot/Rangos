package com.fantastic.kits.audit;

import com.fantastic.kits.kits.Kit;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable security event. The shape mirrors {@link AuditEvent} but adds the
 * {@code playerGroup}/{@code requiredGroup}/{@code reason} fields demanded by
 * the spec to investigate access violations.
 */
public final class SecurityEvent {

    private final long timestamp;
    private final SecurityEventType type;
    private final UUID playerId;
    private final String playerName;
    private final String playerGroup;
    private final String requiredGroup;
    private final String kitId;
    private final String kitName;
    private final String attemptedAction;
    private final String result;
    private final String reason;

    public SecurityEvent(SecurityEventType type, ServerPlayer player, String playerGroup,
                         String requiredGroup, Kit kit, String attemptedAction,
                         String result, String reason) {
        this.timestamp = Instant.now().toEpochMilli();
        this.type = type;
        if (player != null) {
            this.playerId = player.getUUID();
            this.playerName = player.getGameProfile().getName();
        } else {
            this.playerId = null;
            this.playerName = "UNKNOWN";
        }
        this.playerGroup = playerGroup == null ? "" : playerGroup;
        this.requiredGroup = requiredGroup == null ? "" : requiredGroup;
        this.kitId = kit == null ? "" : kit.id();
        this.kitName = kit == null ? "" : kit.displayName();
        this.attemptedAction = attemptedAction == null ? "" : attemptedAction;
        this.result = result == null ? "BLOCKED" : result;
        this.reason = reason == null ? "" : reason;
    }

    public long timestamp() { return timestamp; }
    public SecurityEventType type() { return type; }
    public UUID playerId() { return playerId; }
    public String playerName() { return playerName; }
    public String playerGroup() { return playerGroup; }
    public String requiredGroup() { return requiredGroup; }
    public String kitId() { return kitId; }
    public String kitName() { return kitName; }
    public String attemptedAction() { return attemptedAction; }
    public String result() { return result; }
    public String reason() { return reason; }
}
