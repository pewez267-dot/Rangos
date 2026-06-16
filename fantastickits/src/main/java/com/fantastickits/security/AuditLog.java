package com.fantastickits.security;

import com.fantastickits.FantasticKits;
import com.fantastickits.config.FKConfig;
import com.fantastickits.data.DataPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Append-only, thread-safe audit trail written to {@code config/fantastickits/audit.log}.
 *
 * <p>Every security-relevant event is recorded on a single line with an ISO-8601
 * timestamp, the acting player's UUID and name, the kit involved (when any) and the
 * result. Writes are serialised on a private lock so concurrent server threads never
 * interleave a line.</p>
 */
public final class AuditLog {

    /** Result markers used in the {@code result=} field. */
    public static final String OK = "OK";
    public static final String DENIED = "DENIED";

    /** Denial reasons (also used in messages to the player). */
    public static final String REASON_ALREADY_CLAIMED = "already_claimed";
    public static final String REASON_WRONG_GROUP = "wrong_group";
    public static final String REASON_NO_KIT = "kit_not_found";
    public static final String REASON_NO_GROUP_ASSIGNED = "kit_has_no_group";

    private static final Object LOCK = new Object();

    private AuditLog() {
    }

    private static String now() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    private static String safe(final String value) {
        if (value == null) {
            return "-";
        }
        // Keep each record on a single, parse-friendly line.
        return value.replace('\n', ' ').replace('\r', ' ').replace('|', '/');
    }

    /** Low-level writer. All convenience methods funnel through here. */
    public static void log(final String action,
                           final UUID uuid,
                           final String playerName,
                           final String kit,
                           final String result,
                           final String detail) {
        if (!FKConfig.auditLogEnabled()) {
            return;
        }
        final String line = now()
                + " | action=" + safe(action)
                + " | player=" + safe(playerName)
                + " | uuid=" + (uuid == null ? "-" : uuid.toString())
                + " | kit=" + safe(kit)
                + " | result=" + safe(result)
                + " | detail=" + safe(detail)
                + System.lineSeparator();
        synchronized (LOCK) {
            try {
                final Path path = DataPaths.auditLog();
                Files.write(path, line.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (final Exception e) {
                FantasticKits.LOGGER.error("[FantasticKits] No se pudo escribir en audit.log: {}", e.toString());
            }
        }
    }

    // ---- Convenience methods, one per required event -------------------------

    public static void kitCreated(final UUID uuid, final String name, final String kit) {
        log("KIT_CREATE", uuid, name, kit, OK, "kit creado/abierto en editor");
    }

    public static void kitEdited(final UUID uuid, final String name, final String kit) {
        log("KIT_EDIT", uuid, name, kit, OK, "kit guardado desde el editor");
    }

    public static void kitDeleted(final UUID uuid, final String name, final String kit, final boolean existed) {
        log("KIT_DELETE", uuid, name, kit, existed ? OK : DENIED, existed ? "kit eliminado" : "kit inexistente");
    }

    public static void claimSuccess(final UUID uuid, final String name, final String kit) {
        log("KIT_CLAIM", uuid, name, kit, OK, "items entregados");
    }

    public static void claimDenied(final UUID uuid, final String name, final String kit, final String reason) {
        log("KIT_CLAIM", uuid, name, kit, DENIED, reason);
    }

    public static void adminGet(final UUID adminUuid, final String adminName, final String targetName, final String kit) {
        log("KIT_GET_ADMIN", adminUuid, adminName, kit, OK, "reposicion manual a " + safe(targetName));
    }

    public static void kitTested(final UUID uuid, final String name, final String kit) {
        log("KIT_TEST", uuid, name, kit, OK, "prueba de kit (sin afectar reclamo)");
    }

    public static void commandAllowed(final UUID uuid, final String name, final String command, final String group) {
        log("COMMAND_ALLOW", uuid, name, "-", OK, "/" + safe(command) + " via grupo " + safe(group));
    }

    public static void commandBlocked(final UUID uuid, final String name, final String command) {
        log("COMMAND_BLOCK", uuid, name, "-", DENIED, "/" + safe(command) + " no permitido para el rango del jugador");
    }
}
