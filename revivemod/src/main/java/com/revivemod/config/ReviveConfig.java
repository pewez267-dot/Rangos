package com.revivemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server-side configuration for the Revive Mod.
 * The config is stored as JSON in the world's "config" folder so that
 * server admins can tweak values without restarting the JVM (they will
 * apply on the next /revive reload or server restart).
 */
public class ReviveConfig {
    private static final Logger LOG = LoggerFactory.getLogger("revivemod");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** How long (in seconds) a knocked-out player has before they truly die. */
    public int downTimeSeconds = 90;

    /** Distance (in blocks) at which a player can revive a downed teammate. */
    public double reviveDistance = 3.0;

    /** How long (in ticks; 20 ticks = 1 second) the reviver must keep right-clicking. */
    public int reviveTimeTicks = 80;

    /** HP restored on revive (0..20). */
    public float reviveHealth = 6.0f;

    /** Hunger restored on revive (0..20). */
    public int reviveFood = 10;

    /** Apply Glowing effect to downed players so allies can find them through walls. */
    public boolean glowingWhileDown = true;

    /** Mob/PvP attacks against downed players are absorbed (cancelled). When this is true,
     *  hostile mobs whose current target is a freshly-downed player will lose interest
     *  on knockdown so the bossbar countdown isn't drowned in pointless hits. */
    public boolean clearMobAggroOnDown = true;

    /** Slowness amplifier applied while down (0-indexed: 2 = Slowness III, ~40% speed).
     *  The player is forced into a crawl pose; this just slows the drag. */
    public int crawlSlowness = 2;

    /** Whether a downed player may pay XP levels to revive themselves. */
    public boolean allowSelfRevive = true;

    /** XP level cost for self-reviving. */
    public int selfReviveLevelCost = 10;

    // ---- Sound volumes (all amethyst chimes) ----
    /** Knockdown chime: how many layered copies (higher = louder/fuller). */
    public int knockdownSoundLayers = 5;
    /** Volume of each knockdown chime layer. */
    public float knockdownVolume = 1.0f;
    /** Volume of the death chimes. */
    public float deathVolume = 1.0f;
    /** Number of layered copies of each death chime (matches knockdown loudness). */
    public int deathSoundLayers = 5;
    /** Volume of the revive-success chimes. */
    public float reviveVolume = 1.0f;
    /** Number of layered copies of each revive-success chime. */
    public int reviveSoundLayers = 5;
    /** Volume of the per-second countdown ("bleeding") chime. */
    public float countdownTickVolume = 0.35f;
    /** Volume of the chime played each step while being revived. */
    public float reviveTickVolume = 0.4f;

    // ---- Bleeding particles ----
    /** Red "blood" particle: number of drops spawned each interval. */
    public int bloodParticleCount = 2;
    /** Interval (in ticks) between blood particle spawns. */
    public int bloodParticleInterval = 12;
    /** Scale of the red blood dust particle. */
    public float bloodParticleScale = 0.9f;
    /** White "bleeding" particle: number spawned each interval (same timing/spread as blood). */
    public int whiteParticleCount = 1;
    /** Scale of the white blood dust particle. */
    public float whiteParticleScale = 0.7f;

    public static ReviveConfig load(Path configFile) {
        ReviveConfig cfg;
        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                cfg = GSON.fromJson(json, ReviveConfig.class);
                if (cfg == null) cfg = new ReviveConfig();
            } catch (IOException | RuntimeException ex) {
                LOG.error("[revivemod] Failed to read config, using defaults: {}", ex.getMessage());
                cfg = new ReviveConfig();
            }
        } else {
            cfg = new ReviveConfig();
        }
        // Always re-write to add new fields.
        cfg.save(configFile);
        return cfg;
    }

    public void save(Path configFile) {
        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, GSON.toJson(this));
        } catch (IOException ex) {
            LOG.error("[revivemod] Failed to write config: {}", ex.getMessage());
        }
    }
}
