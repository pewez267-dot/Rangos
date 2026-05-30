package com.theplumteam.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.theplumteam.BlockPopsMod;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import org.lwjgl.opengl.GL11;

public class StarPatternCache {
   private static final class_2960 STAR_PATTERN_CACHE = class_2960.method_60655("blockpops", "textures/gui/background/star_pattern_cache_generated.png");
   private static final int TILE_SIZE = 55;
   private static final int CACHE_TILES_WIDTH = 64;
   private static final int CACHE_TILES_HEIGHT = 32;
   private static class_1043 cachedTexture = null;
   private static class_2960 cachedTextureLocation = null;
   private static int cachedTextureWidth = 0;
   private static int cachedTextureHeight = 0;

   public static void initialize() {
      if (cachedTexture == null) {
         try {
            class_310 mc = class_310.method_1551();
            class_3298 resource = (class_3298)mc.method_1478().method_14486(STAR_PATTERN_CACHE).orElseThrow();

            class_1011 cachedImage;
            try (InputStream stream = resource.method_14482()) {
               cachedImage = class_1011.method_4309(stream);
            }

            cachedTextureWidth = cachedImage.method_4307();
            cachedTextureHeight = cachedImage.method_4323();
            cachedTexture = new class_1043(cachedImage);
            cachedTextureLocation = mc.method_1531().method_4617("blockpops_star_cache", cachedTexture);
            GlStateManager._bindTexture(cachedTexture.method_4624());
            GL11.glTexParameteri(3553, 10241, 9729);
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10242, 10497);
            GL11.glTexParameteri(3553, 10243, 10497);
            BlockPopsMod.logDebug("Star pattern cache loaded: {}x{} (pre-generated texture with linear filtering)", cachedTextureWidth, cachedTextureHeight);
         } catch (IOException var8) {
            BlockPopsMod.LOGGER.error("Failed to load star pattern cache", var8);
         }
      }
   }

   public static class_2960 getTextureLocation() {
      if (cachedTextureLocation == null) {
         initialize();
      }

      return cachedTextureLocation;
   }

   public static int getTextureWidth() {
      if (cachedTexture == null) {
         initialize();
      }

      return cachedTextureWidth;
   }

   public static int getTextureHeight() {
      if (cachedTexture == null) {
         initialize();
      }

      return cachedTextureHeight;
   }

   public static int getTileSize() {
      return 55;
   }

   public static void cleanup() {
      if (cachedTexture != null) {
         cachedTexture.close();
         cachedTexture = null;
         cachedTextureLocation = null;
      }
   }
}
