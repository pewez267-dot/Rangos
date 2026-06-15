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
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Persistent, rotating audit logger. Every administrative or claim-related
 * action is recorded both to the server console (with the {@code [AUDIT]}
 * tag) and to {@code /config/fantastickits/audit/audit.log}.
 *
 * <p>The file rotates as soon as it crosses {@link FKConfig#auditMaxFileSizeMB}
 * yielding {@code audit-1.log}, {@code audit-2.log}, ... so the current file
 * never grows unbounded. Old files are never deleted: kit history must be
 * permanent and inspectable, as required by the spec.
 *
 * <p>Logs are intentionally append-only. There is no API to mutate or remove a
 * past entry, mirroring the spec's "logs are never modifiable" rule.
 */
public final class AuditLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path dir;
    private final FKConfig cfg;
    private final Deque<AuditEvent> recent = new ArrayDeque<>();

    private Path activeFile;
    private Writer writer;

    public AuditLogger(Path dir, FKConfig cfg) {
        this.dir = dir;
        this.cfg = cfg;
        try {
            Files.createDirectories(dir);
            this.activeFile = dir.resolve("audit." + (cfg.auditFormat.equalsIgnoreCase("json") ? "json" : "log"));
            openWriter();
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Cannot initialise audit logger at {}", dir, e);
        }
    }

    public synchronized void log(AuditEventType type, ServerPlayer player, Kit kit, String result, String details) {
        if (!cfg.auditLogEnabled) return;
        AuditEvent ev = new AuditEvent(type, player, kit, result, details);
        store(ev);
    }

    public synchronized void log(AuditEvent ev) {
        if (!cfg.auditLogEnabled) return;
        store(ev);
    }

    public synchronized java.util.List<AuditEvent> recent(int limit) {
        java.util.List<AuditEvent> out = new java.util.ArrayList<>();
        java.util.Iterator<AuditEvent> it = recent.descendingIterator();
        while (it.hasNext() && out.size() < limit) out.add(it.next());
        return out;
    }

    public synchronized void close() {
        try { if (writer != null) { writer.flush(); writer.close(); } }
        catch (IOException ignored) {}
        writer = null;
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private void store(AuditEvent ev) {
        recent.addLast(ev);
        while (recent.size() > Math.max(100, cfg.auditLogMaxEntries)) recent.pollFirst();

        if (cfg.auditLogConsole) {
            FantasticKits.LOGGER.info("[AUDIT] {}", formatHuman(ev));
        }
        if (cfg.auditLogFile && writer != null) {
            try {
                writer.write(cfg.auditFormat.equalsIgnoreCase("json") ? formatJson(ev) : formatHuman(ev));
                writer.write(System.lineSeparator());
                writer.flush();
                rotateIfNeeded();
            } catch (IOException e) {
                FantasticKits.LOGGER.error("Audit write failed", e);
            }
        }
    }

    private String formatHuman(AuditEvent ev) {
        String when = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(ev.timestamp()), ZoneId.systemDefault()).format(FMT);
        StringBuilder sb = new StringBuilder(160);
        sb.append('[').append(when).append("] ");
        sb.append("Player=").append(ev.playerName());
        if (ev.playerId() != null) sb.append(" UUID=").append(ev.playerId());
        if (!ev.address().isEmpty()) sb.append(" IP=").append(ev.address());
        sb.append(" Action=").append(ev.type().name());
        if (!ev.kitName().isEmpty()) sb.append(" Kit=\"").append(ev.kitName()).append("\"");
        if (!ev.kitId().isEmpty()) sb.append(" KitId=").append(ev.kitId());
        if (!ev.group().isEmpty()) sb.append(" Group=").append(ev.group());
        sb.append(" Result=").append(ev.result());
        if (!ev.details().isEmpty()) sb.append(" Info=\"").append(ev.details().replace('"', '\'')).append('"');
        return sb.toString();
    }

    private String formatJson(AuditEvent ev) {
        StringBuilder sb = new StringBuilder(220);
        sb.append('{');
        sb.append("\"ts\":").append(ev.timestamp()).append(',');
        sb.append("\"player\":\"").append(escape(ev.playerName())).append("\",");
        if (ev.playerId() != null) sb.append("\"uuid\":\"").append(ev.playerId()).append("\",");
        sb.append("\"ip\":\"").append(escape(ev.address())).append("\",");
        sb.append("\"action\":\"").append(ev.type().name()).append("\",");
        sb.append("\"kit\":\"").append(escape(ev.kitName())).append("\",");
        sb.append("\"kitId\":\"").append(escape(ev.kitId())).append("\",");
        sb.append("\"group\":\"").append(escape(ev.group())).append("\",");
        sb.append("\"result\":\"").append(escape(ev.result())).append("\",");
        sb.append("\"details\":\"").append(escape(ev.details())).append("\"");
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private void openWriter() throws IOException {
        Files.createDirectories(dir);
        this.writer = Files.newBufferedWriter(activeFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }

    private void rotateIfNeeded() throws IOException {
        long bytes = Files.size(activeFile);
        long limit = (long) cfg.auditMaxFileSizeMB * 1024L * 1024L;
        if (bytes < limit) return;

        // Pick next slot audit-1.log, audit-2.log, ...
        writer.flush();
        writer.close();
        int idx = 1;
        Path candidate;
        String ext = cfg.auditFormat.equalsIgnoreCase("json") ? ".json" : ".log";
        while (true) {
            candidate = dir.resolve("audit-" + idx + ext);
            if (!Files.exists(candidate)) break;
            idx++;
            if (idx > 100_000) break;
        }
        Files.move(activeFile, candidate);
        openWriter();
        FantasticKits.LOGGER.info("[{}] Audit log rotated -> {}", Reference.MOD_NAME, candidate.getFileName());
    }
}
