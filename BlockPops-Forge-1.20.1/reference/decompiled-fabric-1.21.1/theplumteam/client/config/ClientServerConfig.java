package com.theplumteam.client.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.theplumteam.platform.PlatformHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientServerConfig {
   private static int regularTokenCooldownHours = 3;
   private static int maxRegularTokens = 3;
   private static int guaranteedTokenResetHour = 18;
   private static Set<String> hiddenCollections = new HashSet<>();
   private static Set<String> localHiddenCollections = new HashSet<>();
   private static Set<String> enabledRemoteCollections = new HashSet<>();

   public static void update(int regularTokenCooldownHours, int maxRegularTokens, int guaranteedTokenResetHour) {
      ClientServerConfig.regularTokenCooldownHours = regularTokenCooldownHours;
      ClientServerConfig.maxRegularTokens = maxRegularTokens;
      ClientServerConfig.guaranteedTokenResetHour = guaranteedTokenResetHour;
   }

   public static void updateHiddenCollections(List<String> hidden) {
      hiddenCollections = hidden != null ? new HashSet<>(hidden) : new HashSet<>();
   }

   public static int getRegularTokenCooldownHours() {
      return regularTokenCooldownHours;
   }

   public static int getMaxRegularTokens() {
      return maxRegularTokens;
   }

   public static int getGuaranteedTokenResetHour() {
      return guaranteedTokenResetHour;
   }

   public static boolean isCollectionHidden(String collectionId) {
      return hiddenCollections.contains(collectionId) || localHiddenCollections.contains(collectionId);
   }

   public static Set<String> getHiddenCollections() {
      return Collections.unmodifiableSet(hiddenCollections);
   }

   public static void updateEnabledRemoteCollections(List<String> enabled) {
      enabledRemoteCollections = enabled != null ? new HashSet<>(enabled) : new HashSet<>();
   }

   public static Set<String> getEnabledRemoteCollections() {
      return Collections.unmodifiableSet(enabledRemoteCollections);
   }

   public static boolean isRemoteCollectionEnabled(String collectionId) {
      return enabledRemoteCollections.contains(collectionId);
   }

   public static Set<String> getLocalHiddenCollections() {
      return Collections.unmodifiableSet(localHiddenCollections);
   }

   public static void updateLocalHiddenCollections(Set<String> hidden) {
      localHiddenCollections = hidden != null ? new HashSet<>(hidden) : new HashSet<>();
      saveLocalHiddenCollections();
   }

   public static boolean isLocalCollectionHidden(String collectionId) {
      return localHiddenCollections.contains(collectionId);
   }

   private static Path getLocalHiddenPath() {
      return PlatformHelper.getConfigDirectory().resolve("blockpops-hidden.json");
   }

   public static void loadLocalHiddenCollections() {
      try {
         Path path = getLocalHiddenPath();
         if (Files.exists(path)) {
            String json = Files.readString(path);
            JsonObject obj = (JsonObject)new Gson().fromJson(json, JsonObject.class);
            if (obj.has("hidden")) {
               Set<String> loaded = new HashSet<>();
               JsonArray arr = obj.getAsJsonArray("hidden");

               for (int i = 0; i < arr.size(); i++) {
                  loaded.add(arr.get(i).getAsString());
               }

               localHiddenCollections = loaded;
            }
         }
      } catch (Exception var6) {
      }
   }

   private static void saveLocalHiddenCollections() {
      try {
         Path path = getLocalHiddenPath();
         JsonObject obj = new JsonObject();
         JsonArray arr = new JsonArray();

         for (String id : localHiddenCollections) {
            arr.add(id);
         }

         obj.add("hidden", arr);
         Files.writeString(path, new Gson().toJson(obj));
      } catch (Exception var5) {
      }
   }

   public static void reset() {
      regularTokenCooldownHours = 3;
      maxRegularTokens = 3;
      guaranteedTokenResetHour = 18;
      hiddenCollections = new HashSet<>();
      enabledRemoteCollections = new HashSet<>();
   }
}
