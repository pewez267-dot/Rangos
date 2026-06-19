package com.fantasticterraform.data;

import com.fantasticterraform.FantasticTerraform;
import com.fantasticterraform.ambience.AmbienceZone;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia de zonas de ambiente POR MUNDO, en
 * {@code <mundo>/fantasticterraform/ambience.json}.
 */
public final class AmbiencePersistence {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<AmbienceZone>>() {
    }.getType();

    private AmbiencePersistence() {
    }

    private static File file(MinecraftServer server) {
        File dir = server.getWorldPath(LevelResource.ROOT).resolve("fantasticterraform").toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "ambience.json");
    }

    public static List<AmbienceZone> load(MinecraftServer server) {
        File f = file(server);
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

    public static void save(MinecraftServer server, List<AmbienceZone> zones) {
        try (FileWriter writer = new FileWriter(file(server))) {
            GSON.toJson(zones, LIST_TYPE, writer);
        } catch (Exception e) {
            FantasticTerraform.LOGGER.error("No se pudo escribir ambience.json", e);
        }
    }
}
