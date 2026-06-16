package com.fantasticwatch.logging;

import com.fantasticwatch.config.WatchConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Central façade for the Fantastic Watch logging subsystem.
 *
 * <p>Every tracked event is written into the spawning operator's file
 * {@code config/fantasticwatch/ops/{opUUID}.log}. Each line is prefixed with an ISO-8601 UTC
 * timestamp (used by the weekly purge) followed by the event tag and payload:</p>
 *
 * <pre>[ISO-8601-UTC] [EVENT_TYPE] {event-specific data}</pre>
 */
public final class WatchLogger {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final WatchLogger INSTANCE = new WatchLogger();

    private static volatile Path systemLogPath;

    private volatile AsyncLogWriter writer;
    private volatile Path baseDir;
    private volatile Path opsDir;
    private volatile Path indexFile;

    private WatchLogger() {
    }

    public static WatchLogger get() {
        return INSTANCE;
    }

    public static Path systemLogPath() {
        return systemLogPath;
    }

    public synchronized void init(Path configDir) {
        if (writer != null) {
            return;
        }
        this.baseDir = configDir.resolve("fantasticwatch");
        this.opsDir = baseDir.resolve("ops");
        this.indexFile = baseDir.resolve("index.json");
        systemLogPath = baseDir.resolve("system.log");

        try {
            Files.createDirectories(opsDir);
        } catch (IOException e) {
            LOGGER.error("[FantasticWatch] Could not create ops directory {}", opsDir, e);
        }

        boolean async = WatchConfig.ASYNC_WRITE.get();
        int bufferSize = WatchConfig.BUFFER_SIZE.get();
        int flushInterval = WatchConfig.FLUSH_INTERVAL_SECONDS.get();

        AsyncLogWriter w = new AsyncLogWriter(async, bufferSize, flushInterval);
        w.start();
        this.writer = w;

        system("[WATCH_INIT] async=" + async + " buffer_size=" + bufferSize
                + " flush_interval_seconds=" + flushInterval
                + " cleanup_day=" + WatchConfig.cleanupDay());
        LOGGER.info("[FantasticWatch] Tracking logging initialised at {}", baseDir);
    }

    public boolean isReady() {
        return writer != null;
    }

    public Path opsDir() {
        return opsDir;
    }

    public Path baseDir() {
        return baseDir;
    }

    public Path indexFile() {
        return indexFile;
    }

    /** @return the on-disk log file for the given operator, named by username for readability. */
    public Path opsFile(String opName, UUID opUuid) {
        return opsDir.resolve(fileKey(opName, opUuid) + ".log");
    }

    /** @return a filesystem-safe key from the operator's username, or the UUID if the name is blank. */
    public static String fileKey(String opName, UUID opUuid) {
        if (opName != null) {
            String sanitized = opName.replaceAll("[^A-Za-z0-9_]", "_");
            if (!sanitized.isEmpty()) {
                return sanitized;
            }
        }
        return opUuid.toString();
    }

    /**
     * Records a tracked event into the spawning operator's log (named by username).
     *
     * @param opUuid    the original spawning operator (stable id; also encoded in every item uid)
     * @param opName    the spawning operator's username (used for the file name)
     * @param eventType the uppercase event tag, e.g. {@code ITEM_SPAWNED}
     * @param payload   the event-specific data (already formatted by the caller)
     */
    public void record(UUID opUuid, String opName, String eventType, String payload) {
        AsyncLogWriter w = this.writer;
        if (w == null || opUuid == null) {
            return;
        }
        String line = "[" + nowIso() + "] " + padEvent(eventType) + " | " + (payload == null ? "" : payload);
        w.append(opsFile(opName, opUuid), line);
    }

    /** Fixed width for the event-type column so payloads line up vertically. */
    private static final int EVENT_WIDTH = 16;

    private static String padEvent(String eventType) {
        if (eventType.length() >= EVENT_WIDTH) {
            return eventType;
        }
        return eventType + " ".repeat(EVENT_WIDTH - eventType.length());
    }

    /** Writes a line to {@code system.log} (init, cleanup, diagnostics). */
    public void system(String message) {
        AsyncLogWriter w = this.writer;
        Path path = systemLogPath;
        if (w == null || path == null) {
            return;
        }
        w.append(path, "[" + nowIso() + "] " + message);
    }

    public synchronized void shutdown() {
        AsyncLogWriter w = this.writer;
        if (w == null) {
            return;
        }
        system("[WATCH_SHUTDOWN] flushing pending tracking entries");
        w.stop();
        this.writer = null;
        LOGGER.info("[FantasticWatch] Tracking logging shut down cleanly");
    }

    /** ISO-8601 UTC, seconds precision, e.g. {@code 2025-01-15T14:32:07Z}. */
    public static String nowIso() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
