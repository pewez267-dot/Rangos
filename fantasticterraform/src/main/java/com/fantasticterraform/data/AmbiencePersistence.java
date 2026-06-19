package com.fantasticterraform.data;

import com.fantasticterraform.FantasticTerraform;
import com.fantasticterraform.ambience.AmbienceZone;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia de zonas de ambiente en {@code config/fantasticterraform/ambience.json}
 * mediante Gson.
 */
public final class AmbiencePersistence {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<AmbienceZone>>() {
    }.getType();

    private AmbiencePersistence() {
    }

    private static File file() {
        File dir = new File(FMLPaths.CONFIGDIR.get().toFile(), "fantasticterraform");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "ambience.json");
    }

    public static List<AmbienceZone> load() {
        File f = file();
        if (!f.isFile()) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(f)) {
            List<AmbienceZone> list = GSON.fromJson(reader, LIST_TYPE);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            FantasticTerraform.LOGGER.error("No se pudo leer ambience.json", e);
            return new ArrayList<>();
        }
    }

    public static void save(List<AmbienceZone> zones) {
        try (FileWriter writer = new FileWriter(file())) {
            GSON.toJson(zones, LIST_TYPE, writer);
        } catch (Exception e) {
            FantasticTerraform.LOGGER.error("No se pudo escribir ambience.json", e);
        }
    }
}
