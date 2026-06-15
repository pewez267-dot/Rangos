package com.fantastic.kits.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.Reference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Strongly-typed wrapper around the on-disk {@code config.toml}.
 * <p>
 * All fields have safe defaults. Loading creates the file with full inline
 * comments if it does not exist. Saving back is supported so the file can be
 * regenerated when defaults are added in future versions without losing the
 * options the operator already customised.
 */
public final class FKConfig {

    // -------- General --------
    public boolean strictGroupMatching = true;
    public boolean luckPermsRequired = false;
    public String defaultGroupName = "default";
    public String chatPrefix = Reference.CHAT_PREFIX;

    // -------- Claim safety --------
    public int claimCooldownMillis = 750;
    public int maxKitsPerSecond = 2;
    public boolean blockClaimsWhenInventoryFull = true;
    public boolean blockClaimsForFakePlayers = true;

    // -------- GUI --------
    public boolean guiAnimationsEnabled = true;
    public int guiClickCooldownMillis = 150;
    public int guiMaxOpenPerMinute = 60;

    // -------- Command discovery --------
    public boolean discoverModCommands = true;
    public boolean discoverVanillaCommands = true;
    public boolean discoverForgeCommands = true;

    // -------- Audit log --------
    public boolean auditLogEnabled = true;
    public boolean auditLogConsole = true;
    public boolean auditLogFile = true;
    public int auditLogMaxEntries = 50_000;
    public int auditMaxFileSizeMB = 25;
    public boolean auditViewerEnabled = true;
    public String auditFormat = "log"; // "log" or "json"

    // -------- Security events --------
    public boolean securityEventsEnabled = true;
    public boolean securityEventsConsole = true;
    public boolean securityEventsFile = true;
    public int securityMaxFileSizeMB = 25;

    // -------- Anti exploit --------
    public boolean blockPacketSpam = true;
    public int packetSpamThreshold = 30; // per second
    public boolean blockNbtInjection = true;
    public boolean validateInventorySync = true;
    public boolean rejectForgedClientFlags = true;

    private FKConfig() {}

    /**
     * Load the configuration from {@code <root>/config.toml}, or create the
     * file with defaults the first time the mod runs.
     */
    public static FKConfig loadOrCreate(Path configRoot) {
        FKConfig cfg = new FKConfig();
        try {
            Files.createDirectories(configRoot);
            Path file = configRoot.resolve(Reference.FILE_CONFIG);

            try (CommentedFileConfig fc = CommentedFileConfig.builder(file).preserveInsertionOrder().sync().build()) {
                if (!Files.exists(file)) {
                    cfg.writeDefaults(fc);
                    fc.save();
                } else {
                    fc.load();
                    cfg.readFrom(fc);
                    // Backfill any missing keys without overwriting existing ones.
                    cfg.writeDefaults(fc);
                    fc.save();
                }
            }
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Failed to load config, falling back to in-memory defaults.", e);
        }
        return cfg;
    }

    private void readFrom(CommentedConfig fc) {
        strictGroupMatching = fc.getOrElse("general.strictGroupMatching", strictGroupMatching);
        luckPermsRequired = fc.getOrElse("general.luckPermsRequired", luckPermsRequired);
        defaultGroupName = fc.getOrElse("general.defaultGroupName", defaultGroupName);
        chatPrefix = fc.getOrElse("general.chatPrefix", chatPrefix);

        claimCooldownMillis = fc.getOrElse("claims.cooldownMillis", claimCooldownMillis);
        maxKitsPerSecond = fc.getOrElse("claims.maxKitsPerSecond", maxKitsPerSecond);
        blockClaimsWhenInventoryFull = fc.getOrElse("claims.blockWhenInventoryFull", blockClaimsWhenInventoryFull);
        blockClaimsForFakePlayers = fc.getOrElse("claims.blockFakePlayers", blockClaimsForFakePlayers);

        guiAnimationsEnabled = fc.getOrElse("gui.animationsEnabled", guiAnimationsEnabled);
        guiClickCooldownMillis = fc.getOrElse("gui.clickCooldownMillis", guiClickCooldownMillis);
        guiMaxOpenPerMinute = fc.getOrElse("gui.maxOpenPerMinute", guiMaxOpenPerMinute);

        discoverModCommands = fc.getOrElse("commands.discoverMods", discoverModCommands);
        discoverVanillaCommands = fc.getOrElse("commands.discoverVanilla", discoverVanillaCommands);
        discoverForgeCommands = fc.getOrElse("commands.discoverForge", discoverForgeCommands);

        auditLogEnabled = fc.getOrElse("audit.auditLogEnabled", auditLogEnabled);
        auditLogConsole = fc.getOrElse("audit.auditLogConsole", auditLogConsole);
        auditLogFile = fc.getOrElse("audit.auditLogFile", auditLogFile);
        auditLogMaxEntries = fc.getOrElse("audit.auditLogMaxEntries", auditLogMaxEntries);
        auditMaxFileSizeMB = fc.getOrElse("audit.auditMaxFileSizeMB", auditMaxFileSizeMB);
        auditViewerEnabled = fc.getOrElse("audit.auditViewerEnabled", auditViewerEnabled);
        auditFormat = fc.getOrElse("audit.format", auditFormat);

        securityEventsEnabled = fc.getOrElse("security.enabled", securityEventsEnabled);
        securityEventsConsole = fc.getOrElse("security.console", securityEventsConsole);
        securityEventsFile = fc.getOrElse("security.file", securityEventsFile);
        securityMaxFileSizeMB = fc.getOrElse("security.maxFileSizeMB", securityMaxFileSizeMB);

        blockPacketSpam = fc.getOrElse("antiexploit.blockPacketSpam", blockPacketSpam);
        packetSpamThreshold = fc.getOrElse("antiexploit.packetSpamThreshold", packetSpamThreshold);
        blockNbtInjection = fc.getOrElse("antiexploit.blockNbtInjection", blockNbtInjection);
        validateInventorySync = fc.getOrElse("antiexploit.validateInventorySync", validateInventorySync);
        rejectForgedClientFlags = fc.getOrElse("antiexploit.rejectForgedClientFlags", rejectForgedClientFlags);
    }

