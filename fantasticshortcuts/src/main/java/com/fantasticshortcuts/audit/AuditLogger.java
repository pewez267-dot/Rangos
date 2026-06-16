package com.fantasticshortcuts.audit;

import com.fantasticshortcuts.FantasticShortcuts;
import com.fantasticshortcuts.config.ShortcutsConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asynchronous, thread-safe audit trail written to
 * {@code config/fantasticshortcuts/audit/audit.log}.
 *
 * <p>All writes are funnelled through a single dedicated daemon thread, so concurrent
 * callers never interleave a line and the game thread never blocks on disk I/O.</p>
 */
public final class AuditLogger {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(r, "FantasticShortcuts-Audit-" + this.counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    private AuditLogger() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("fantasticshortcuts").resolve("audit").resolve("audit.log");
    }

    private static String ts() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private static String clean(final String v) {
        return v == null ? "-" : v.replace('\n', ' ').replace('\r', ' ');
    }

    /** Builds and asynchronously appends one audit line: {@code [ts] [EVENT] k=v ...}. */
    private static void write(final String event, final String fields) {
        if (!ShortcutsConfig.auditEnabled()) {
            return;
        }
        final String line = "[" + ts() + "] [" + event + "] " + fields + System.lineSeparator();
        EXECUTOR.submit(() -> {
            try {
                final Path path = file();
                Files.createDirectories(path.getParent());
                Files.write(path, line.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (final Exception e) {
                FantasticShortcuts.LOGGER.error("[FantasticShortcuts] No se pudo escribir audit.log: {}", e.toString());
            }
        });
    }

    public static void shortcutCreated(final String name, final String alias, final String original, final String byName, final UUID byUuid) {
        write("SHORTCUT_CREATED", "nombre=" + clean(name) + " alias=" + clean(alias)
                + " original=" + clean(original) + " creado_por=" + clean(byName) + " uuid=" + uuid(byUuid));
    }

    public static void shortcutEdited(final String name, final String changes, final String byName, final UUID byUuid) {
        write("SHORTCUT_EDITED", "nombre=" + clean(name) + " cambios=[" + clean(changes) + "]"
                + " editado_por=" + clean(byName) + " uuid=" + uuid(byUuid));
    }

    public static void shortcutDeleted(final String name, final String alias, final String byName, final UUID byUuid) {
        write("SHORTCUT_DELETED", "nombre=" + clean(name) + " alias=" + clean(alias)
                + " eliminado_por=" + clean(byName) + " uuid=" + uuid(byUuid));
    }

    public static void shortcutUsed(final String alias, final String resulting, final String playerName, final UUID uuid, final String pos, final String dim) {
        write("SHORTCUT_USED", "jugador=" + clean(playerName) + " uuid=" + uuid(uuid)
                + " alias=" + clean(alias) + " comando=" + clean(resulting) + " pos={" + clean(pos) + "} dim={" + clean(dim) + "}");
    }

    public static void shortcutDenied(final String alias, final String playerName, final UUID uuid, final String reason) {
        write("SHORTCUT_DENIED", "jugador=" + clean(playerName) + " uuid=" + uuid(uuid)
                + " alias=" + clean(alias) + " motivo=" + clean(reason));
    }

    public static void conflictDetected(final String alias, final String existing, final String action) {
        write("CONFLICT_DETECTED", "alias=" + clean(alias) + " shortcut_existente=" + clean(existing) + " accion=" + clean(action));
    }

    public static void invalidAttempt(final String description, final String playerName, final UUID uuid) {
        write("INVALID_ATTEMPT", "jugador=" + clean(playerName) + " uuid=" + uuid(uuid) + " detalle=" + clean(description));
    }

    private static String uuid(final UUID uuid) {
        return uuid == null ? "-" : uuid.toString();
    }

    /** Flushes and stops the writer thread on server shutdown. */
    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (final InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
