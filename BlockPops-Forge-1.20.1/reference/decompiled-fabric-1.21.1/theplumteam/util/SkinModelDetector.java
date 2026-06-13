package com.theplumteam.util;

import com.theplumteam.BlockPopsMod;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_1046;
import net.minecraft.class_1060;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_640;
import net.minecraft.class_8685;
import net.minecraft.class_8685.class_7920;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkinModelDetector {
   private static final Logger LOGGER = LoggerFactory.getLogger(SkinModelDetector.class);
   private static final ConcurrentHashMap<class_2960, SkinModelDetector.SkinModel> DETECTION_CACHE = new ConcurrentHashMap<>();

   public static void clearCache() {
      DETECTION_CACHE.clear();
      BlockPopsMod.logDebug("Skin model detection cache cleared");
   }

   public static SkinModelDetector.SkinModel detectSkinModel(class_2960 textureLocation) {
      SkinModelDetector.SkinModel cached = DETECTION_CACHE.get(textureLocation);
      if (cached != null) {
         return cached;
      } else {
         LOGGER.debug("Attempting to detect skin model for texture: {}", textureLocation);

         try {
            if (class_310.method_1551().method_1562() != null) {
               for (class_640 playerInfo : class_310.method_1551().method_1562().method_2880()) {
                  class_8685 skin = playerInfo.method_52810();
                  if (skin.comp_1626().equals(textureLocation)) {
                     SkinModelDetector.SkinModel model = skin.comp_1629() == class_7920.field_41122
                        ? SkinModelDetector.SkinModel.SLIM
                        : SkinModelDetector.SkinModel.CLASSIC;
                     BlockPopsMod.logDebug("Detected {} skin from PlayerInfo for texture: {}", model, textureLocation);
                     DETECTION_CACHE.put(textureLocation, model);
                     return model;
                  }
               }
            }

            class_1060 textureManager = class_310.method_1551().method_1531();
            class_1044 abstractTexture = textureManager.method_4619(textureLocation);
            if (abstractTexture instanceof class_1043 dynamicTexture) {
               class_1011 image = dynamicTexture.method_4525();
               if (image != null) {
                  SkinModelDetector.SkinModel model = detectSkinModel(image);
                  BlockPopsMod.logDebug("Detected {} skin from DynamicTexture for texture: {}", model, textureLocation);
                  DETECTION_CACHE.put(textureLocation, model);
                  return model;
               }
            } else if (abstractTexture instanceof class_1046 httpTexture) {
               try {
                  Field textureImageField = class_1046.class.getDeclaredField("textureImage");
                  textureImageField.setAccessible(true);
                  class_1011 image = (class_1011)textureImageField.get(httpTexture);
                  if (image != null) {
                     SkinModelDetector.SkinModel model = detectSkinModel(image);
                     BlockPopsMod.logDebug("Detected {} skin from HttpTexture for texture: {}", model, textureLocation);
                     DETECTION_CACHE.put(textureLocation, model);
                     return model;
                  }
               } catch (Exception var10) {
                  LOGGER.debug("Could not access HttpTexture image via reflection: {}", var10.getMessage());
               }
            }

            try {
               InputStream inputStream = ((class_3298)class_310.method_1551().method_1478().method_14486(textureLocation).orElseThrow()).method_14482();
               class_1011 image = class_1011.method_4309(inputStream);
               SkinModelDetector.SkinModel model = detectSkinModel(image);
               image.close();
               inputStream.close();
               BlockPopsMod.logDebug("Detected {} skin for texture: {}", model, textureLocation);
               DETECTION_CACHE.put(textureLocation, model);
               return model;
            } catch (Exception var9) {
               LOGGER.warn("Could not load texture from resource manager: {}", var9.getMessage());
               LOGGER.warn("Could not detect skin model, defaulting to CLASSIC");
               DETECTION_CACHE.put(textureLocation, SkinModelDetector.SkinModel.CLASSIC);
               return SkinModelDetector.SkinModel.CLASSIC;
            }
         } catch (Exception var11) {
            LOGGER.error("Failed to detect skin model from texture: {}", textureLocation, var11);
            DETECTION_CACHE.put(textureLocation, SkinModelDetector.SkinModel.CLASSIC);
            return SkinModelDetector.SkinModel.CLASSIC;
         }
      }
   }

   public static SkinModelDetector.SkinModel detectSkinModel(class_1011 image) {
      if (image == null) {
         return SkinModelDetector.SkinModel.CLASSIC;
      } else {
         int width = image.method_4307();
         int height = image.method_4323();
         SkinModelDetector.SkinResolution resolution = SkinModelDetector.SkinResolution.fromDimensions(width, height);
         int scale = resolution != null ? resolution.getScale() : 1;
         if (resolution == SkinModelDetector.SkinResolution.LEGACY) {
            return SkinModelDetector.SkinModel.CLASSIC;
         } else {
            int rightArmX = 54 * scale;
            int rightArmEndX = 56 * scale;
            int rightArmStartY = 20 * scale;
            int rightArmEndY = 32 * scale;
            int leftArmX = 46 * scale;
            int leftArmEndX = 48 * scale;
            int leftArmStartY = 52 * scale;
            int leftArmEndY = 64 * scale;
            int totalPixels = 0;
            int transparentPixels = 0;

            for (int y = rightArmStartY; y < rightArmEndY && y < height; y++) {
               for (int x = rightArmX; x < rightArmEndX && x < width; x++) {
                  totalPixels++;
                  int rgba = image.method_4315(x, y);
                  int alpha = rgba >> 24 & 0xFF;
                  int blue = rgba >> 16 & 0xFF;
                  int green = rgba >> 8 & 0xFF;
                  int red = rgba & 0xFF;
                  if (alpha < 10 || red == 0 && green == 0 && blue == 0) {
                     transparentPixels++;
                  }
               }
            }

            for (int y = leftArmStartY; y < leftArmEndY && y < height; y++) {
               for (int xx = leftArmX; xx < leftArmEndX && xx < width; xx++) {
                  totalPixels++;
                  int rgba = image.method_4315(xx, y);
                  int alpha = rgba >> 24 & 0xFF;
                  int blue = rgba >> 16 & 0xFF;
                  int green = rgba >> 8 & 0xFF;
                  int red = rgba & 0xFF;
                  if (alpha < 10 || red == 0 && green == 0 && blue == 0) {
                     transparentPixels++;
                  }
               }
            }

            if (totalPixels > 0) {
               float transparentRatio = (float)transparentPixels / (float)totalPixels;
               boolean isSlim = transparentRatio > 0.5F;
               LOGGER.debug("Skin model detection: {}% transparent pixels -> {}", (int)(transparentRatio * 100.0F), isSlim ? "slim" : "classic");
               return isSlim ? SkinModelDetector.SkinModel.SLIM : SkinModelDetector.SkinModel.CLASSIC;
            } else {
               return SkinModelDetector.SkinModel.CLASSIC;
            }
         }
      }
   }

   public static enum SkinModel {
      CLASSIC,
      SLIM;
   }

   public static enum SkinResolution {
      LEGACY(64, 32, 1),
      STANDARD(64, 64, 1),
      HD_128(128, 64, 2),
      HD_256(256, 128, 4),
      HD_512(512, 256, 8),
      HD_1024(1024, 512, 16),
      HD_2048(2048, 1024, 32);

      private final int width;
      private final int height;
      private final int scale;

      private SkinResolution(int width, int height, int scale) {
         this.width = width;
         this.height = height;
         this.scale = scale;
      }

      public int getScale() {
         return this.scale;
      }

      public static SkinModelDetector.SkinResolution fromDimensions(int width, int height) {
         for (SkinModelDetector.SkinResolution res : values()) {
            if (res.width == width && res.height == height) {
               return res;
            }
         }

         return null;
      }
   }
}
