package com.fantasticaudit.logging;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/**
 * Tracks the last-known username per UUID so a name change can be detected and the player's old
 * and new log files can be cross-linked.
 *
 * <p>Because log files are named by username, a Mojang name change would otherwise split a
 * player's history across two files with no connection between them. This tracker persists a
 * small {@code config/fantasticaudit/known_names.properties} map (UUID → name); on login the
 * caller compares and, when the name changed, writes a {@code NAME_CHANGE} line into both the old
 * and new files. The stable UUID is the join key.</p>
 */
public final class AliasTracker {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AliasTracker INSTANCE = new AliasTracker();

    private final Properties names = new Properties();
    private volatile Path file;

    private AliasTracker() {
    }

    public static AliasTracker get() {
        return INSTANCE;
    }

    /** Loads the persisted UUID → name map. */
    public synchronized void init(Path baseDir) {
        this.file = baseDir.resolve("known_names.properties");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                names.load(reader);
            } catch (IOException e) {
                LOGGER.error("[FantasticAudit] Failed to load {}", file, e);
            }
        }
    }

    /**
     * Records the current username for a UUID and reports the previous one if it changed.
     *
     * @return the previous username when it differs from {@code name} (i.e. a rename happened),
     * otherwise {@code null} (first sighting or unchanged name)
     */
    public synchronized String recordAndGetPrevious(UUID uuid, String name) {
        if (uuid == null || name == null || name.isEmpty() || file == null) {
            return null;
        }
        String key = uuid.toString();
        String previous = names.getProperty(key);
        if (name.equals(previous)) {
            return null;
        }
        names.setProperty(key, name);
        persist();
        return previous; // null on first sighting; old name on a genuine rename
    }

    private void persist() {
        Path target = this.file;
        if (target == null) {
            return;
        }
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = Files.createTempFile(parent, "known_names", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                names.store(writer, "Fantastic Audit - last known username per UUID");
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException atomicUnsupported) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            LOGGER.error("[FantasticAudit] Failed to persist {}", target, e);
            AuditLogger.get().system("[ALIAS_PERSIST_ERROR] error=" + e.getMessage());
        }
    }
}
