package com.fantasticwatch.logging;

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
 * Asynchronous, thread-safe disk-writing engine for Fantastic Watch.
 *
 * <p>A single dedicated thread owns all buffer state, so concurrent tracking events from many
 * operators and players can never corrupt a log file. Producers only ever push to a lock-free
 * queue and therefore never block the server thread on I/O. Lines are coalesced per-file and
 * flushed when the buffered-line count reaches {@code bufferSize} or {@code flushIntervalSeconds}
 * elapses. {@link #stop()} drains and flushes on shutdown so nothing is lost.</p>
 */
public final class AsyncLogWriter {

    private static final Logger LOGGER = LogUtils.getLogger();

    private record WriteTask(Path path, String line) {
    }

    private final boolean async;
    private final int bufferSize;
    private final long flushIntervalMillis;

    private final Object syncLock = new Object();
    private final BlockingQueue<WriteTask> queue = new LinkedBlockingQueue<>();

    // Owned exclusively by the worker thread (async mode only).
    private final Map<Path, StringBuilder> buffers = new HashMap<>();
    private int bufferedLines;
    private long lastFlush;

    private volatile boolean running;
    private Thread worker;

    public AsyncLogWriter(boolean async, int bufferSize, long flushIntervalSeconds) {
        this.async = async;
        this.bufferSize = Math.max(1, bufferSize);
        this.flushIntervalMillis = Math.max(1L, flushIntervalSeconds) * 1000L;
    }

    public synchronized void start() {
        if (!async || running) {
            return;
        }
        running = true;
        lastFlush = System.currentTimeMillis();
        worker = new Thread(this::runLoop, "FantasticWatch-LogWriter");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Queues a single line to be appended to {@code path}. Non-blocking in async mode.
     */
    public void append(Path path, String line) {
        if (path == null || line == null) {
            return;
        }
        if (async) {
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
                    buffers.computeIfAbsent(task.path(), p -> new StringBuilder())
                            .append(task.line()).append(System.lineSeparator());
                    bufferedLines++;
                }
                long now = System.currentTimeMillis();
                if (bufferedLines >= bufferSize || (now - lastFlush) >= flushIntervalMillis) {
                    flushBuffers();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            drainQueueIntoBuffers();
            flushBuffers();
        }
    }

    private void drainQueueIntoBuffers() {
        WriteTask task;
        while ((task = queue.poll()) != null) {
            buffers.computeIfAbsent(task.path(), p -> new StringBuilder())
                    .append(task.line()).append(System.lineSeparator());
            bufferedLines++;
        }
    }

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

    private void reportWriteFailure(Path failedPath, IOException cause) {
        LOGGER.error("[FantasticWatch] Failed to write tracking log file {}", failedPath, cause);
        Path systemLog = WatchLogger.systemLogPath();
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
            LOGGER.error("[FantasticWatch] Failed to write to system.log as well", nested);
        }
    }

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
