package com.theplumteam.client.remote;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.theplumteam.BlockPopsMod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_2960;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

public class RemoteModelManager {
   private static final Set<class_2960> registeredModels = new HashSet<>();

   public static void registerCachedModels(Path cacheDir) {
      Path modelsDir = cacheDir.resolve("assets/blockpops/geckolib/models");
      if (!Files.isDirectory(modelsDir)) {
         BlockPopsMod.logDebug("No cached models directory found at {}", modelsDir);
      } else {
         Map<class_2960, BakedGeoModel> modelCache = GeckoLibCache.getBakedModels();
         int registered = 0;

         try {
            for (Path modelFile : Files.walk(modelsDir).filter(p -> p.toString().endsWith(".geo.json")).toList()) {
               try {
                  Path relative = modelsDir.relativize(modelFile);
                  String relPath = relative.toString().replace('\\', '/');
                  class_2960 location = class_2960.method_60655("blockpops", "geo/" + relPath);
                  String jsonStr = Files.readString(modelFile, StandardCharsets.UTF_8);
                  JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
                  Model model = (Model)KeyFramesAdapter.GEO_GSON.fromJson(json, Model.class);
                  GeometryTree tree = GeometryTree.fromModel(model);
                  BakedGeoModel bakedModel = BakedModelFactory.getForNamespace(location.method_12836()).constructGeoModel(tree);
                  modelCache.put(location, bakedModel);
                  registeredModels.add(location);
                  registered++;
                  BlockPopsMod.logDebug("Registered remote model: {}", location);
               } catch (Exception var15) {
                  BlockPopsMod.LOGGER.warn("Failed to load cached model {}: {}", modelFile.getFileName(), var15.getMessage());
               }
            }
         } catch (IOException var16) {
            BlockPopsMod.LOGGER.error("Failed to scan cached models directory: {}", var16.getMessage());
         }

         if (registered > 0) {
            BlockPopsMod.LOGGER.info("Registered {} remote geo model(s)", registered);
         }
      }
   }

   public static void clearRegisteredModels() {
      if (!registeredModels.isEmpty()) {
         Map<class_2960, BakedGeoModel> modelCache = GeckoLibCache.getBakedModels();

         for (class_2960 location : registeredModels) {
            modelCache.remove(location);
         }

         BlockPopsMod.logDebug("Cleared {} remote model(s)", registeredModels.size());
         registeredModels.clear();
      }
   }
}