    private void writeDefaults(CommentedFileConfig fc) {
        setIfAbsent(fc, "general.strictGroupMatching", strictGroupMatching,
                "When true, kits can ONLY be claimed by players whose primary LuckPerms group equals the kit owner group. Inheritance is ignored on purpose. Default: true.");
        setIfAbsent(fc, "general.luckPermsRequired", luckPermsRequired,
                "If true the mod refuses to load when LuckPerms is missing. Default: false (warn-only).");
        setIfAbsent(fc, "general.defaultGroupName", defaultGroupName,
                "Group name used as a fallback when LuckPerms is unavailable.");
        setIfAbsent(fc, "general.chatPrefix", chatPrefix,
                "Prefix used by all chat feedback. Supports legacy color codes (\u00A7).");

        setIfAbsent(fc, "claims.cooldownMillis", claimCooldownMillis,
                "Per-player cooldown between two consecutive claim attempts (anti macro).");
        setIfAbsent(fc, "claims.maxKitsPerSecond", maxKitsPerSecond,
                "Hard cap on claims per second across all players (anti race-condition).");
        setIfAbsent(fc, "claims.blockWhenInventoryFull", blockClaimsWhenInventoryFull,
                "Refuse the claim if the player has no room - prevents item loss.");
        setIfAbsent(fc, "claims.blockFakePlayers", blockClaimsForFakePlayers,
                "Refuse claims initiated by FakePlayer entities.");

        setIfAbsent(fc, "gui.animationsEnabled", guiAnimationsEnabled,
                "Toggle subtle UI animations for icons.");
        setIfAbsent(fc, "gui.clickCooldownMillis", guiClickCooldownMillis,
                "Minimum millisecond delta between clicks on a single GUI slot.");
        setIfAbsent(fc, "gui.maxOpenPerMinute", guiMaxOpenPerMinute,
                "Hard limit on GUI openings per player per minute.");

        setIfAbsent(fc, "commands.discoverMods", discoverModCommands,
                "Discover commands registered by mods.");
        setIfAbsent(fc, "commands.discoverVanilla", discoverVanillaCommands,
                "Discover vanilla commands.");
        setIfAbsent(fc, "commands.discoverForge", discoverForgeCommands,
                "Discover commands registered by Forge.");

        setIfAbsent(fc, "audit.auditLogEnabled", auditLogEnabled,
                "Master switch for the audit log subsystem.");
        setIfAbsent(fc, "audit.auditLogConsole", auditLogConsole,
                "Mirror audit entries to the server console.");
        setIfAbsent(fc, "audit.auditLogFile", auditLogFile,
                "Persist audit entries to /config/fantastickits/audit/audit.log.");
        setIfAbsent(fc, "audit.auditLogMaxEntries", auditLogMaxEntries,
                "Soft cap on entries kept in memory.");
        setIfAbsent(fc, "audit.auditMaxFileSizeMB", auditMaxFileSizeMB,
                "When the active audit file reaches this size it rotates to audit-N.log.");
        setIfAbsent(fc, "audit.auditViewerEnabled", auditViewerEnabled,
                "Allow administrators to inspect audit entries via in-game tools.");
        setIfAbsent(fc, "audit.format", auditFormat,
                "Output format: log (human readable) or json (machine readable).");

        setIfAbsent(fc, "security.enabled", securityEventsEnabled,
                "Master switch for the SECURITY_EVENTS subsystem.");
        setIfAbsent(fc, "security.console", securityEventsConsole,
                "Print security events to the console with [SECURITY] tag.");
        setIfAbsent(fc, "security.file", securityEventsFile,
                "Persist security events to /config/fantastickits/security/security.log.");
        setIfAbsent(fc, "security.maxFileSizeMB", securityMaxFileSizeMB,
                "Rotation threshold for the security log file.");

        setIfAbsent(fc, "antiexploit.blockPacketSpam", blockPacketSpam,
                "Throttle players who exceed packetSpamThreshold packets/second on /fkits packets.");
        setIfAbsent(fc, "antiexploit.packetSpamThreshold", packetSpamThreshold,
                "Threshold (packets per second per player).");
        setIfAbsent(fc, "antiexploit.blockNbtInjection", blockNbtInjection,
                "Reject NBT updates that try to inject blacklisted tags.");
        setIfAbsent(fc, "antiexploit.validateInventorySync", validateInventorySync,
                "Re-verify inventory state on every claim against the server snapshot.");
        setIfAbsent(fc, "antiexploit.rejectForgedClientFlags", rejectForgedClientFlags,
                "Treat clients claiming impossible capabilities as forged and disconnect.");
    }

    private static void setIfAbsent(CommentedFileConfig fc, String path, Object value, String comment) {
        if (!fc.contains(path)) {
            fc.set(path, value);
        }
        fc.setComment(path, " " + comment);
    }
}
