package com.fantasticwatch.tracking;

import com.fantasticwatch.config.WatchConfig;
import com.fantasticwatch.logging.WatchLogger;
import com.fantasticwatch.util.NbtUtil;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Owns end-of-life summaries and the weekly (Monday-to-Monday by default) purge.
 *
 * <p>The purge boundary is the most recent configured {@code cleanup_day} at 00:00 UTC. On server
 * start, index entries and operator-log lines older than the boundary are removed (logs are
 * pruned line-by-line via an atomic temp-and-rename, never truncated in place). In-world NBT marks
 * are stripped lazily on next encounter (see {@link ItemTracker#stripIfExpired}); scanning every
 * chunk for expired items would be infeasible and unsafe on a production server.</p>
 */
public final class LifecycleManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private LifecycleManager() {
    }

    /** @return the most recent configured cleanup-day at 00:00 UTC. */
    public static Instant purgeCutoff() {
        DayOfWeek day = WatchConfig.cleanupDay();
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime boundary = nowUtc.with(TemporalAdjusters.previousOrSame(day)).truncatedTo(ChronoUnit.DAYS);
        return boundary.toInstant();
    }

    /**
     * Writes the lifecycle-end summary for an item to its origin log. Emitted as a single line so
     * the line-based retention purge can parse and prune it like any other entry.
     */
    public static void logLifecycleEnd(NbtUtil.MarkData mark, String itemId, String lastOwnerName,
                                       java.util.UUID lastOwnerUuid, String finalAction) {
        if (mark == null || mark.spawnedBy() == null) {
            return;
        }
        long durationSeconds = computeDurationSeconds(mark.spawnedAt());
        String payload = itemId
                + " by " + mark.spawnedByName()
                + " transfers=" + mark.transferCount()
                + " last=" + (lastOwnerName == null ? "unknown" : lastOwnerName)
                + " final=" + finalAction
                + " dur=" + durationSeconds + "s"
                + "  #" + mark.uid();
        WatchLogger.get().record(mark.spawnedBy(), mark.spawnedByName(), "ITEM_LIFECYCLE_END", payload);
    }

    /** Runs the weekly purge on a background thread so server start is never blocked. */
    public static void runWeeklyPurgeAsync(Path opsDir) {
        Thread thread = new Thread(() -> runWeeklyPurge(opsDir), "FantasticWatch-WeeklyPurge");
        thread.setDaemon(true);
        thread.start();
    }

    /** Performs the synchronous purge pass. */
    public static void runWeeklyPurge(Path opsDir) {
        Instant cutoff = purgeCutoff();

        // 1) Purge expired index entries.
        List<String> expiredUids = new ArrayList<>();
        Map<String, TrackingIndex.IndexEntry> snapshot = TrackingIndex.get().snapshot();
        for (Map.Entry<String, TrackingIndex.IndexEntry> entry : snapshot.entrySet()) {
            Instant spawned = parseInstant(entry.getValue().spawned_at);
            if (spawned != null && spawned.isBefore(cutoff)) {
                expiredUids.add(entry.getKey());
            }
        }
        int indexRemoved = TrackingIndex.get().removeAll(expiredUids);

        // 2) Purge expired log lines from every operator log.
        int filesProcessed = 0;
        long linesRemoved = 0;
        if (opsDir != null && Files.isDirectory(opsDir)) {
            try (Stream<Path> files = Files.list(opsDir)) {
                List<Path> logFiles = files
                        .filter(p -> p.getFileName().toString().endsWith(".log"))
                        .toList();
                for (Path file : logFiles) {
                    long removed = pruneLogLinesBefore(file, cutoff);
                    if (removed >= 0) {
                        filesProcessed++;
                        linesRemoved += removed;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("[FantasticWatch] Failed to enumerate ops directory {}", opsDir, e);
                WatchLogger.get().system("[PURGE_ERROR] failed to list " + opsDir + " : " + e.getMessage());
            }
        }

        String summary = "[PURGE] cutoff=" + cutoff
                + " cleanup_day=" + WatchConfig.cleanupDay()
                + " index_removed=" + indexRemoved
                + " files_processed=" + filesProcessed
                + " lines_removed=" + linesRemoved;
        LOGGER.info("[FantasticWatch] {}", summary);
        WatchLogger.get().system(summary);
    }

    /**
     * @return number of lines removed from the file, or {@code -1} on failure
     */
    private static long pruneLogLinesBefore(Path file, Instant cutoff) {
        List<String> kept = new ArrayList<>();
        long removed = 0;
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String line : lines) {
                Instant ts = parseLeadingTimestamp(line);
                if (ts == null || !ts.isBefore(cutoff)) {
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
            LOGGER.error("[FantasticWatch] Failed to purge log file {}", file, e);
            WatchLogger.get().system("[PURGE_ERROR] file=" + file + " error=" + e.getMessage());
            return -1;
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
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static long computeDurationSeconds(String spawnedAtIso) {
        Instant spawned = parseInstant(spawnedAtIso);
        if (spawned == null) {
            return -1L;
        }
        return Math.max(0L, ChronoUnit.SECONDS.between(spawned, Instant.now()));
    }

    private static Instant parseLeadingTimestamp(String line) {
        if (line == null || line.isEmpty() || line.charAt(0) != '[') {
            return null;
        }
        int end = line.indexOf(']');
        if (end <= 1) {
            return null;
        }
        return parseInstant(line.substring(1, end));
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(iso);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
