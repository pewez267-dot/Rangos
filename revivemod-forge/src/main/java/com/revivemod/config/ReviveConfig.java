package com.revivemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReviveConfig {
    private static final Logger LOG = LoggerFactory.getLogger((String)"revivemod");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public int downTimeSeconds = 90;
    public double reviveDistance = 3.0;
    public int reviveTimeTicks = 80;
    public float reviveHealth = 6.0f;
    public int reviveFood = 10;
    public boolean glowingWhileDown = true;
    public boolean clearMobAggroOnDown = true;
    public int crawlSlowness = 2;
    public boolean allowSelfRevive = true;
    public int selfReviveLevelCost = 10;
    public int knockdownSoundLayers = 5;
    public float knockdownVolume = 1.0f;
    public float deathVolume = 1.0f;
    public int deathSoundLayers = 5;
    public float reviveVolume = 1.0f;
    public int reviveSoundLayers = 5;
    public float countdownTickVolume = 0.35f;
    public float reviveTickVolume = 0.4f;
    public int bloodParticleCount = 2;
    public int bloodParticleInterval = 12;
    public float bloodParticleScale = 0.9f;
    public int whiteParticleCount = 1;
    public float whiteParticleScale = 0.7f;

    public static ReviveConfig load(Path configFile) {
        ReviveConfig cfg;
        if (Files.exists(configFile, new LinkOption[0])) {
            try {
                String json = Files.readString(configFile);
                cfg = (ReviveConfig)GSON.fromJson(json, ReviveConfig.class);
                if (cfg == null) {
                    cfg = new ReviveConfig();
                }
            }
            catch (IOException | RuntimeException ex) {
                LOG.error("[revivemod] Failed to read config, using defaults: {}", (Object)ex.getMessage());
                cfg = new ReviveConfig();
            }
        } else {
            cfg = new ReviveConfig();
        }
        cfg.save(configFile);
        return cfg;
    }

    public void save(Path configFile) {
        try {
            Files.createDirectories(configFile.getParent(), new FileAttribute[0]);
            Files.writeString(configFile, (CharSequence)GSON.toJson((Object)this), new OpenOption[0]);
        }
        catch (IOException ex) {
            LOG.error("[revivemod] Failed to write config: {}", (Object)ex.getMessage());
        }
    }
}

