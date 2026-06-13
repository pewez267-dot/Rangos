package com.theplumteam.client.remote;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.class_310;
import org.jetbrains.annotations.Nullable;

public class RemoteAssetManager {
   private static final Gson GSON = new Gson();
   private static final String CDN_BASE_URL = "https://f003.backblazeb2.com/file/blockpops-assets";
   private static final String MANIFEST_PATH = "manifest.json";
   private static final int CONNECT_TIMEOUT = 10000;
   private static final int READ_TIMEOUT = 30000;
   private static Path cacheDir;
   private static boolean initialized = false;
   private static JsonObject cachedManifest = null;
   private static final AtomicBoolean syncing = new AtomicBoolean(false);
   private static String lastSyncError = null;
   private static long lastSyncTimestamp = 0L;
   private static int lastSyncDownloaded = 0;
   private static int lastSyncCached = 0;
   private static int lastSyncFailed = 0;
   private static int lastSyncTotalFiles = 0;
   private static final AtomicBoolean checking = new AtomicBoolean(false);
   private static Boolean updateAvailable = null;
   private static int remoteManifestVersion = -1;

   public static void init() {
      if (!initialized) {
         cacheDir = class_310.method_1551().field_1697.toPath().resolve("blockpops-cache");

         try {
            Files.createDirectories(cacheDir);
         } catch (IOException var1) {
            BlockPopsMod.LOGGER.error("Failed to create blockpops-cache directory: {}", var1.getMessage());
         }

         initialized = true;
         BlockPopsMod.logDebug("RemoteAssetManager initialized, cache at: {}", cacheDir);
      }
   }

   public static CompletableFuture<RemoteAssetManager.CodeResult> fetchCollectionByCode(String code) {
      return CompletableFuture.supplyAsync(
         () -> {
            try {
               String normalizedCode = code.trim().toLowerCase(Locale.ROOT);
               if (normalizedCode.isEmpty()) {
                  return null;
               } else {
                  String url = "https://f003.backblazeb2.com/file/blockpops-assets/codes/" + normalizedCode + ".json";
                  String json = downloadString(url);
                  if (json == null) {
                     return null;
                  } else {
                     JsonObject obj = (JsonObject)GSON.fromJson(json, JsonObject.class);
                     return obj.has("id") && obj.has("name")
                        ? new RemoteAssetManager.CodeResult(obj.get("id").getAsString(), obj.get("name").getAsString())
                        : null;
                  }
               }
            } catch (ConnectException | SocketTimeoutException var5) {
               BlockPopsMod.LOGGER.warn("CDN unreachable looking up code '{}': {}", code, var5.getMessage());
               return RemoteAssetManager.CodeResult.error("CDN unreachable - check your connection");
            } catch (Exception var6) {
               BlockPopsMod.LOGGER.warn("Failed to look up collection code '{}': {}", code, var6.getMessage());
               return RemoteAssetManager.CodeResult.error("Connection failed: " + var6.getMessage());
            }
         }
      );
   }

   public static CompletableFuture<List<String>> fetchAvailableCollections() {
      return CompletableFuture.supplyAsync(() -> {
         try {
            String manifestJson = downloadString("https://f003.backblazeb2.com/file/blockpops-assets/manifest.json");
            if (manifestJson == null) {
               return cachedManifest != null ? extractCollectionIds(cachedManifest) : Collections.emptyList();
            } else {
               cachedManifest = (JsonObject)GSON.fromJson(manifestJson, JsonObject.class);
               if (cacheDir != null) {
                  try {
                     Files.writeString(cacheDir.resolve("manifest.json"), manifestJson, StandardCharsets.UTF_8);
                  } catch (Exception var2) {
                  }
               }

               return extractCollectionIds(cachedManifest);
            }
         } catch (Exception var3) {
            BlockPopsMod.LOGGER.warn("Failed to fetch remote manifest: {}", var3.getMessage());
            return Collections.emptyList();
         }
      });
   }

   public static void syncEnabledCollections(Set<String> enabledIds) {
      syncEnabledCollections(enabledIds, null);
   }

