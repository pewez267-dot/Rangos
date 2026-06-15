package com.pewez.fantasticshortcuts.audit;

import com.pewez.fantasticshortcuts.FantasticShortcuts;
import com.pewez.fantasticshortcuts.config.FSConfig;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sistema de auditoría con rotación diaria.
 *
 * <p>Escribe en {@code config/fantasticshortcuts/audit/audit-YYYY-MM-DD.log}. Cada línea tiene el
 * formato:
 * <pre>[2026-06-15T12:34:56] EXECUTE_SHORTCUT actor=Steve(uuid) group=vip | /gc -> /gamemode creative</pre>
 *
 * <p>Es thread-safe (sincronizado) y nunca lanza excepciones hacia el llamante: un fallo de
 * auditoría jamás debe romper la ejecución del juego.
 */
public final class AuditLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Path auditDir;
    private final Object lock = new Object();

    public AuditLog(Path baseDir) {
        this.auditDir = baseDir.resolve("audit");
    }

    public Path directory() {
        return auditDir;
    }

    /**
     * Registra un evento de auditoría.
     *
     * @param event   tipo de evento
     * @param actor   descripción del actor (nombre + uuid, o "SERVER"/"UNKNOWN")
     * @param group   grupo primario de LuckPerms si está disponible (puede ser {@code null})
     * @param details detalle libre (atajo afectado, comando, motivo, etc.)
     */
    public void record(AuditEvent event, String actor, String group, String details) {
        if (!FSConfig.auditEnabled()) {
            return;
        }
        final String line = "[" + LocalDateTime.now().format(TIME) + "] "
                + event.name()
                + " actor=" + safe(actor)
                + " group=" + safe(group == null ? "-" : group)
                + " | " + safe(details)
                + System.lineSeparator();
        synchronized (lock) {
            try {
                Files.createDirectories(auditDir);
                final Path file = auditDir.resolve("audit-" + LocalDate.now().format(DAY) + ".log");
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    writer.write(line);
                }
            } catch (IOException e) {
                FantasticShortcuts.LOGGER.warn("[F-Shortcuts] No se pudo escribir en el log de auditoria: {}", e.toString());
            }
        }
    }

    private static String safe(String value) {
        if (value == null) {
            return "-";
        }
        // Evita que un valor con saltos de línea contamine el formato del log.
        return value.replace("\n", "\\n").replace("\r", "\\r");
    }
}
