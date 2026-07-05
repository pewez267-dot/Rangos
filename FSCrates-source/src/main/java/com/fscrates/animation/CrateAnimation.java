package com.fscrates.animation;

public record CrateAnimation(String id, String displayName, Style style, Theme theme, int durationTicks, String description) {
    public boolean hasBeam() {
        return this.style != Style.INSTANT && (this.theme == Theme.CELESTIAL || this.theme == Theme.MAGIC || this.theme == Theme.NEON || this.theme == Theme.ANCIENT || this.theme == Theme.INFERNAL);
    }

    public static enum Style {
        ROULETTE,
        SLOT_MACHINE,
        INSTANT;

    }

    public static enum Theme {
        CLASSIC,
        CASINO,
        NEON,
        INFERNAL,
        CELESTIAL,
        MAGIC,
        NATURE,
        ANCIENT;

    }
}

