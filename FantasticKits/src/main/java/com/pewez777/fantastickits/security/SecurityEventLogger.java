/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.security;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import com.pewez777.fantastickits.config.FantasticKitsConfig;
import com.pewez777.fantastickits.storage.StoragePaths;

import org.slf4j.Logger;

/**
 * The specialized security-events subsystem.
 *
 * <p>Every suspicious or blocked action is recorded automatically to
 * {@code config/fantastickits/audit/security.log} (append-only, rotating) and,
 * optionally, the console. This lets the server owner investigate exploit
 * attempts, improper claims, tampering and unauthorized access. The standard
 * response to any suspicious event is: cancel the action, record audit + this
 * security event, notify the console, and never crash.</p>
 */
public final class SecurityEventLogger {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static RotatingLogWriter writer;

    private SecurityEventLogger() {
    }

    public static synchronized void initialize() {
        StoragePaths.ensureDirectories();
        writer = new RotatingLogWriter(StoragePaths.auditDir().resolve("security.log"));
    }

    private static String safe(String value) {
        return (value == null || value.isEmpty()) ? "-" : value.replace('\n', ' ').replace('\r', ' ');
    }

    /**
     * Records a security event.
     *
     * @param type            the security event type
     * @param uuid            offending player's UUID (nullable)
     * @param name            offending player's name
     * @param detectedGroup   the player's actual detected group
     * @param requiredGroup   the group that would have been required
     * @param kit             affected kit name
     * @param attemptedAction short description of the attempted action
     * @param result          outcome (typically BLOCKED)
     * @param reason          human-readable reason for the block
     */
    public static void log(SecurityEventType type, UUID uuid, String name,
                           String detectedGroup, String requiredGroup, String kit,
                           String attemptedAction, String result, String reason) {
        if (!FantasticKitsConfig.SECURITY_EVENTS_ENABLED.get()) {
            return;
        }

        String line = "[SECURITY] [" + TS.format(LocalDateTime.now()) + "]"
                + " Player: " + safe(name)
                + " UUID: " + (uuid == null ? "-" : uuid)
                + " Action: " + (type == null ? "-" : type.name())
                + " Kit: " + safe(kit)
                + " PlayerGroup: " + safe(detectedGroup)
                + " RequiredGroup: " + safe(requiredGroup)
                + " Attempted: " + safe(attemptedAction)
                + " Result: " + safe(result)
                + " Reason: " + safe(reason);

        if (FantasticKitsConfig.SECURITY_EVENTS_CONSOLE.get()) {
            LOGGER.warn("[F-Kits][SECURITY] {}", line);
        }
        if (FantasticKitsConfig.SECURITY_EVENTS_FILE.get() && writer != null) {
            writer.append(line, FantasticKitsConfig.SECURITY_MAX_FILE_SIZE_MB.get());
        }
    }
}
