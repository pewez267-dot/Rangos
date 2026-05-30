package com.theplumteam.client.remote;

import com.theplumteam.BlockPopsMod;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_2960;
import net.minecraft.class_310;

public class RemoteTextureManager {
   private static final Set<class_2960> registeredTextures = new HashSet<>();

   public static void registerCachedTextures(Path cacheDir) {
      Path assetsDir = cacheDir.resolve("assets");
      if (Files.isDirectory(assetsDir)) {
         try {
            Files.walk(assetsDir).filter(p -> p.toString().endsWith(".png")).forEach(pngFile -> {
               try {
                  Path relative = assetsDir.resolve("").relativize(pngFile);
                  String relStr = relative.toString().replace('\\', '/');
                  int firstSlash = relStr.indexOf(47);
                  if (firstSlash < 0) {
                     return;
                  }

                  String namespace = relStr.substring(0, firstSlash);
                  String path = relStr.substring(firstSlash + 1);
                  class_2960 location = class_2960.method_60655(namespace, path);
                  if (registeredTextures.contains(location)) {
                     return;
                  }

                  try (InputStream is = Files.newInputStream(pngFile)) {
                     class_1011 image = class_1011.method_4309(is);
                     class_1043 texture = new class_1043(image);
                     class_310.method_1551().method_1531().method_4616(location, texture);
                     registeredTextures.add(location);
                     BlockPopsMod.logDebug("Registered remote texture: {}", location);
                  }
               } catch (Exception var13) {
                  BlockPopsMod.LOGGER.warn("Failed to register texture {}: {}", pngFile, var13.getMessage());
               }
            });
            BlockPopsMod.LOGGER.info("Registered {} remote textures", registeredTextures.size());
         } catch (Exception var3) {
            BlockPopsMod.LOGGER.error("Failed to scan cache for textures: {}", var3.getMessage());
         }
      }
   }

   public static boolean isRemoteTexture(class_2960 location) {
      return registeredTextures.contains(location);
   }

   public static void clearRegisteredTextures() {
      for (class_2960 loc : registeredTextures) {
         try {
            class_310.method_1551().method_1531().method_4615(loc);
         } catch (Exception var3) {
         }
      }

      registeredTextures.clear();
   }
}
