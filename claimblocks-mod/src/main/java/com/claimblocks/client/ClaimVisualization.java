package com.claimblocks.client;

/**
 * Client-side visualisation hook. The actual particle effects are emitted
 * server-side in {@link com.claimblocks.event.ClaimEntryTracker}, so this
 * class is a no-op stub kept for symmetry with the requested module layout.
 */
public class ClaimVisualization {
    public static void register() {
        // nothing for now - server-side particles are sufficient
    }
}
