/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.security;

/**
 * The complete, fixed catalogue of security-relevant events tracked by the
 * specialized security subsystem.
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
