package com.fscrates.animation;

/**
 * Metadata for a crate-opening animation. Every animation shows the reward as a
 * spinning ROULETTE of items (horizontal reel) or a SLOT reel (vertical) that
 * decelerates onto the winner — so the reward is always clearly revealed. The
 * {@link Theme} only changes the surrounding flair (light beam colour, accent
 * particles and the win sound), never whether the reveal works.
 */
public record CrateAnimation(String id, String displayName, Style style, Theme theme,
                             int durationTicks, String description) {

    /** The reveal technique. Kept deliberately small so every option works. */
    public enum Style {
        ROULETTE,     // horizontal scrolling reel that slows to a stop
        SLOT_MACHINE, // vertical scrolling reel
        INSTANT       // no animation (skip)
    }

    /** Flavour: drives the beam colour, accent particles and win chord. */
    public enum Theme {
        CLASSIC, CASINO, NEON, INFERNAL, CELESTIAL, MAGIC, NATURE, ANCIENT
    }

    /** Whether this animation should show a vertical light beam behind the reel. */
    public boolean hasBeam() {
        return style != Style.INSTANT
                && (theme == Theme.CELESTIAL || theme == Theme.MAGIC || theme == Theme.NEON
                    || theme == Theme.ANCIENT || theme == Theme.INFERNAL);
    }
}
