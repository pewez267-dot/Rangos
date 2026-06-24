package com.fantastickits.data;

import com.fantastickits.FantasticKits;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Writes audit events to config/fantastickits/audit.log
 * Each line is: [ISO-8601 timestamp] [ACTION] player=name uuid=UUID kit=kitName result=RESULT detail=...
 */
public class AuditLog {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private final Path logPath;

    public AuditLog() {
        this.logPath = DataPaths.getConfigDir().resolve("audit.log");
        try {
            Files.createDirectories(logPath.getParent());
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Failed to create audit log directory", e);
        }
    }

    public enum Action {
        KIT_CREATED,
        KIT_EDITED,
        KIT_DELETED,
        KIT_CLAIMED,
        KIT_CLAIM_DENIED,
        COMMAND_ALLOWED,
        COMMAND_BLOCKED
    }

    /**
     * Log an audit event.
     *
     * @param action     The action type
     * @param playerUUID The UUID of the player involved
     * @param playerName The name of the player involved
     * @param kitName    The kit involved (or command name for command events)
     * @param result     "SUCCESS" or "DENIED"
     * @param detail     Additional detail (e.g., denial reason)
     */
    public synchronized void log(Action action, UUID playerUUID, String playerName, String kitName, String result, String detail) {
        String timestamp = FORMATTER.format(Instant.now().atOffset(ZoneOffset.UTC));
        String line = String.format("[%s] [%s] player=%s uuid=%s kit=%s result=%s detail=%s",
                timestamp, action.name(), playerName, playerUUID.toString(), kitName, result, detail != null ? detail : "none");

        try (BufferedWriter writer = Files.newBufferedWriter(logPath,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Failed to write to audit.log: {}", line, e);
        }
    }

    /**
     * Convenience method for kit creation events.
     */
    public void logKitCreated(UUID adminUUID, String adminName, String kitName) {
        log(Action.KIT_CREATED, adminUUID, adminName, kitName, "SUCCESS", "Kit created");
    }

    /**
     * Convenience method for kit edit events.
     */
    public void logKitEdited(UUID adminUUID, String adminName, String kitName) {
        log(Action.KIT_EDITED, adminUUID, adminName, kitName, "SUCCESS", "Kit edited");
    }

    /**
     * Convenience method for kit deletion events.
     */
    public void logKitDeleted(UUID adminUUID, String adminName, String kitName) {
        log(Action.KIT_DELETED, adminUUID, adminName, kitName, "SUCCESS", "Kit deleted");
    }

    /**
     * Convenience method for successful kit claim.
     */
    public void logKitClaimed(UUID playerUUID, String playerName, String kitName) {
        log(Action.KIT_CLAIMED, playerUUID, playerName, kitName, "SUCCESS", "Kit claimed successfully");
    }

    /**
     * Convenience method for denied kit claim.
     */
    public void logKitClaimDenied(UUID playerUUID, String playerName, String kitName, String reason) {
        log(Action.KIT_CLAIM_DENIED, playerUUID, playerName, kitName, "DENIED", reason);
    }

    /**
     * Convenience method for allowed command execution.
     */
    public void logCommandAllowed(UUID playerUUID, String playerName, String command) {
        log(Action.COMMAND_ALLOWED, playerUUID, playerName, command, "SUCCESS", "Command execution permitted");
    }

    /**
     * Convenience method for blocked command execution.
     */
    public void logCommandBlocked(UUID playerUUID, String playerName, String command) {
        log(Action.COMMAND_BLOCKED, playerUUID, playerName, command, "DENIED", "Command not allowed for player group");
    }
}
