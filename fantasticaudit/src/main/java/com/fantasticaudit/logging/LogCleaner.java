package com.fantasticaudit.logging;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Retention enforcement for per-player audit logs.
 *
 * <p>On server start this removes individual log <em>lines</em> whose ISO-8601 timestamp is
 * older than {@code log_retention_days}; the file itself is never deleted (so a player's UUID
 * file persists across the retention boundary). Each file is rewritten atomically via a
 * temp-file-and-rename so a crash mid-clean can never leave a half-written log.</p>
 */
public final class LogCleaner {

    private static final Logger LOGGER = LogUtils.getLogger();

    private LogCleaner() {
    }

    /**
     * Runs the cleanup on a dedicated background thread so server startup is never blocked.
     *
     * @param playersDir    directory containing the {@code {UUID}.log} files
     * @param retentionDays number of days of lines to keep
     */
    public static void runAsync(Path playersDir, int retentionDays) {
        Thread thread = new Thread(() -> clean(playersDir, retentionDays), "FantasticAudit-LogCleaner");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Performs the synchronous cleanup pass. Public for testability; normal callers should use
     * {@link #runAsync(Path, int)}.
     */
    public static void clean(Path playersDir, int retentionDays) {
        if (playersDir == null || !Files.isDirectory(playersDir)) {
            return;
        }
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int filesProcessed = 0;
        long linesRemoved = 0;

        try (Stream<Path> files = Files.list(playersDir)) {
            List<Path> logFiles = files
                    .filter(p -> p.getFileName().toString().endsWith(".log"))
                    .toList();
            for (Path file : logFiles) {
                long removed = cleanFile(file, cutoff);
                if (removed >= 0) {
                    filesProcessed++;
                    linesRemoved += removed;
                }
            }
        } catch (IOException e) {
            LOGGER.error("[FantasticAudit] Failed to enumerate player log directory {}", playersDir, e);
            AuditLogger.get().system("[CLEANUP_ERROR] failed to list " + playersDir + " : " + e.getMessage());
            return;
        }

        String summary = "[CLEANUP] retention_days=" + retentionDays
                + " cutoff=" + cutoff
                + " files_processed=" + filesProcessed
                + " lines_removed=" + linesRemoved;
        LOGGER.info("[FantasticAudit] {}", summary);
        AuditLogger.get().system(summary);
    }

    /**
     * @return the number of lines removed, or {@code -1} if the file could not be processed
     */
    private static long cleanFile(Path file, Instant cutoff) {
        List<String> kept = new ArrayList<>();
        long removed = 0;
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (keepLine(line, cutoff)) {
                    kept.add(line);
                } else {
                    removed++;
                }
            }
            if (removed == 0) {
                return 0;
            }
            rewriteAtomically(file, kept);
            return removed;
        } catch (IOException e) {
            LOGGER.error("[FantasticAudit] Failed to clean log file {}", file, e);
            AuditLogger.get().system("[CLEANUP_ERROR] file=" + file + " error=" + e.getMessage());
            return -1;
        }
    }

    /**
     * A line is kept when it has no parseable leading timestamp (we never silently discard data
     * we cannot interpret) or when its timestamp is at/after the cutoff.
     */
    private static boolean keepLine(String line, Instant cutoff) {
        Instant ts = parseTimestamp(line);
        if (ts == null) {
            return true;
        }
        return !ts.isBefore(cutoff);
    }

    /**
     * Extracts the leading {@code [ISO-8601]} timestamp from a log line.
     *
     * @return the parsed instant, or {@code null} when the line is not in the expected format
     */
    private static Instant parseTimestamp(String line) {
        if (line == null || line.isEmpty() || line.charAt(0) != '[') {
            return null;
        }
        int end = line.indexOf(']');
        if (end <= 1) {
            return null;
        }
        String candidate = line.substring(1, end);
        try {
            return Instant.parse(candidate);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void rewriteAtomically(Path file, List<String> kept) throws IOException {
        Path dir = file.getParent();
        Path temp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
        try {
            Files.write(temp, kept, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException atomicUnsupported) {
                // Some filesystems (e.g. certain network mounts) cannot do an atomic move.
                // Fall back to a plain replace; still far safer than truncating in place.
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