   public static void syncEnabledCollections(Set<String> enabledIds, @Nullable Runnable onComplete) {
      if (initialized && !enabledIds.isEmpty()) {
         if (!syncing.compareAndSet(false, true)) {
            BlockPopsMod.LOGGER.debug("Remote sync already in progress, skipping duplicate call");
            if (onComplete != null) {
               class_310.method_1551().execute(onComplete);
            }
         } else {
            CompletableFuture.runAsync(
               () -> {
                  try {
                     lastSyncError = null;
                     BlockPopsMod.LOGGER.info("Syncing {} enabled remote collection(s)...", enabledIds.size());
                     JsonObject manifest = getManifest();
                     if (manifest != null) {
                        JsonArray files = manifest.getAsJsonArray("files");
                        int downloaded = 0;
                        int skipped = 0;
                        int failed = 0;

                        for (JsonElement fileElement : files) {
                           JsonObject fileInfo = fileElement.getAsJsonObject();
                           String filePath = fileInfo.get("path").getAsString();
                           String expectedSha256 = fileInfo.get("sha256").getAsString();
                           if (isFileForEnabledCollection(filePath, enabledIds)) {
                              Path localFile = cacheDir.resolve(filePath);
                              if (Files.exists(localFile) && sha256(localFile).equals(expectedSha256)) {
                                 skipped++;
                              } else if (downloadFile("https://f003.backblazeb2.com/file/blockpops-assets/" + filePath, localFile)) {
                                 String actualHash = sha256(localFile);
                                 if (!actualHash.equals(expectedSha256)) {
                                    BlockPopsMod.LOGGER
                                       .warn(
                                          "Hash mismatch for {} (expected={}, actual={}), keeping file",
                                          new Object[]{filePath, expectedSha256.substring(0, 8), actualHash.substring(0, 8)}
                                       );
                                 }

                                 downloaded++;
                              } else {
                                 failed++;
                              }
                           }
                        }

                        BlockPopsMod.LOGGER.info("Remote sync complete: {} downloaded, {} cached, {} failed", new Object[]{downloaded, skipped, failed});
                        lastSyncTimestamp = System.currentTimeMillis();
                        lastSyncDownloaded = downloaded;
                        lastSyncCached = skipped;
                        lastSyncFailed = failed;
                        lastSyncTotalFiles = downloaded + skipped + failed;
                        if (failed > 0) {
                           lastSyncError = failed + " file(s) failed to download";
                        }

                        updateAvailable = null;
                        loadCachedCollections(enabledIds);
                        return;
                     }

                     BlockPopsMod.LOGGER.warn("Could not get remote manifest");
                     lastSyncError = "Could not reach CDN - using cached data";
                     loadCachedCollections(enabledIds);
                  } catch (Exception var17) {
                     BlockPopsMod.LOGGER.error("Failed to sync remote collections: {}", var17.getMessage());
                     lastSyncError = "Sync failed: " + var17.getMessage();
                     return;
                  } finally {
                     syncing.set(false);
                     if (onComplete != null) {
                        class_310.method_1551().execute(onComplete);
                     }
                  }
               }
            );
         }
      } else {
         if (onComplete != null) {
            class_310.method_1551().execute(onComplete);
         }
      }
   }

   private static boolean isFileForEnabledCollection(String filePath, Set<String> enabledIds) {
      if (filePath.startsWith("data/blockpops/collections/")) {
         String filename = filePath.substring(filePath.lastIndexOf(47) + 1);
         String id = filename.replace(".json", "");
         return enabledIds.contains(id);
      } else {
         for (String id : enabledIds) {
            if (filePath.contains("/" + id + "/")
               || filePath.contains("/" + id + ".")
               || filePath.contains("_" + id + ".")
               || filePath.contains("_" + id + "_")) {
               return true;
            }
         }

         return false;
      }
   }

