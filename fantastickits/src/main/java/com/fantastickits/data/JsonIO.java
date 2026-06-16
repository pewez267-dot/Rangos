package com.fantastickits.data;

import com.fantastickits.FantasticKits;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Small Gson helper that reads and writes a {@link JsonObject} to disk.
 *
 * <p>Writes are <strong>atomic</strong>: the payload is first written to a sibling
 * {@code .tmp} file which is then moved over the target. This prevents a crash or
 * concurrent reader from ever observing a half-written file.</p>
 */
public final class JsonIO {

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private JsonIO() {
    }

    /** Reads a JSON object from {@code path}, returning an empty object if missing or invalid. */
    public static JsonObject read(final Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return new JsonObject();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            final var parsed = JsonParser.parseReader(reader);
            if (parsed != null && parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
            FantasticKits.LOGGER.warn("[FantasticKits] {} no contiene un objeto JSON valido; se ignora.", path.getFileName());
            return new JsonObject();
        } catch (final Exception e) {
            FantasticKits.LOGGER.error("[FantasticKits] Error leyendo {}: {}", path, e.toString());
            return new JsonObject();
        }
    }

    /** Writes {@code root} to {@code path} atomically (temp file + move). */
    public static void write(final Path path, final JsonObject root) {
        try {
            Files.createDirectories(path.getParent());
            final Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final IOException atomicFailure) {
                // Some filesystems do not support atomic moves; fall back to a plain replace.
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final Exception e) {
            FantasticKits.LOGGER.error("[FantasticKits] Error guardando {}: {}", path, e.toString());
        }
    }
}
