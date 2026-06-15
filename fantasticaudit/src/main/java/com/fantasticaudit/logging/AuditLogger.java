package com.fantasticaudit.logging;

import com.fantasticaudit.config.AuditConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Central façade for the Fantastic Audit logging subsystem.
 *
 * <p>This is the single entry point used by every event handler. It owns the
 * {@link LogWriter} (the async engine), knows the on-disk layout, and formats every line
 * exactly as the audit specification requires:</p>
 *
 * <pre>[ISO-8601-UTC] [EVENT_TYPE] jugador={name} uuid={UUID} | {event-specific data}</pre>
 *
 * <p>All methods are no-ops until {@link #init(Path)} has run (which happens on
 * {@code ServerStartingEvent}). That makes the handlers safe to register at any time.</p>
 */
public final class AuditLogger {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AuditLogger INSTANCE = new AuditLogger();

    /** Static so {@link LogWriter} can target the system log when reporting its own failures. */
    private static volatile Path systemLogPath;

    private volatile LogWriter writer;
    private volatile Path playersDir;

    private AuditLogger() {
    }

    public static AuditLogger get() {
        return INSTANCE;
    }

    /** @return the audit system log path, or {@code null} before {@link #init(Path)}. */
    public static Path systemLogPath() {
        return systemLogPath;
    }

    /**
     * Initialises the directory layout and starts the writer. Idempotent: a second call while
     * already running is ignored.
     *
     * @param configDir the server config directory (typically {@code config/})
     */
    public synchronized void init(Path configDir) {
        if (writer != null) {
            return;
        }
        Path baseDir = configDir.resolve("fantasticaudit");
        this.playersDir = baseDir.resolve("logs").resolve("players");
        systemLogPath = baseDir.resolve("audit_system.log");

        try {
            Files.createDirectories(playersDir);
        } catch (IOException e) {
            LOGGER.error("[FantasticAudit] Could not create log directory {}", playersDir, e);
        }

        boolean async = AuditConfig.ASYNC_WRITE.get();
        int bufferSize = AuditConfig.BUFFER_SIZE.get();
        int flushInterval = AuditConfig.FLUSH_INTERVAL_SECONDS.get();

        LogWriter w = new LogWriter(async, bufferSize, flushInterval);
        w.start();
        this.writer = w;

        system("[AUDIT_INIT] async=" + async + " buffer_size=" + bufferSize
                + " flush_interval_seconds=" + flushInterval
                + " retention_days=" + AuditConfig.LOG_RETENTION_DAYS.get());
        LOGGER.info("[FantasticAudit] Audit logging initialised at {}", baseDir);
    }

    /** @return the retention period for log lines, in days. */
    public int retentionDays() {
        return AuditConfig.LOG_RETENTION_DAYS.get();
    }

    /** @return the directory holding per-player log files, or {@code null} before init. */
    public Path playersDir() {
        return playersDir;
    }

    /** @return {@code true} once {@link #init(Path)} has successfully started the writer. */
    public boolean isReady() {
        return writer != null;
    }

    /**
     * Records a single audit entry into the player's {@code {UUID}.log} file.
     *
     * @param uuid       the acting player's UUID (file key)
     * @param playerName the acting player's display name
     * @param eventType  the uppercase event tag, e.g. {@code BLOCK_BREAK}
     * @param data       the event-specific payload (already formatted by the caller)
     */
    public void record(UUID uuid, String playerName, String eventType, String data) {
        LogWriter w = this.writer;
        if (w == null || uuid == null) {
            return;
        }
        String line = "[" + nowIso() + "] [" + eventType + "] jugador=" + safe(playerName)
                + " uuid=" + uuid + " | " + (data == null ? "" : data);
        w.append(playersFile(uuid), line);
    }

    /**
     * Writes a line to the audit system log ({@code audit_system.log}). Used for init, cleanup
     * and internal diagnostics rather than per-player gameplay events.
     *
     * @param message the message body (a leading timestamp is added automatically)
     */
    public void system(String message) {
        LogWriter w = this.writer;
        Path path = systemLogPath;
        if (w == null || path == null) {
            return;
        }
        w.append(path, "[" + nowIso() + "] " + message);
    }

    /** Flushes and stops the writer. Called on {@code ServerStoppingEvent}. */
    public synchronized void shutdown() {
        LogWriter w = this.writer;
        if (w == null) {
            return;
        }
        system("[AUDIT_SHUTDOWN] flushing pending audit entries");
        w.stop();
        this.writer = null;
        LOGGER.info("[FantasticAudit] Audit logging shut down cleanly");
    }

    private Path playersFile(UUID uuid) {
        return playersDir.resolve(uuid.toString() + ".log");
    }

    /** ISO-8601 UTC, seconds precision, e.g. {@code 2025-01-15T14:32:07Z}. */
    private static String nowIso() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private static String safe(String name) {
        return name == null || name.isEmpty() ? "unknown" : name;
    }
}
