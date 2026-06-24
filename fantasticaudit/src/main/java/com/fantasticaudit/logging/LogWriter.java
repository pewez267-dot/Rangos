package com.fantasticaudit.logging;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The asynchronous, thread-safe disk-writing engine for Fantastic Audit.
 *
 * <p>Design: a single dedicated writer thread owns all buffer state, so concurrent events
 * from many players can never corrupt a file. Producers (game-thread event handlers) only
 * ever touch a lock-free {@link LinkedBlockingQueue}; they never touch the file system and
 * therefore never block the server thread on I/O.</p>
 *
 * <p>Lines are coalesced into a per-file in-memory buffer and flushed when either the global
 * buffered-line count reaches {@code bufferSize} or {@code flushIntervalSeconds} elapses,
 * whichever comes first. On JVM/server shutdown {@link #stop()} drains the queue and performs
 * a final flush so no data is lost.</p>
 *
 * <p>When {@code async} is {@code false} the engine writes synchronously under a single lock.
 * That path is still fully thread-safe; it simply trades throughput for immediacy.</p>
 */
public final class LogWriter {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** A single queued append: a target file and the already-formatted line (without newline). */
    private record WriteTask(Path path, String line) {
    }

    private final boolean async;
    private final int bufferSize;
    private final long flushIntervalMillis;

    /** Used only when {@code async == false} to serialise synchronous writes. */
    private final Object syncLock = new Object();

    private final BlockingQueue<WriteTask> queue = new LinkedBlockingQueue<>();

    // The following state is owned exclusively by the worker thread (async mode only).
    private final Map<Path, StringBuilder> buffers = new HashMap<>();
    private int bufferedLines;
    private long lastFlush;

    private volatile boolean running;
    private Thread worker;

    public LogWriter(boolean async, int bufferSize, long flushIntervalSeconds) {
        this.async = async;
        this.bufferSize = Math.max(1, bufferSize);
        this.flushIntervalMillis = Math.max(1L, flushIntervalSeconds) * 1000L;
    }

    /** Starts the background writer thread (no-op in synchronous mode). */
    public synchronized void start() {
        if (!async || running) {
            return;
        }
        running = true;
        lastFlush = System.currentTimeMillis();
        worker = new Thread(this::runLoop, "FantasticAudit-LogWriter");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Queues a single line to be appended to {@code path}. Non-blocking in async mode.
     *
     * @param path the destination log file
     * @param line the formatted line (a trailing newline is added automatically)
     */
    public void append(Path path, String line) {
        if (path == null || line == null) {
            return;
        }
        if (async) {
            // offer() on an unbounded LinkedBlockingQueue always succeeds and never blocks.
            queue.offer(new WriteTask(path, line));
        } else {
            synchronized (syncLock) {
                writeToDisk(path, line + System.lineSeparator());
            }
        }
    }

    private void runLoop() {
        try {
            while (running || !queue.isEmpty()) {
                WriteTask task = queue.poll(250, TimeUnit.MILLISECONDS);
                if (task != null) {
                    StringBuilder sb = buffers.computeIfAbsent(task.path(), p -> new StringBuilder());
                    sb.append(task.line()).append(System.lineSeparator());
                    bufferedLines++;
                }
                long now = System.currentTimeMillis();
                if (bufferedLines >= bufferSize || (now - lastFlush) >= flushIntervalMillis) {
                    flushBuffers();
                }
            }
        } catch (InterruptedException e) {
            // Shutdown signal: stop the loop but preserve interrupt status for the JVM.
            Thread.currentThread().interrupt();
        } finally {
            // Always drain anything still queued, then flush, so shutdown never loses data.
            drainQueueIntoBuffers();
            flushBuffers();
        }
    }

    private void drainQueueIntoBuffers() {
        WriteTask task;
        while ((task = queue.poll()) != null) {
            StringBuilder sb = buffers.computeIfAbsent(task.path(), p -> new StringBuilder());
            sb.append(task.line()).append(System.lineSeparator());
            bufferedLines++;
        }
    }

    /** Flushes every buffered file to disk. Only ever called from the worker thread. */
    private void flushBuffers() {
        if (!buffers.isEmpty()) {
            for (Map.Entry<Path, StringBuilder> entry : buffers.entrySet()) {
                StringBuilder content = entry.getValue();
                if (content.length() == 0) {
                    continue;
                }
                writeToDisk(entry.getKey(), content.toString());
                content.setLength(0);
            }
        }
        bufferedLines = 0;
        lastFlush = System.currentTimeMillis();
    }

    /**
     * Appends raw text to a file, creating parent directories and the file as needed.
     * Failures are reported to the system log instead of being thrown, so a single bad write
     * can never crash the server or stall the writer loop.
     */
    private void writeToDisk(Path path, String text) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, text.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            reportWriteFailure(path, e);
        }
    }

    /**
     * Best-effort error reporting. We deliberately avoid recursing through the buffered path:
     * the system log is written directly here, and if even that fails we fall back to the
     * mod logger so the failure is never silently swallowed.
     */
    private void reportWriteFailure(Path failedPath, IOException cause) {
        LOGGER.error("[FantasticAudit] Failed to write audit log file {}", failedPath, cause);
        Path systemLog = AuditLogger.systemLogPath();
        if (systemLog == null || systemLog.equals(failedPath)) {
            return;
        }
        try {
            Path parent = systemLog.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String line = "[" + Instant.now() + "] [WRITE_ERROR] file=" + failedPath
                    + " error=" + cause.getClass().getSimpleName() + ":" + cause.getMessage()
                    + System.lineSeparator();
            Files.write(systemLog, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException nested) {
            LOGGER.error("[FantasticAudit] Failed to write to audit_system.log as well", nested);
        }
    }

    /**
     * Stops the writer, draining and flushing everything still buffered or queued.
     * Safe to call multiple times and from any thread.
     */
    public synchronized void stop() {
        if (!async) {
            return;
        }
        running = false;
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            worker = null;
        }
    }
}
