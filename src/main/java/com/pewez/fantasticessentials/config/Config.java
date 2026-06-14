package com.pewez.fantasticessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pewez.fantasticessentials.EssentialsMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main configuration, stored in config/essentials/config.json.
 */
public final class Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config INSTANCE = new Config();
    private static Path file;

    // ----- General -----
    public boolean broadcastToConsole = true;

    // ----- Homes -----
    public int defaultHomeLimit = 5;
    /** Players with op level >= this bypass the home limit. */
    public int homeLimitBypassLevel = 2;

    // ----- Teleportation -----
    /** Seconds to wait before a teleport (home/warp/back/tpa) completes. 0 = instant. */
    public int teleportWarmupSeconds = 0;
    /** Cancel the warmup if the player moves. */
    public boolean cancelWarmupOnMove = true;
    /** Cancel the warmup if the player takes damage. */
    public boolean cancelWarmupOnDamage = true;
    /** Op level that bypasses the warmup. */
    public int warmupBypassLevel = 2;

    // ----- Cooldowns (seconds, 0 = disabled) -----
    public int homeCooldownSeconds = 0;
    public int warpCooldownSeconds = 0;
    public int backCooldownSeconds = 0;
    public int tpaCooldownSeconds = 0;
    /** Op level that bypasses cooldowns. */
    public int cooldownBypassLevel = 2;

    // ----- TPA -----
    /** Seconds before a pending tpa request expires. */
    public int tpaTimeoutSeconds = 120;

    // ----- Permission levels per command node (0 = everyone) -----
    public Map<String, Integer> permissionLevels = defaultPermissionLevels();

    private static Map<String, Integer> defaultPermissionLevels() {
        Map<String, Integer> map = new LinkedHashMap<>();
        // Player commands - available to everyone by default
        map.put("fantasticessentials.command.home", 0);
        map.put("fantasticessentials.command.sethome", 0);
        map.put("fantasticessentials.command.delhome", 0);
        map.put("fantasticessentials.command.homes", 0);
        map.put("fantasticessentials.command.warp", 0);
        map.put("fantasticessentials.command.warps", 0);
        map.put("fantasticessentials.command.back", 0);
        map.put("fantasticessentials.command.tpa", 0);
        map.put("fantasticessentials.command.tpaccept", 0);
        map.put("fantasticessentials.command.tpdeny", 0);
        map.put("fantasticessentials.command.msg", 0);
        map.put("fantasticessentials.command.reply", 0);
        map.put("fantasticessentials.command.ping", 0);
        map.put("fantasticessentials.command.whois", 0);
        // Convenience menus
        map.put("fantasticessentials.command.anvil", 0);
        map.put("fantasticessentials.command.cartographytable", 0);
        map.put("fantasticessentials.command.craft", 0);
        map.put("fantasticessentials.command.enchanting", 0);
        map.put("fantasticessentials.command.enderchest", 0);
        map.put("fantasticessentials.command.grindstone", 0);
        map.put("fantasticessentials.command.loom", 0);
        map.put("fantasticessentials.command.smithing", 0);
        map.put("fantasticessentials.command.stonecutter", 0);
        // Admin commands
        map.put("fantasticessentials.command.setwarp", 2);
        map.put("fantasticessentials.command.delwarp", 2);
        map.put("fantasticessentials.command.tpall", 2);
        map.put("fantasticessentials.command.feed", 2);
        map.put("fantasticessentials.command.feed.others", 2);
        map.put("fantasticessentials.command.heal", 2);
        map.put("fantasticessentials.command.heal.others", 2);
        map.put("fantasticessentials.command.fly", 2);
        map.put("fantasticessentials.command.fly.others", 2);
        map.put("fantasticessentials.command.flyspeed", 2);
        map.put("fantasticessentials.command.walkspeed", 2);
        map.put("fantasticessentials.command.glow", 2);
        map.put("fantasticessentials.command.invulnerable", 2);
        map.put("fantasticessentials.command.hat", 2);
        map.put("fantasticessentials.command.repair", 2);
        map.put("fantasticessentials.command.itemedit", 2);
        map.put("fantasticessentials.command.signedit", 2);
        map.put("fantasticessentials.command.broadcast", 2);
        map.put("fantasticessentials.command.commandspy", 3);
        map.put("fantasticessentials.command.essentials", 4);
        map.put("fantasticessentials.command.mods", 2);
        return map;
    }

    public int permissionLevel(String node, int defaultLevel) {
        Integer value = permissionLevels.get(node);
        return value != null ? value : defaultLevel;
    }

    public static Config get() {
        return INSTANCE;
    }

    public static void load(Path configDir) {
        file = configDir.resolve("config.json");
        if (Files.exists(file)) {
            try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                Config loaded = GSON.fromJson(content, Config.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                    if (INSTANCE.permissionLevels == null) {
                        INSTANCE.permissionLevels = defaultPermissionLevels();
                    } else {
                        // Merge any missing default nodes
                        defaultPermissionLevels().forEach(INSTANCE.permissionLevels::putIfAbsent);
                    }
                }
            } catch (Exception e) {
                EssentialsMod.LOGGER.error("Failed to read config.json, using defaults", e);
            }
        }
        save();
    }

    public static void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(INSTANCE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            EssentialsMod.LOGGER.error("Failed to write config.json", e);
        }
    }
}
