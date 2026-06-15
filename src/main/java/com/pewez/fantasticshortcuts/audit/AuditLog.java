package com.pewez.fantasticshortcuts.audit;

import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import com.pewez.fantasticshortcuts.config.ModConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Append-only audit log. One file per day under {@code config/fantasticshortcuts/audit/}.
 */
public final class AuditLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static Path auditDir;

    private AuditLog() {
    }

    public static void init(Path configDir) {
        auditDir = configDir.resolve("audit");
        try {
            Files.createDirectories(auditDir);
        } catch (IOException e) {
            FantasticShortcutsMod.LOGGER.error("Could not create audit directory", e);
        }
    }

    public static void record(AuditEvent event, String actor, String details) {
        if (ModConfig.AUDIT_ENABLED != null && !ModConfig.AUDIT_ENABLED.get()) {
            return;
        }
        if (auditDir == null) {
            return;
        }
        String line = "[" + LocalDateTime.now().format(TIME) + "] " + event.name()
                + " | actor=" + safe(actor) + " | " + safe(details) + System.lineSeparator();
        Path file = auditDir.resolve("audit-" + LocalDate.now() + ".log");
        try {
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            FantasticShortcutsMod.LOGGER.error("Failed to write audit entry", e);
        }
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ');
    }
}
