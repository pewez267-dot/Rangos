package com.fantastic.kits.audit;

/**
 * Security-only event vocabulary. Strictly disjoint from {@link AuditEventType}
 * because security events follow a different review pipeline (dedicated file
 * and dedicated console tag {@code [SECURITY]}).
 */
public enum SecurityEventType {
    INVALID_GROUP_ACCESS,
    COMMAND_ACCESS_DENIED,
    DUPLICATE_CLAIM_ATTEMPT,
    INVALID_PACKET,
    KIT_DATA_TAMPERING,
    NBT_MANIPULATION_ATTEMPT,
    FORGED_CLIENT_ACTION,
    INVALID_GUI_INTERACTION,
    DESYNC_DETECTED,
    REPEATED_REQUEST_SPAM
}
