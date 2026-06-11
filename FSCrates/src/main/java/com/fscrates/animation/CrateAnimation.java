package com.fscrates.animation;

/**
 * Metadata describing a single crate-opening animation. The actual client-side
 * playback is handled by {@code AnimationPlayer}; this record is the
 * server/data representation so animations can be selected in the GUI and
 * stored in NBT by id.
 *
 * <p>Animations are intentionally data-driven: adding a new one is as simple as
 * registering another {@link CrateAnimation} in {@link AnimationRegistry}. The
 * client renderer interprets the {@link Style} + {@link Theme} to drive its
 * particles, GUI overlays, camera shake and timing, so the catalogue can grow
 * without limit.
 */
public record CrateAnimation(String id, String displayName, Style style, Theme theme,
                             int durationTicks, String description) {

    /** The core visual technique a renderer should use. */
    public enum Style {
        ROULETTE,       // horizontal scrolling reel that slows to a stop
        SLOT_MACHINE,   // vertical spinning reels
        SPIN,           // 3D crate spins then bursts
        ITEM_RAIN,      // items fall from the top
        LOOT_EXPLOSION, // reward bursts outward from the crate
        BEAM_REVEAL,    // a vertical beam of light reveals the reward
        ORBIT,          // candidate rewards orbit before one is chosen
        CARD_FLIP,      // a face-down card flips to reveal
        SHATTER,        // crate shell shatters to reveal contents
        PORTAL,         // a portal opens and the reward emerges
        SUMMON_CIRCLE,  // a magic circle charges and summons the reward
        WAVE_PULSE,     // concentric pulses build to a reveal
        FIREWORKS,      // celebratory fireworks finish
        GALAXY_SWIRL,   // a swirl of star particles condenses into the reward
        INSTANT;        // no animation (used when player skips with SHIFT)
    }

    /** A flavour/palette hint so renderers can theme particles, sounds, colours. */
    public enum Theme {
        CLASSIC, CASINO, RPG, MAGIC, SCIFI, NATURE, INFERNAL, CELESTIAL, NEON, ANCIENT
    }
}
