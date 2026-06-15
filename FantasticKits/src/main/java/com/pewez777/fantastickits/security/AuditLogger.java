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
 * The audit-log subsystem.
 *
 * <p>Records every administrative and claim action to an append-only file at
 * {@code config/fantastickits/audit/audit.log} (with size-based rotation) and,
 * optionally, to the server console. Logs are never modifiable from the GUI;
 * the server is the sole writer.</p>
 */
public final class AuditLogger {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static RotatingLogWriter writer;

    private AuditLogger() {
    }

    /** Initializes the underlying file writer. Safe to call on server start. */
    public static synchronized void initialize() {
        StoragePaths.ensureDirectories();
        writer = new RotatingLogWriter(StoragePaths.auditDir().resolve("audit.log"));
    }

    private static String safe(String value) {
        return (value == null || value.isEmpty()) ? "-" : value.replace('\n', ' ').replace('\r', ' ');
    }

    /**
     * Records an audit entry.
     *
     * @param action the audited action
     * @param uuid   acting player's UUID (may be {@code null} for console)
     * @param name   acting player's name
     * @param ip     player's IP if available and permitted, else {@code null}
     * @param kit    affected kit name
     * @param group  associated group
     * @param result outcome (e.g. SUCCESS / DENIED / FAILURE)
     * @param extra  additional information
     */
    public static void log(AuditAction action, UUID uuid, String name, String ip,
                           String kit, String group, String result, String extra) {
        if (!FantasticKitsConfig.AUDIT_LOG_ENABLED.get()) {
            return;
        }

        String ipField = FantasticKitsConfig.LOG_PLAYER_IP.get() ? safe(ip) : "(hidden)";
        String line = "[" + TS.format(LocalDateTime.now()) + "]"
                + " Player: " + safe(name)
                + " UUID: " + (uuid == null ? "-" : uuid)
                + " IP: " + ipField
                + " Action: " + (action == null ? "-" : action.name())
                + " Kit: " + safe(kit)
                + " Group: " + safe(group)
                + " Result: " + safe(result)
                + " Info: " + safe(extra);

        if (FantasticKitsConfig.AUDIT_LOG_CONSOLE.get()) {
            LOGGER.info("[F-Kits][AUDIT] {}", line);
        }
        if (FantasticKitsConfig.AUDIT_LOG_FILE.get() && writer != null) {
            writer.append(line, FantasticKitsConfig.AUDIT_MAX_FILE_SIZE_MB.get());
        }
    }
}
