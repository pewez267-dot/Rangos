package com.fantasticchest.data;

import com.fantasticchest.FantasticChest;
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
 * Reads/writes {@code config/fantasticchest/chests.json} with Gson. Writes run on a
 * single dedicated thread (guaranteed write order) and atomically (temp file + move), so
 * the server thread never blocks on disk I/O and the file is never left half-written.
 */
public final class ChestSerializer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Type ROOT_TYPE = new TypeToken<Root>() {
    }.getType();

    private final ExecutorService writer = Executors.newSingleThreadExecutor(threadFactory());

    private static ThreadFactory threadFactory() {
        return r -> {
            final Thread t = new Thread(r, "FantasticChest-IO");
            t.setDaemon(true);
            return t;
        };
    }

    private static final class Root {
        List<ChestDefinition> chests = new ArrayList<>();
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("fantasticchest").resolve("chests.json");
    }

    /** Synchronously loads all definitions (once, at server start). */
    public List<ChestDefinition> load() {
        final Path path = file();
        if (!Files.isRegularFile(path)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            final Root root = GSON.fromJson(reader, ROOT_TYPE);
            if (root == null || root.chests == null) {
                return new ArrayList<>();
            }
            final List<ChestDefinition> out = new ArrayList<>();
            for (final ChestDefinition d : root.chests) {
                if (d != null && d.id != null && !d.id.isBlank()) {
                    out.add(d);
                }
            }
            return out;
        } catch (final Exception e) {
            FantasticChest.LOGGER.error("[FantasticChest] chests.json invalido, se ignora: {}", e.toString());
            return new ArrayList<>();
        }
    }

    /** Queues an asynchronous, atomic save of the given snapshot. */
    public void saveAsync(final List<ChestDefinition> snapshot) {
        final Root root = new Root();
        root.chests = new ArrayList<>(snapshot);
        this.writer.submit(() -> writeNow(root));
    }

    private static synchronized void writeNow(final Root root) {
        try {
            final Path path = file();
            Files.createDirectories(path.getParent());
            final Path tmp = path.resolveSibling("chests.json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, ROOT_TYPE, w);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final Exception atomicFailure) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final Exception e) {
            FantasticChest.LOGGER.error("[FantasticChest] Error guardando chests.json: {}", e.toString());
        }
    }

    /** Flushes pending writes and stops the writer thread (called on server shutdown). */
    public void shutdown() {
        this.writer.shutdown();
        try {
            if (!this.writer.awaitTermination(10, TimeUnit.SECONDS)) {
                this.writer.shutdownNow();
            }
        } catch (final InterruptedException e) {
            this.writer.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
