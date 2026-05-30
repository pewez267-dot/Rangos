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
import net.minecraft.class_3518;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;

public class RemoteAnimationManager {
   private static final Set<class_2960> registeredAnimations = new HashSet<>();

   public static void registerCachedAnimations(Path cacheDir) {
      Path animationsDir = cacheDir.resolve("assets/blockpops/geckolib/animations");
      if (!Files.isDirectory(animationsDir)) {
         BlockPopsMod.logDebug("No cached animations directory found at {}", animationsDir);
      } else {
         Map<class_2960, BakedAnimations> animationCache = GeckoLibCache.getBakedAnimations();
         int registered = 0;

         try {
            for (Path animFile : Files.walk(animationsDir).filter(p -> p.toString().endsWith(".animation.json")).toList()) {
               try {
                  Path relative = animationsDir.relativize(animFile);
                  String relPath = relative.toString().replace('\\', '/');
                  class_2960 location = class_2960.method_60655("blockpops", "animations/" + relPath);
                  String jsonStr = Files.readString(animFile, StandardCharsets.UTF_8);
                  JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
                  BakedAnimations bakedAnimations = (BakedAnimations)KeyFramesAdapter.GEO_GSON
                     .fromJson(class_3518.method_15296(json, "animations"), BakedAnimations.class);
                  animationCache.put(location, bakedAnimations);
                  registeredAnimations.add(location);
                  registered++;
                  BlockPopsMod.logDebug("Registered remote animation: {}", location);
               } catch (Exception var13) {
                  BlockPopsMod.LOGGER.warn("Failed to load cached animation {}: {}", animFile.getFileName(), var13.getMessage());
               }
            }
         } catch (IOException var14) {
            BlockPopsMod.LOGGER.error("Failed to scan cached animations directory: {}", var14.getMessage());
         }

         if (registered > 0) {
            BlockPopsMod.LOGGER.info("Registered {} remote animation(s)", registered);
         }
      }
   }

   public static void clearRegisteredAnimations() {
      if (!registeredAnimations.isEmpty()) {
         Map<class_2960, BakedAnimations> animationCache = GeckoLibCache.getBakedAnimations();

         for (class_2960 location : registeredAnimations) {
            animationCache.remove(location);
         }

         BlockPopsMod.logDebug("Cleared {} remote animation(s)", registeredAnimations.size());
         registeredAnimations.clear();
      }
   }
}