   private static JsonObject getManifest() {
      if (cachedManifest != null) {
         return cachedManifest;
      } else {
         if (cacheDir != null) {
            Path manifestFile = cacheDir.resolve("manifest.json");
            if (Files.exists(manifestFile)) {
               try {
                  String content = Files.readString(manifestFile, StandardCharsets.UTF_8);
                  cachedManifest = (JsonObject)GSON.fromJson(content, JsonObject.class);
                  return cachedManifest;
               } catch (Exception var4) {
               }
            }
         }

         try {
            String manifestJson = downloadString("https://f003.backblazeb2.com/file/blockpops-assets/manifest.json");
            if (manifestJson != null) {
               cachedManifest = (JsonObject)GSON.fromJson(manifestJson, JsonObject.class);
               if (cacheDir != null) {
                  try {
                     Files.writeString(cacheDir.resolve("manifest.json"), manifestJson, StandardCharsets.UTF_8);
                  } catch (Exception var2) {
                  }
               }

               return cachedManifest;
            }
         } catch (IOException var3) {
            BlockPopsMod.LOGGER.warn("Failed to download {}: {}", "manifest.json", var3.getMessage());
         }

         return null;
      }
   }

   private static List<String> extractCollectionIds(JsonObject manifest) {
      List<String> ids = new ArrayList<>();
      if (manifest.has("collections")) {
         for (JsonElement el : manifest.getAsJsonArray("collections")) {
            JsonObject col = el.getAsJsonObject();
            if (col.has("id")) {
               ids.add(col.get("id").getAsString());
            }
         }
      }

      return ids;
   }

   private static void loadCachedCollections(Set<String> enabledIds) {
      if (cacheDir != null) {
         Path collectionsDir = cacheDir.resolve("data/blockpops/collections");
         if (Files.isDirectory(collectionsDir)) {
            try {
               List<FigureCollection> collections = new ArrayList<>();
               File[] jsonFiles = collectionsDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
               if (jsonFiles == null) {
                  return;
               }

               for (File jsonFile : jsonFiles) {
                  String id = jsonFile.getName().replace(".json", "");
                  if (enabledIds.contains(id)) {
                     try {
                        String content = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
                        JsonObject json = (JsonObject)GSON.fromJson(content, JsonObject.class);
                        FigureCollection collection = FigureCollection.fromJson(json);
                        collections.add(collection);
                        BlockPopsMod.LOGGER.info("Loaded remote collection '{}' with {} figures", collection.getName(), collection.getFigures().size());
                     } catch (Exception var12) {
                        BlockPopsMod.LOGGER.error("Failed to load cached collection {}: {}", jsonFile.getName(), var12.getMessage());
                     }
                  }
               }

               class_310.method_1551().execute(() -> {
                  RemoteModelManager.clearRegisteredModels();
                  RemoteAnimationManager.clearRegisteredAnimations();
                  RemoteTextureManager.clearRegisteredTextures();
                  RemoteModelManager.registerCachedModels(cacheDir);
                  RemoteAnimationManager.registerCachedAnimations(cacheDir);
                  RemoteTextureManager.registerCachedTextures(cacheDir);

                  for (FigureCollection collectionx : collections) {
                     CollectionRegistry.registerDynamicCollection(collectionx);
                     BlockPopsMod.LOGGER.info("Registered remote collection: {}", collectionx.getId());
                  }
               });
            } catch (Exception var13) {
               BlockPopsMod.LOGGER.error("Failed to load cached collections: {}", var13.getMessage());
            }
         }
      }
   }

   public static void clearCache() {
      cachedManifest = null;
      if (cacheDir != null && Files.isDirectory(cacheDir)) {
         try {
            Files.walk(cacheDir).sorted(Comparator.reverseOrder()).forEach(path -> {
               try {
                  Files.deleteIfExists(path);
               } catch (IOException var2) {
               }
            });
            Files.createDirectories(cacheDir);
            BlockPopsMod.LOGGER.info("Cleared remote asset cache");
         } catch (Exception var1) {
            BlockPopsMod.LOGGER.error("Failed to clear cache: {}", var1.getMessage());
         }
      }

      RemoteModelManager.clearRegisteredModels();
      RemoteAnimationManager.clearRegisteredAnimations();
      RemoteTextureManager.clearRegisteredTextures();
   }

   public static void invalidateManifest() {
      cachedManifest = null;
      if (cacheDir != null) {
         try {
            Files.deleteIfExists(cacheDir.resolve("manifest.json"));
         } catch (IOException var1) {
         }
      }

      BlockPopsMod.LOGGER.info("Invalidated manifest cache (files preserved)");
   }

   @Nullable
   public static String getLastSyncError() {
      return lastSyncError;
   }

