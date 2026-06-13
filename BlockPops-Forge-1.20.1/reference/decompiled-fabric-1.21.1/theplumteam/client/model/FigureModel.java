package com.theplumteam.client.model;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.client.discovery.ClientDiscoveryManager;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.figure.FigureType;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_640;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.model.GeoModel;

public class FigureModel extends GeoModel<BoxBlockEntity> {
   private static final class_2960 FALLBACK_MODEL = class_2960.method_60655("blockpops", "geo/block/box_block.geo.json");
   private static final class_2960 FALLBACK_TEXTURE = class_2960.method_60655("minecraft", "textures/entity/player/wide/steve.png");
   private static final class_2960 POSE_ANIMATION = class_2960.method_60655("blockpops", "animations/figure/figure_poses.animation.json");
   private static boolean checkedQuickSkin = false;
   private static boolean quickSkinAvailable = false;
   private static Method getSkinLocationMethod;
   private static Object skinServiceInstance;

   private static class_2960 resolveQuickSkinId(String skinId) {
      if (!checkedQuickSkin) {
         try {
            Class<?> serviceClass = Class.forName("com.quickskin.mod.client.services.SkinService");
            Method getInstanceMethod = serviceClass.getMethod("getInstance");
            skinServiceInstance = getInstanceMethod.invoke(null);
            getSkinLocationMethod = serviceClass.getMethod("getSkinLocation", UUID.class, String.class);
            quickSkinAvailable = true;
         } catch (Exception var3) {
            quickSkinAvailable = false;
         }

         checkedQuickSkin = true;
      }

      if (quickSkinAvailable && skinServiceInstance != null && skinId != null) {
         try {
            return (class_2960)getSkinLocationMethod.invoke(skinServiceInstance, null, skinId);
         } catch (Exception var4) {
         }
      }

      return null;
   }

   private static class_2960 getLiveQuickSkin(UUID uuid) {
      try {
         Class<?> serviceClass = Class.forName("com.quickskin.mod.client.services.PlayerAppearanceService");
         Method getInstanceMethod = serviceClass.getMethod("getInstance");
         Object instance = getInstanceMethod.invoke(null);
         Method getLocMethod = serviceClass.getMethod("getSkinLocation", UUID.class);
         return (class_2960)getLocMethod.invoke(instance, uuid);
      } catch (Exception var5) {
         return null;
      }
   }

   public class_2960 getModelResource(BoxBlockEntity animatable) {
      FigureDefinition figure = animatable.getFigureDefinition();
      if (figure != null) {
         int skinIndex = animatable.getAlternativeSkinIndex();
         class_2960 model = figure.getModelForSkinIndex(skinIndex);
         if (model != null && !GeckoLibCache.getBakedModels().containsKey(model)) {
            return FALLBACK_MODEL;
         }

         if (model != null) {
            return model;
         }
      }

      return FALLBACK_MODEL;
   }

   public class_2960 getTextureResource(BoxBlockEntity animatable) {
      FigureDefinition figure = animatable.getFigureDefinition();
      if (figure == null) {
         return FALLBACK_TEXTURE;
      } else {
         int skinIndex = animatable.getAlternativeSkinIndex();
         if (skinIndex > 0 && figure.hasAlternatives()) {
            int altListIndex = skinIndex - 1;
            if (altListIndex < figure.getAlternatives().size()) {
               return figure.getAlternatives().get(altListIndex).texture();
            }
         }

         if (figure.getType() == FigureType.PLAYER && figure.getPlayerUUID() != null) {
            String uniqueFigureId = animatable.getCollectionId() + ":" + animatable.getFigureId();
            String qsId = animatable.getQuickSkinId();
            if (qsId != null && !qsId.isEmpty()) {
               class_2960 loc = resolveQuickSkinId(qsId);
               if (loc != null) {
                  return loc;
               }
            }

            String nbtSnapshot = animatable.getSkinSnapshot();
            if (nbtSnapshot != null && !nbtSnapshot.isEmpty()) {
               return this.getSkinLocationFromSnapshot(figure, nbtSnapshot);
            } else {
               String discoveryQuickSkin = ClientDiscoveryManager.getFigureQuickSkin(uniqueFigureId);
               if (discoveryQuickSkin != null && !discoveryQuickSkin.isEmpty()) {
                  class_2960 loc = resolveQuickSkinId(discoveryQuickSkin);
                  if (loc != null) {
                     return loc;
                  }
               }

               String discoverySnapshot = ClientDiscoveryManager.getFigureSkin(uniqueFigureId);
               if (discoverySnapshot != null && !discoverySnapshot.isEmpty()) {
                  return this.getSkinLocationFromSnapshot(figure, discoverySnapshot);
               } else {
                  if (quickSkinAvailable) {
                     class_2960 liveQS = getLiveQuickSkin(figure.getPlayerUUID());
                     if (liveQS != null) {
                        return liveQS;
                     }
                  }

                  if (class_310.method_1551().method_1562() != null) {
                     class_640 info = class_310.method_1551().method_1562().method_2871(figure.getPlayerUUID());
                     if (info != null) {
                        return info.method_52810().comp_1626();
                     }
                  }

                  return FALLBACK_TEXTURE;
               }
            }
         } else {
            return figure.getTexturePath() != null ? figure.getTexturePath() : FALLBACK_TEXTURE;
         }
      }
   }

   private class_2960 getSkinLocationFromSnapshot(FigureDefinition figure, String snapshot) {
      if (snapshot != null && !snapshot.isEmpty()) {
         try {
            GameProfile profile = new GameProfile(figure.getPlayerUUID(), figure.getName());
            profile.getProperties().put("textures", new Property("textures", snapshot));
            return class_310.method_1551().method_1582().method_52862(profile).comp_1626();
         } catch (Exception var4) {
            return FALLBACK_TEXTURE;
         }
      } else {
         return FALLBACK_TEXTURE;
      }
   }

   public class_2960 getAnimationResource(BoxBlockEntity animatable) {
      FigureDefinition figure = animatable.getFigureDefinition();
      if (figure != null) {
         int skinIndex = animatable.getAlternativeSkinIndex();
         if (skinIndex > 0 && figure.hasAlternatives()) {
            class_2960 altModel = figure.getModelForSkinIndex(skinIndex);
            if (altModel != null && !altModel.equals(figure.getModelPath())) {
               return POSE_ANIMATION;
            }
         }

         if (figure.getPoseAnimationPath() != null) {
            class_2960 anim = figure.getPoseAnimationPath();
            if (!GeckoLibCache.getBakedAnimations().containsKey(anim)) {
               return POSE_ANIMATION;
            }

            return anim;
         }
      }

      return POSE_ANIMATION;
   }

   public class_1921 getRenderType(BoxBlockEntity animatable, class_2960 texture) {
      class_2960 textureToUse = this.getTextureResource(animatable);
      if (textureToUse == null) {
         textureToUse = FALLBACK_TEXTURE;
      }

      return class_1921.method_23580(textureToUse);
   }
}
