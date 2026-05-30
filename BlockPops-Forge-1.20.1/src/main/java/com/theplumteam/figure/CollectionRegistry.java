package com.theplumteam.figure;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.theplumteam.BlockPopsMod;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class CollectionRegistry {
   private static final Gson GSON = new Gson();
   private static final Map<String, FigureCollection> collections = new LinkedHashMap<>();
   private static final Map<String, FigureDefinition> figuresById = new HashMap<>();
   private static final Map<String, FigureCollection> dynamicCollections = new LinkedHashMap<>();
   private static boolean initialized = false;

   public static void loadCollections(ResourceManager resourceManager) {
      collections.clear();
      figuresById.clear();

      for (Entry<String, FigureCollection> entry : dynamicCollections.entrySet()) {
         registerCollectionInternal(entry.getValue(), false);
      }

      try {
         Map<ResourceLocation, Resource> resources = resourceManager.listResources("collections", locationx -> locationx.getPath().endsWith(".json"));
         BlockPopsMod.logDebug("Loading figure collections...");

         for (Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();

            try (
               InputStream stream = entry.getValue().open();
               BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            ) {
               JsonObject json = GSON.fromJson(reader, JsonObject.class);
               FigureCollection collection = FigureCollection.fromJson(json);
               collections.put(collection.getId(), collection);

               for (FigureDefinition figure : collection.getFigures()) {
                  String fullId = collection.getId() + ":" + figure.getId();
                  figuresById.put(fullId, figure);
               }

               BlockPopsMod.logDebug("Loaded collection '{}' with {} figures from {}", collection.getName(), collection.getFigures().size(), location);
            } catch (Exception var16) {
               BlockPopsMod.LOGGER.error("Failed to load collection from {}: {}", location, var16.getMessage());
            }
         }

         initialized = true;
         BlockPopsMod.logDebug("Loaded {} collections with {} total figures", collections.size(), figuresById.size());
      } catch (Exception var17) {
         BlockPopsMod.LOGGER.error("Failed to load collections: {}", var17.getMessage());
      }
   }

   public static Optional<FigureCollection> getCollection(String collectionId) {
      return Optional.ofNullable(collections.get(collectionId));
   }

   public static Optional<FigureDefinition> getFigure(String collectionId, String figureId) {
      String fullId = collectionId + ":" + figureId;
      return Optional.ofNullable(figuresById.get(fullId));
   }

   public static Collection<FigureCollection> getAllCollections() {
      return Collections.unmodifiableCollection(collections.values());
   }

   public static Set<String> getCollectionIds() {
      return Collections.unmodifiableSet(collections.keySet());
   }

   public static boolean hasCollection(String collectionId) {
      return collections.containsKey(collectionId);
   }

   public static boolean isInitialized() {
      return initialized;
   }

   public static Optional<FigureCollection> getDefaultCollection() {
      return collections.containsKey("default") ? Optional.of(collections.get("default")) : collections.values().stream().findFirst();
   }

   public static void registerDynamicCollection(FigureCollection collection) {
      dynamicCollections.put(collection.getId(), collection);
      registerCollectionInternal(collection, true);
   }

   private static void registerCollectionInternal(FigureCollection collection, boolean isDynamic) {
      collections.put(collection.getId(), collection);

      for (FigureDefinition figure : collection.getFigures()) {
         String fullId = collection.getId() + ":" + figure.getId();
         figuresById.put(fullId, figure);
      }

      if (isDynamic) {
         BlockPopsMod.logDebug("Registered dynamic collection '{}' with {} figures", collection.getName(), collection.getFigures().size());
      }
   }
}