   public static Path getCacheDir() {
      return cacheDir;
   }

   public static boolean isSyncing() {
      return syncing.get();
   }

   public static long getLastSyncTimestamp() {
      return lastSyncTimestamp;
   }

   public static int getLastSyncDownloaded() {
      return lastSyncDownloaded;
   }

   public static int getLastSyncCached() {
      return lastSyncCached;
   }

   public static int getLastSyncFailed() {
      return lastSyncFailed;
   }

   public static int getLastSyncTotalFiles() {
      return lastSyncTotalFiles;
   }

   public static void checkForUpdates(@Nullable Consumer<Boolean> onResult) {
      if (!initialized) {
         if (onResult != null) {
            onResult.accept(null);
         }
      } else if (!checking.compareAndSet(false, true)) {
         if (onResult != null) {
            onResult.accept(updateAvailable);
         }
      } else {
         CompletableFuture.runAsync(() -> {
            try {
               String manifestJson = downloadString("https://f003.backblazeb2.com/file/blockpops-assets/manifest.json");
               if (manifestJson != null) {
                  JsonObject remoteManifest = (JsonObject)GSON.fromJson(manifestJson, JsonObject.class);
                  int remoteVersion = remoteManifest.has("version") ? remoteManifest.get("version").getAsInt() : -1;
                  remoteManifestVersion = remoteVersion;
                  int cachedVersion = -1;
                  if (cachedManifest != null && cachedManifest.has("version")) {
                     cachedVersion = cachedManifest.get("version").getAsInt();
                  } else if (cacheDir != null) {
                     Path manifestFile = cacheDir.resolve("manifest.json");
                     if (Files.exists(manifestFile)) {
                        try {
                           String content = Files.readString(manifestFile, StandardCharsets.UTF_8);
                           JsonObject diskManifest = (JsonObject)GSON.fromJson(content, JsonObject.class);
                           if (diskManifest.has("version")) {
                              cachedVersion = diskManifest.get("version").getAsInt();
                           }
                        } catch (Exception var12) {
                        }
                     }
                  }

                  boolean hasChanges = remoteVersion != cachedVersion;
                  if (!hasChanges && cachedManifest != null) {
                     JsonArray remoteFiles = remoteManifest.getAsJsonArray("files");
                     JsonArray cachedFiles = cachedManifest.getAsJsonArray("files");
                     hasChanges = remoteFiles != null && cachedFiles != null && remoteFiles.size() != cachedFiles.size();
                  }

                  updateAvailable = hasChanges;
                  BlockPopsMod.logDebug("Update check: remote v{}, cached v{}, update={}", remoteVersion, cachedVersion, hasChanges);
                  if (onResult != null) {
                     class_310.method_1551().execute(() -> onResult.accept(updateAvailable));
                  }

                  return;
               }

               updateAvailable = null;
               if (onResult != null) {
                  class_310.method_1551().execute(() -> onResult.accept(null));
               }
            } catch (Exception var13) {
               BlockPopsMod.LOGGER.warn("Failed to check for updates: {}", var13.getMessage());
               updateAvailable = null;
               if (onResult != null) {
                  class_310.method_1551().execute(() -> onResult.accept(null));
               }

               return;
            } finally {
               checking.set(false);
            }
         });
      }
   }

   @Nullable
   public static Boolean isUpdateAvailable() {
      return updateAvailable;
   }

   public static int getRemoteManifestVersion() {
      return remoteManifestVersion;
   }

   public static int getCachedManifestVersion() {
      return cachedManifest != null && cachedManifest.has("version") ? cachedManifest.get("version").getAsInt() : -1;
   }

   public static int countManifestFilesForCollection(String collectionId) {
      JsonObject manifest = cachedManifest;
      if (manifest == null) {
         return 0;
      } else {
         JsonArray files = manifest.getAsJsonArray("files");
         if (files == null) {
            return 0;
         } else {
            int count = 0;
            Set<String> singleId = Set.of(collectionId);

            for (JsonElement el : files) {
               String path = el.getAsJsonObject().get("path").getAsString();
               if (isFileForEnabledCollection(path, singleId)) {
                  count++;
               }
            }

            return count;
         }
      }
   }

