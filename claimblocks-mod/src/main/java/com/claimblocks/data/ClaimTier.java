package com.claimblocks.data;

/**
 * Catalogue of the 10 tier configurations: id, horizontal radius, vertical
 * height (applied symmetrically up and down) and an RGB triplet used by the
 * client outline.
 *
 * Adding a new tier means adding a row here, the matching block in
 * {@link com.claimblocks.block.ModBlocks}, and a texture / blockstate pair
 * in resources.
 */
public final class ClaimTier {
    public static final ClaimTier[] VALUES = new ClaimTier[] {
        new ClaimTier("claimstone_10x10",   10,  15,  0xB0, 0xBE, 0xC5),
        new ClaimTier("claimstone_25x25",   25,  20,  0x64, 0xB5, 0xF6),
        new ClaimTier("claimstone_40x40",   40,  30,  0x4D, 0xD0, 0xE1),
        new ClaimTier("claimstone_64x64",   64,  40,  0x81, 0xC7, 0x84),
        new ClaimTier("claimstone_80x80",   80,  50,  0x38, 0x8E, 0x3C),
        new ClaimTier("claimstone_100x100", 100, 60,  0xFF, 0xD5, 0x4F),
        new ClaimTier("claimstone_150x150", 150, 80,  0xFF, 0x8A, 0x65),
        new ClaimTier("claimstone_250x250", 250, 100, 0xEF, 0x53, 0x50),
        new ClaimTier("claimstone_300x300", 300, 120, 0xB7, 0x1C, 0x1C),
        new ClaimTier("claimstone_500x500", 500, 150, 0x7B, 0x1F, 0xA2),
    };

    public final String id;
    public final int radius;
    public final int height;
    public final float r, g, b;

    private ClaimTier(String id, int radius, int height, int rR, int rG, int rB) {
        this.id = id;
        this.radius = radius;
        this.height = height;
        this.r = rR / 255f;
        this.g = rG / 255f;
        this.b = rB / 255f;
    }

    /** Display label used in messages and menus, e.g. "100x100". */
    public String label() {
        // id format is "claimstone_NxN"
        return id.substring("claimstone_".length());
    }

    /** True if this is a paid-tier claim that supports passive effect flags. */
    public boolean isPaid() {
        return id.equals("claimstone_250x250")
            || id.equals("claimstone_300x300")
            || id.equals("claimstone_500x500");
    }

    public static ClaimTier byId(String id) {
        for (ClaimTier t : VALUES) if (t.id.equals(id)) return t;
        return null;
    }

    /** Migration helper for old v2.x JSON files that used a tier number 1-5. */
    public static ClaimTier byLegacyTier(int legacyTier) {
        return switch (legacyTier) {
            case 1 -> byId("claimstone_10x10");
            case 2 -> byId("claimstone_25x25");
            case 3 -> byId("claimstone_40x40");
            case 4 -> byId("claimstone_64x64");
            case 5 -> byId("claimstone_80x80");
            default -> null;
        };
    }

    /** Best-effort tier match for a (radius, height) pair stored in JSON.
     *  Used by the visualisation and menu when the original tier id is not
     *  preserved. */
    public static ClaimTier closestMatch(int radius, int height) {
        ClaimTier best = VALUES[0];
        int bestScore = Integer.MAX_VALUE;
        for (ClaimTier t : VALUES) {
            int score = Math.abs(t.radius - radius) + Math.abs(t.height - height);
            if (score < bestScore) {
                bestScore = score;
                best = t;
            }
        }
        return best;
    }
}
