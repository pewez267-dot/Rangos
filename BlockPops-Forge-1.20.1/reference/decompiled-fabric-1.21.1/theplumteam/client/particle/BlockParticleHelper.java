package com.theplumteam.client.particle;

import com.theplumteam.block.PopBlockColor;
import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.blockentity.FigureBlockEntity;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.figure.FigureDefinition;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.class_1058;
import net.minecraft.class_1723;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_727;

public class BlockParticleHelper {
   private static final class_2960 MISSING_CHECK_SPRITE = class_2960.method_60655("blockpops", "_missing_particle_");

   public static boolean spawnBoxDestroyParticles(class_1937 level, class_2338 pos, BoxBlockEntity boxBE) {
      if (level instanceof class_638 clientLevel) {
         PopBlockColor color = boxBE.getColor();
         if (color != null) {
            class_2960 colorTexture = class_2960.method_60655("blockpops", "textures/block/box/" + color.getTextureName().toLowerCase(Locale.ROOT) + ".png");
            class_1058 sprite = getSpriteOrNull(convertTextureToSpriteId(colorTexture));
            if (sprite != null) {
               spawnParticlesWithSprite(clientLevel, pos, sprite);
               return true;
            }
         }

         String collectionId = boxBE.getCollectionId();
         FigureCollection collection = CollectionRegistry.getCollection(collectionId).orElse(null);
         if (collection == null) {
            return false;
         } else {
            class_2960 boxTexture = collection.getBoxTexture();
            if (boxTexture == null) {
               return false;
            } else {
               class_1058 sprite = getSpriteOrNull(convertTextureToSpriteId(boxTexture));
               if (sprite == null) {
                  return false;
               } else {
                  spawnParticlesWithSprite(clientLevel, pos, sprite);
                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   public static boolean spawnFigureDestroyParticles(class_1937 level, class_2338 pos, FigureBlockEntity figureBE) {
      if (level instanceof class_638 clientLevel) {
         FigureDefinition figure = figureBE.getFigureDefinition();
         if (figure != null && figure.getTexturePath() != null) {
            class_1058 sprite = getSpriteOrNull(convertTextureToSpriteId(figure.getTexturePath()));
            if (sprite != null) {
               spawnParticlesWithSprite(clientLevel, pos, sprite);
               return true;
            }
         }

         String collectionId = figureBE.getCollectionId();
         FigureCollection collection = CollectionRegistry.getCollection(collectionId).orElse(null);
         if (collection != null && collection.getBoxTexture() != null) {
            class_1058 sprite = getSpriteOrNull(convertTextureToSpriteId(collection.getBoxTexture()));
            if (sprite != null) {
               spawnParticlesWithSprite(clientLevel, pos, sprite);
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static class_1058 getSpriteOrNull(class_2960 spriteId) {
      Function<class_2960, class_1058> atlas = class_310.method_1551().method_1549(class_1723.field_21668);
      class_1058 sprite = atlas.apply(spriteId);
      class_1058 missing = atlas.apply(MISSING_CHECK_SPRITE);
      return sprite == missing ? null : sprite;
   }

   private static class_2960 convertTextureToSpriteId(class_2960 texturePath) {
      String path = texturePath.method_12832();
      if (path.startsWith("textures/")) {
         path = path.substring("textures/".length());
      }

      if (path.endsWith(".png")) {
         path = path.substring(0, path.length() - ".png".length());
      }

      return class_2960.method_60655(texturePath.method_12836(), path);
   }

   private static void spawnParticlesWithSprite(class_638 level, class_2338 pos, class_1058 sprite) {
      class_2680 dummyState = class_2246.field_10340.method_9564();

      for (int x = 0; x < 4; x++) {
         for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
               double px = (double)pos.method_10263() + ((double)x + 0.5) / 4.0;
               double py = (double)pos.method_10264() + ((double)y + 0.5) / 4.0;
               double pz = (double)pos.method_10260() + ((double)z + 0.5) / 4.0;
               BlockParticleHelper.CustomSpriteParticle particle = new BlockParticleHelper.CustomSpriteParticle(
                  level,
                  px,
                  py,
                  pz,
                  px - (double)pos.method_10263() - 0.5,
                  py - (double)pos.method_10264() - 0.5,
                  pz - (double)pos.method_10260() - 0.5,
                  dummyState,
                  pos
               );
               particle.setCustomSprite(sprite);
               class_310.method_1551().field_1713.method_3058(particle);
            }
         }
      }
   }

   private static class CustomSpriteParticle extends class_727 {
      public CustomSpriteParticle(class_638 level, double x, double y, double z, double xd, double yd, double zd, class_2680 state, class_2338 pos) {
         super(level, x, y, z, xd, yd, zd, state, pos);
      }

      public void setCustomSprite(class_1058 sprite) {
         this.method_18141(sprite);
      }
   }
}
