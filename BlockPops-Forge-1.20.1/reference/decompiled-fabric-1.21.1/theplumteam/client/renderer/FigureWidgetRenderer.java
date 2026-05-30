package com.theplumteam.client.renderer;

import com.theplumteam.block.PopBlockColor;
import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.client.model.FigureModel;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.figure.FigureType;
import com.theplumteam.registry.ModBlocks;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.class_1921;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class FigureWidgetRenderer {
   private static FigureModel figureModel;
   private static GeoBlockRenderer<BoxBlockEntity> figureRenderer;
   private static final Map<String, BoxBlockEntity> renderEntityCache = new ConcurrentHashMap<>();
   private static volatile boolean initialized = false;

   private FigureWidgetRenderer() {
   }

   public static void ensureInitialized() {
      if (!initialized) {
         synchronized (FigureWidgetRenderer.class) {
            if (!initialized) {
               figureModel = new FigureModel();
               figureRenderer = new GeoBlockRenderer<BoxBlockEntity>(figureModel) {
                  protected void rotateBlock(class_2350 facing, class_4587 poseStack) {
                  }

                  public class_1921 getRenderType(BoxBlockEntity animatable, class_2960 texture, class_4597 bufferSource, float partialTick) {
                     return class_1921.method_23580(texture);
                  }

                  public void preRender(
                     class_4587 poseStack,
                     BoxBlockEntity animatable,
                     BakedGeoModel model,
                     class_4597 bufferSource,
                     class_4588 buffer,
                     boolean isReRender,
                     float partialTick,
                     int packedLight,
                     int packedOverlay,
                     int colour
                  ) {
                     super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
                     FigureDefinition figureDef = animatable.getFigureDefinition();
                     if (figureDef != null) {
                        for (String boneName : figureDef.getAllVariantBoneNames()) {
                           model.getBone(boneName).ifPresent(bone -> {
                              bone.setHidden(false);
                              bone.setChildrenHidden(false);
                           });
                        }

                        for (String boneName : figureDef.getHiddenBonesForSkinIndex(animatable.getAlternativeSkinIndex())) {
                           model.getBone(boneName).ifPresent(bone -> {
                              bone.setHidden(true);
                              bone.setChildrenHidden(true);
                           });
                        }

                        boolean usingAltModel = animatable.getAlternativeSkinIndex() > 0
                           && figureDef.getModelForSkinIndex(animatable.getAlternativeSkinIndex()) != null
                           && !figureDef.getModelForSkinIndex(animatable.getAlternativeSkinIndex()).equals(figureDef.getModelPath());
                        if (!usingAltModel) {
                           for (FigureDefinition.ExtraTexture extra : figureDef.getExtraTextures()) {
                              for (String boneName : extra.bones()) {
                                 model.getBone(boneName).ifPresent(bone -> {
                                    bone.setHidden(true);
                                    bone.setChildrenHidden(false);
                                 });
                              }
                           }
                        }

                        float defScale = figureDef.getScaleForSkinIndex(animatable.getAlternativeSkinIndex());
                        if (defScale != 1.0F) {
                           poseStack.method_22905(defScale, defScale, defScale);
                        }
                     }
                  }

                  public void renderRecursively(
                     class_4587 poseStack,
                     BoxBlockEntity animatable,
                     GeoBone bone,
                     class_1921 renderType,
                     class_4597 bufferSource,
                     class_4588 buffer,
                     boolean isReRender,
                     float partialTick,
                     int packedLight,
                     int packedOverlay,
                     int colour
                  ) {
                     FigureDefinition fd = animatable.getFigureDefinition();
                     if (fd != null) {
                        List<String> hiddenBones = fd.getHiddenBonesForSkinIndex(animatable.getAlternativeSkinIndex());
                        if (!hiddenBones.isEmpty() && hiddenBones.contains(bone.getName())) {
                           return;
                        }
                     }

                     super.renderRecursively(
                        poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour
                     );
                  }
               };
               figureRenderer.addRenderLayer(
                  new FigureBoneTextureLayer(figureRenderer, BoxBlockEntity::getFigureDefinition, BoxBlockEntity::getAlternativeSkinIndex)
               );
               initialized = true;
            }
         }
      }
   }

   public static FigureModel getModel() {
      ensureInitialized();
      return figureModel;
   }

   public static GeoBlockRenderer<BoxBlockEntity> getRenderer() {
      ensureInitialized();
      return figureRenderer;
   }

   public static BoxBlockEntity getOrCreateRenderEntity(FigureDefinition figure, String collectionId) {
      if (figure != null && collectionId != null) {
         String cacheKey = collectionId + ":" + figure.getId();
         return renderEntityCache.computeIfAbsent(cacheKey, key -> {
            try {
               BoxBlockEntity entity = new BoxBlockEntity(class_2338.field_10980, ((class_2248)ModBlocks.BOX_BLOCK.get()).method_9564());
               entity.method_31662(class_310.method_1551().field_1687);
               entity.setFigureId(figure.getId());
               entity.setCollectionIdOverride(collectionId);
               if (figure.getType() == FigureType.PLAYER) {
                  PopBlockColor favoriteColor = figure.getFavoriteColor();
                  if (favoriteColor != null) {
                     entity.setColorOverride(favoriteColor.name());
                  }
               }

               return entity;
            } catch (Exception var5) {
               return null;
            }
         });
      } else {
         return null;
      }
   }

   public static void clearCache() {
      renderEntityCache.clear();
   }

   public static int getCacheSize() {
      return renderEntityCache.size();
   }
}
