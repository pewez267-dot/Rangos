package com.theplumteam.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ServerCollectionLoader {
   private static final Gson GSON = new Gson();
   private static final String CDN_BASE_URL = "https://f003.backblazeb2.com/file/blockpops-assets";
   private static final int CONNECT_TIMEOUT = 10000;
   private static final int READ_TIMEOUT = 30000;

   public static void loadCollections(Set<String> enabledIds) {
      if (!enabledIds.isEmpty()) {
         CompletableFuture.runAsync(
            () -> {
               BlockPopsMod.LOGGER.info("[Server] Loading {} remote collection(s)...", enabledIds.size());

               for (String id : enabledIds) {
                  try {
                     String url = "https://f003.backblazeb2.com/file/blockpops-assets/data/blockpops/collections/" + id + ".json";
                     String json = downloadString(url);
                     if (json == null) {
                        BlockPopsMod.LOGGER.warn("[Server] Failed to download collection '{}' from CDN", id);
                     } else {
                        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
                        FigureCollection collection = FigureCollection.fromJson(jsonObject);
                        CollectionRegistry.registerDynamicCollection(collection);
                        BlockPopsMod.LOGGER
                           .info("[Server] Registered remote collection '{}' with {} figures", collection.getName(), collection.getFigures().size());
                     }
                  } catch (Exception var7) {
                     BlockPopsMod.LOGGER.error("[Server] Failed to load collection '{}': {}", id, var7.getMessage());
                  }
               }
            }
         );
      }
   }

   private static String downloadString(String url) {
      try {
         HttpURLConnection conn = (HttpURLConnection)URI.create(url).toURL().openConnection();
         conn.setConnectTimeout(10000);
         conn.setReadTimeout(30000);
         conn.setRequestProperty("User-Agent", "BlockPops-Server/1.0");
         if (conn.getResponseCode() != 200) {
            BlockPopsMod.LOGGER.warn("[Server] HTTP {} for {}", conn.getResponseCode(), url);
            return null;
         } else {
            String result;
            try (
               InputStream is = conn.getInputStream();
               BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            ) {
               StringBuilder sb = new StringBuilder();

               String line;
               while ((line = reader.readLine()) != null) {
                  sb.append(line);
               }

               result = sb.toString();
            }

            return result;
         }
      } catch (Exception var11) {
         BlockPopsMod.LOGGER.warn("[Server] Failed to download {}: {}", url, var11.getMessage());
         return null;
      }
   }
}
