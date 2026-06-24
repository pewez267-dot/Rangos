package com.fantasticterraform.client;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Presets de brush guardables del lado cliente. Captura toda la configuracion del brush
 * activo (tipo, radio, intensidad, altura, falloff, bloques, mezcla, profundidad, hueco)
 * y la persiste en {@code config/fantasticterraform/brush_presets.json}, para reutilizar
 * pinceles favoritos entre sesiones.
 */
public final class ClientBrushPresets {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Preset>>() {
    }.getType();

    private static LinkedHashMap<String, Preset> presets;

    private ClientBrushPresets() {
    }

    /** Datos serializables de un preset. */
    public static final class Preset {
        public String brushId = "sphere";
        public int radius = 5;
        public double intensity = 0.5D;
        public int height = 5;
        public String block = "minecraft:stone";
        public int falloff = 2;
        public String secondaryBlock = "minecraft:cobblestone";
        public double mix = 0.0D;
        public int depth = 1;
        public boolean hollow = false;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("fantasticterraform").resolve("brush_presets.json");
    }

    private static synchronized LinkedHashMap<String, Preset> presets() {
        if (presets == null) {
            presets = new LinkedHashMap<>();
            Path p = file();
            if (Files.isRegularFile(p)) {
                try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    LinkedHashMap<String, Preset> loaded = GSON.fromJson(r, MAP_TYPE);
                    if (loaded != null) {
                        presets.putAll(loaded);
                    }
                } catch (Exception ignored) {
                    // archivo corrupto: empezar limpio.
                }
            }
        }
        return presets;
    }

    private static void persist() {
        Path p = file();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                GSON.toJson(presets(), MAP_TYPE, w);
            }
        } catch (Exception ignored) {
            // Si falla la escritura, los presets siguen disponibles en memoria esta sesion.
        }
    }

    public static List<String> names() {
        return new ArrayList<>(presets().keySet());
    }

    /** Guarda el brush activo bajo un nombre. */
    public static void save(String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        Preset preset = new Preset();
        preset.brushId = ClientToolState.brushId;
        preset.radius = ClientToolState.brushRadius;
        preset.intensity = ClientToolState.brushIntensity;
        preset.height = ClientToolState.brushHeight;
        preset.block = ClientToolState.brushBlock;
        preset.falloff = ClientToolState.brushFalloff;
        preset.secondaryBlock = ClientToolState.brushSecondaryBlock;
        preset.mix = ClientToolState.brushMix;
        preset.depth = ClientToolState.brushDepth;
        preset.hollow = ClientToolState.brushHollow;
        presets().put(name.trim(), preset);
        persist();
    }

    /** Aplica un preset al estado del brush. */
    public static boolean apply(String name) {
        Preset preset = presets().get(name);
        if (preset == null) {
            return false;
        }
        ClientToolState.brushId = preset.brushId;
        ClientToolState.brushRadius = preset.radius;
        ClientToolState.brushIntensity = preset.intensity;
        ClientToolState.brushHeight = preset.height;
        ClientToolState.brushBlock = preset.block;
        ClientToolState.brushFalloff = preset.falloff;
        ClientToolState.brushSecondaryBlock = preset.secondaryBlock;
        ClientToolState.brushMix = preset.mix;
        ClientToolState.brushDepth = preset.depth;
        ClientToolState.brushHollow = preset.hollow;
        return true;
    }

    public static void delete(String name) {
        if (presets().remove(name) != null) {
            persist();
        }
    }

    public static boolean isEmpty() {
        return presets().isEmpty();
    }
}
