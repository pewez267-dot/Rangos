package com.fantasticshortcuts.data;

import com.fantasticshortcuts.FantasticShortcuts;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Reads and writes {@code config/fantasticshortcuts/shortcuts.json} with Gson.
 *
 * <p>Writes happen asynchronously on a single dedicated thread and atomically (temp file
 * + move), so concurrent admin operations never corrupt the file and the game thread is
 * never blocked by disk I/O.</p>
 */
public final class ShortcutSerializer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Type ROOT_TYPE = new TypeToken<Root>() {
    }.getType();

    private final ExecutorService writer = Executors.newSingleThreadExecutor(threadFactory());

    private static ThreadFactory threadFactory() {
        return r -> {
            final Thread t = new Thread(r, "FantasticShortcuts-IO");
            t.setDaemon(true);
            return t;
        };
    }

    private static final class Root {
        List<Shortcut> shortcuts = new ArrayList<>();
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("fantasticshortcuts").resolve("shortcuts.json");
    }

    /** Synchronously loads the shortcut list (called once at startup / reload). */
    public List<Shortcut> load() {
        final Path path = file();
        if (!Files.isRegularFile(path)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            final Root root = GSON.fromJson(reader, ROOT_TYPE);
            if (root == null || root.shortcuts == null) {
                return new ArrayList<>();
            }
            final List<Shortcut> out = new ArrayList<>();
            for (final Shortcut s : root.shortcuts) {
                if (s != null) {
                    out.add(s);
                }
            }
            return out;
        } catch (final Exception e) {
            FantasticShortcuts.LOGGER.error("[FantasticShortcuts] shortcuts.json invalido, se ignora: {}", e.toString());
            return new ArrayList<>();
        }
    }

    /** Asynchronously persists a snapshot of the shortcut list. */
    public void saveAsync(final List<Shortcut> snapshot) {
        final Root root = new Root();
        root.shortcuts = new ArrayList<>(snapshot);
        this.writer.submit(() -> writeNow(root));
    }

    private static synchronized void writeNow(final Root root) {
        try {
            final Path path = file();
            Files.createDirectories(path.getParent());
            final Path tmp = path.resolveSibling("shortcuts.json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, ROOT_TYPE, w);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final Exception atomicFailure) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final Exception e) {
            FantasticShortcuts.LOGGER.error("[FantasticShortcuts] Error guardando shortcuts.json: {}", e.toString());
        }
    }

    public void shutdown() {
        this.writer.shutdown();
        try {
            if (!this.writer.awaitTermination(5, TimeUnit.SECONDS)) {
                this.writer.shutdownNow();
            }
        } catch (final InterruptedException e) {
            this.writer.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