   public static int countManifestModelsForCollection(String collectionId) {
      JsonObject manifest = cachedManifest;
      if (manifest == null) {
         return 0;
      } else {
         JsonArray files = manifest.getAsJsonArray("files");
         if (files == null) {
            return 0;
         } else {
            int count = 0;
            Set<String> singleId = Set.of(collectionId);

            for (JsonElement el : files) {
               String path = el.getAsJsonObject().get("path").getAsString();
               if (path.endsWith(".geo.json") && isFileForEnabledCollection(path, singleId)) {
                  count++;
               }
            }

            return count;
         }
      }
   }

   public static int countManifestTexturesForCollection(String collectionId) {
      JsonObject manifest = cachedManifest;
      if (manifest == null) {
         return 0;
      } else {
         JsonArray files = manifest.getAsJsonArray("files");
         if (files == null) {
            return 0;
         } else {
            int count = 0;
            Set<String> singleId = Set.of(collectionId);

            for (JsonElement el : files) {
               String path = el.getAsJsonObject().get("path").getAsString();
               if (path.endsWith(".png") && isFileForEnabledCollection(path, singleId)) {
                  count++;
               }
            }

            return count;
         }
      }
   }

   public static int countManifestAnimationsForCollection(String collectionId) {
      JsonObject manifest = cachedManifest;
      if (manifest == null) {
         return 0;
      } else {
         JsonArray files = manifest.getAsJsonArray("files");
         if (files == null) {
            return 0;
         } else {
            int count = 0;
            Set<String> singleId = Set.of(collectionId);

            for (JsonElement el : files) {
               String path = el.getAsJsonObject().get("path").getAsString();
               if (path.endsWith(".animation.json") && isFileForEnabledCollection(path, singleId)) {
                  count++;
               }
            }

            return count;
         }
      }
   }

   private static String downloadString(String url) throws IOException {
      HttpURLConnection conn = (HttpURLConnection)URI.create(url).toURL().openConnection();
      conn.setConnectTimeout(10000);
      conn.setReadTimeout(30000);
      conn.setRequestProperty("User-Agent", "BlockPops-Mod/1.0");
      if (conn.getResponseCode() != 200) {
         BlockPopsMod.LOGGER.warn("HTTP {} for {}", conn.getResponseCode(), url);
         return null;
      } else {
         String var6;
         try (
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
         ) {
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
               sb.append(line);
            }

            var6 = sb.toString();
         }

         return var6;
      }
   }

   private static boolean downloadFile(String url, Path target) {
      try {
         Files.createDirectories(target.getParent());
         HttpURLConnection conn = (HttpURLConnection)URI.create(url).toURL().openConnection();
         conn.setConnectTimeout(10000);
         conn.setReadTimeout(30000);
         conn.setRequestProperty("User-Agent", "BlockPops-Mod/1.0");
         if (conn.getResponseCode() != 200) {
            BlockPopsMod.LOGGER.warn("HTTP {} downloading {}", conn.getResponseCode(), url);
            return false;
         } else {
            try (InputStream is = conn.getInputStream()) {
               Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
         }
      } catch (Exception var8) {
         BlockPopsMod.LOGGER.warn("Failed to download file {}: {}", url, var8.getMessage());
         return false;
      }
   }

   private static String sha256(Path file) {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] bytes = Files.readAllBytes(file);
         byte[] hash = digest.digest(bytes);
         StringBuilder sb = new StringBuilder();

         for (byte b : hash) {
            sb.append(String.format("%02x", b));
         }

         return sb.toString();
      } catch (Exception var9) {
         return "";
      }
   }

   public static class CodeResult {
      private final String id;
      private final String name;
      private final String error;

      public CodeResult(String id, String name, String error) {
         this.id = id;
         this.name = name;
         this.error = error;
      }

      public CodeResult(String id, String name) {
         this(id, name, null);
      }

      public static RemoteAssetManager.CodeResult error(String error) {
         return new RemoteAssetManager.CodeResult(null, null, error);
      }

      public boolean isError() {
         return this.error != null;
      }

      public boolean isSuccess() {
         return this.id != null && this.error == null;
      }

      public String id() {
         return this.id;
      }

      public String name() {
         return this.name;
      }

      public String error() {
         return this.error;
      }
   }
}
