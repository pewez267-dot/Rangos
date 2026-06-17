package com.fantasticaudit.util;

import net.minecraft.core.BlockPos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tiny de-duplication guard shared between the Forge {@code BlockEvent.BreakEvent} handler and the
 * optional Architectury {@code BlockEvent.BREAK} handler.
 *
 * <p>A single block break can reach both handlers (the directly-hit block fires Forge's event,
 * which Architectury also bridges), while area-tool blocks (e.g. JustHammers) only reach the
 * Architectury handler. Keying on {@code uuid + dimension + pos + gameTime} lets whichever handler
 * fires first claim the break; the second observes the key and skips, so the central block is never
 * logged twice while the extra blocks are still captured.</p>
 */
public final class RecentBreaks {

    private static final RecentBreaks INSTANCE = new RecentBreaks();

    /** Entries older than this are purged; a block break is observed by both handlers within 1 tick. */
    private static final long EXPIRY_MILLIS = 3_000L;
    private static final int MAX_ENTRIES = 8_192;

    private final ConcurrentHashMap<String, Long> seen = new ConcurrentHashMap<>();

    private RecentBreaks() {
    }

    public static RecentBreaks get() {
        return INSTANCE;
    }

    /**
     * Atomically claims a break for logging.
     *
     * @return {@code true} if this is the first handler to see the break (caller should log it),
     * {@code false} if it was already claimed by the other handler this tick
     */
    public boolean claim(UUID uuid, String dimension, BlockPos pos, long gameTime) {
        final long now = System.currentTimeMillis();
        purgeIfNeeded(now);
        final String key = uuid + "|" + dimension + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ()
                + "|" + gameTime;
        return seen.putIfAbsent(key, now) == null;
    }

    private void purgeIfNeeded(long now) {
        if (seen.size() < MAX_ENTRIES) {
            // Cheap path: only drop clearly-expired entries occasionally.
            seen.values().removeIf(stamp -> now - stamp > EXPIRY_MILLIS);
            return;
        }
        // Safety valve: never let the map grow unbounded.
        seen.clear();
    }
}
