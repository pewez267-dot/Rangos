package com.fsmobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuracion (autoritativa del servidor) del control de mobs. Se lee en vivo desde el manejador
 * de spawns, asi que cualquier cambio se aplica al momento sin reiniciar. Persistencia en JSON.
 *
 * Filosofia de rendimiento: si el tope de un mob/categoria es -1 (sin limite) y el multiplicador es
 * 1.0, el manejador de spawns sale de inmediato SIN escanear entidades => coste cero por defecto.
 */
public final class MobControl {

    private MobControl() {}

    // Categorias estandar que se muestran/gestionan (nombres oficiales de MobCategory).
    public static final String[] CATEGORIES = {
            "monster", "creature", "ambient", "water_creature",
            "water_ambient", "axolotls", "underground_water_creature"
    };

    private static volatile int radius = 32;
    private static volatile double multiplier = 1.0;
    private static final Map<String, Integer> CATEGORY_CAPS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> TYPE_CAPS = new ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        for (String c : CATEGORIES) {
            CATEGORY_CAPS.put(c, -1);
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("fantasticmobs.json");
    }

    // ------------------------------------------------------------ getters

    public static int getRadius() {
        return radius;
    }

    public static double getMultiplier() {
        return multiplier;
    }

    /** Tope de una categoria (-1 = sin limite). */
    public static int categoryCap(MobCategory cat) {
        Integer v = CATEGORY_CAPS.get(cat.getName());
        return v == null ? -1 : v;
    }

    public static int categoryCap(String name) {
        Integer v = CATEGORY_CAPS.get(name);
        return v == null ? -1 : v;
    }

    /** Tope de un mob concreto por id, o null si no tiene override. */
    public static Integer typeCap(String id) {
        return id == null ? null : TYPE_CAPS.get(id);
    }

    public static Map<String, Integer> categoryCaps() {
        return CATEGORY_CAPS;
    }

    public static Map<String, Integer> typeCaps() {
        return TYPE_CAPS;
    }

    // ------------------------------------------------------------ setters (servidor)

    public static void setRadius(int r) {
        radius = Math.max(4, Math.min(128, r));
    }

    public static void setMultiplier(double m) {
        multiplier = Math.max(0.0, Math.min(1.0, m));
    }

    public static void setCategoryCap(String name, int cap) {
        CATEGORY_CAPS.put(name, cap < 0 ? -1 : cap);
    }

    public static void setTypeCap(String id, int cap) {
        if (id == null) {
            return;
        }
        if (cap < 0) {
            TYPE_CAPS.remove(id);
        } else {
            TYPE_CAPS.put(id, cap);
        }
    }

    public static void reset() {
        radius = 32;
        multiplier = 1.0;
        CATEGORY_CAPS.clear();
        for (String c : CATEGORIES) {
            CATEGORY_CAPS.put(c, -1);
        }
        TYPE_CAPS.clear();
    }

    // ------------------------------------------------------------ persistencia

    public static synchronized void load() {
        Path path = file();
        if (!Files.exists(path)) {
            save();
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("radius")) {
                setRadius(root.get("radius").getAsInt());
            }
            if (root.has("multiplier")) {
                setMultiplier(root.get("multiplier").getAsDouble());
            }
            if (root.has("categoryCaps")) {
                JsonObject o = root.getAsJsonObject("categoryCaps");
                for (String k : o.keySet()) {
                    CATEGORY_CAPS.put(k, o.get(k).getAsInt());
                }
            }
            if (root.has("typeCaps")) {
                JsonObject o = root.getAsJsonObject("typeCaps");
                TYPE_CAPS.clear();
                for (String k : o.keySet()) {
                    TYPE_CAPS.put(k, o.get(k).getAsInt());
                }
            }
        } catch (Exception ex) {
            FSMobs.LOGGER.error("[FantasticMobs] No se pudo leer {}: {}", path, ex.toString());
        }
    }

    public static synchronized void save() {
        JsonObject root = new JsonObject();
        root.addProperty("radius", radius);
        root.addProperty("multiplier", multiplier);
        JsonObject cc = new JsonObject();
        for (Map.Entry<String, Integer> e : CATEGORY_CAPS.entrySet()) {
            cc.addProperty(e.getKey(), e.getValue());
        }
        root.add("categoryCaps", cc);
        JsonObject tc = new JsonObject();
        for (Map.Entry<String, Integer> e : TYPE_CAPS.entrySet()) {
            tc.addProperty(e.getKey(), e.getValue());
        }
        root.add("typeCaps", tc);
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            FSMobs.LOGGER.error("[FantasticMobs] No se pudo guardar {}: {}", file(), ex.toString());
        }
    }

    // ------------------------------------------------------------ sincronizacion GUI

    /** Escribe todo el estado en el buffer (para enviar al cliente que abre la GUI). */
    public static void writeSnapshot(FriendlyByteBuf buf) {
        buf.writeVarInt(radius);
        buf.writeDouble(multiplier);
        buf.writeVarInt(CATEGORY_CAPS.size());
        for (Map.Entry<String, Integer> e : CATEGORY_CAPS.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeVarInt(e.getValue());
        }
        buf.writeVarInt(TYPE_CAPS.size());
        for (Map.Entry<String, Integer> e : TYPE_CAPS.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeVarInt(e.getValue());
        }
    }

    /** Snapshot inmutable para el cliente. */
    public record Snapshot(int radius, double multiplier,
                           Map<String, Integer> categoryCaps, Map<String, Integer> typeCaps) {

        public static Snapshot read(FriendlyByteBuf buf) {
            int r = buf.readVarInt();
            double m = buf.readDouble();
            Map<String, Integer> cc = new LinkedHashMap<>();
            int n = buf.readVarInt();
            for (int i = 0; i < n; i++) {
                cc.put(buf.readUtf(), buf.readVarInt());
            }
            Map<String, Integer> tc = new LinkedHashMap<>();
            int t = buf.readVarInt();
            for (int i = 0; i < t; i++) {
                tc.put(buf.readUtf(), buf.readVarInt());
            }
            return new Snapshot(r, m, cc, tc);
        }

        public List<String> sortedTypeIds() {
            List<String> ids = new ArrayList<>(typeCaps.keySet());
            ids.sort(String::compareTo);
            return ids;
        }
    }
}
