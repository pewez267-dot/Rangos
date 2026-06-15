package com.fantastic.kits.audit;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.Reference;
import com.fantastic.kits.config.FKConfig;
import com.fantastic.kits.kits.Kit;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Persistent log of security-related anomalies. Lives in
 * {@code /config/fantastickits/security/security.log}, rotated by size, and
 * always echoed to the console with the {@code [SECURITY]} tag so server
 * owners notice incidents in real time.
 */
public final class SecurityEventLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path dir;
    private final FKConfig cfg;
    private Path activeFile;
    private Writer writer;

    public SecurityEventLogger(Path dir, FKConfig cfg) {
        this.dir = dir;
        this.cfg = cfg;
        try {
            Files.createDirectories(dir);
            this.activeFile = dir.resolve("security.log");
            openWriter();
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Cannot initialise security logger at {}", dir, e);
        }
    }

    public synchronized void log(SecurityEventType type, ServerPlayer player, String playerGroup,
                                 String requiredGroup, Kit kit, String attemptedAction,
                                 String result, String reason) {
        if (!cfg.securityEventsEnabled) return;
        write(new SecurityEvent(type, player, playerGroup, requiredGroup, kit, attemptedAction, result, reason));
    }

    public synchronized void close() {
        try { if (writer != null) { writer.flush(); writer.close(); } }
        catch (IOException ignored) {}
        writer = null;
    }

    private void write(SecurityEvent ev) {
        if (cfg.securityEventsConsole) {
            FantasticKits.LOGGER.warn("[SECURITY] {}", format(ev));
        }
        if (cfg.securityEventsFile && writer != null) {
            try {
                writer.write(format(ev));
                writer.write(System.lineSeparator());
                writer.flush();
                rotateIfNeeded();
            } catch (IOException e) {
                FantasticKits.LOGGER.error("Security write failed", e);
            }
        }
    }

    private String format(SecurityEvent ev) {
        String when = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(ev.timestamp()),
                ZoneId.systemDefault()).format(FMT);
        StringBuilder sb = new StringBuilder(200);
        sb.append('[').append(when).append("] [SECURITY] ");
        sb.append(ev.type().name());
        sb.append(" Player=").append(ev.playerName());
        if (ev.playerId() != null) sb.append(" UUID=").append(ev.playerId());
        if (!ev.playerGroup().isEmpty()) sb.append(" PlayerGroup=").append(ev.playerGroup());
        if (!ev.requiredGroup().isEmpty()) sb.append(" RequiredGroup=").append(ev.requiredGroup());
        if (!ev.kitName().isEmpty()) sb.append(" Kit=\"").append(ev.kitName()).append("\"");
        if (!ev.attemptedAction().isEmpty()) sb.append(" Action=\"").append(ev.attemptedAction()).append("\"");
        sb.append(" Result=").append(ev.result());
        if (!ev.reason().isEmpty()) sb.append(" Reason=\"").append(ev.reason()).append("\"");
        return sb.toString();
    }

    private void openWriter() throws IOException {
        Files.createDirectories(dir);
        this.writer = Files.newBufferedWriter(activeFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }

    private void rotateIfNeeded() throws IOException {
        long bytes = Files.size(activeFile);
        long limit = (long) cfg.securityMaxFileSizeMB * 1024L * 1024L;
        if (bytes < limit) return;
        writer.flush();
        writer.close();
        int idx = 1;
        Path candidate;
        while (true) {
            candidate = dir.resolve("security-" + idx + ".log");
            if (!Files.exists(candidate)) break;
            idx++;
            if (idx > 100_000) break;
        }
        Files.move(activeFile, candidate);
        openWriter();
        FantasticKits.LOGGER.info("[{}] Security log rotated -> {}", Reference.MOD_NAME, candidate.getFileName());
    }
}
