package com.theplumteam.client.renderer;

import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.client.model.BoxBlockModel;
import com.theplumteam.client.model.FigureModel;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.util.SkinModelDetector;
import java.util.Collections;
import java.util.List;
import net.minecraft.class_1921;
import net.minecraft.class_2350;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class BoxBlockRenderer extends GeoBlockRenderer<BoxBlockEntity> {
   private final GeoBlockRenderer<BoxBlockEntity> figureRenderer;
   private List<String> figureHiddenBones = Collections.emptyList();
   private boolean renderingFigure = false;

   public BoxBlockRenderer() {
      super(new BoxBlockModel());
      this.figureRenderer = new GeoBlockRenderer<BoxBlockEntity>(new FigureModel()) {
         protected void rotateBlock(class_2350 facing, class_4587 poseStack) {
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
            float defScale = animatable.getFigureDefinition() != null
               ? animatable.getFigureDefinition().getScaleForSkinIndex(animatable.getAlternativeSkinIndex())
               : 1.0F;
            if (defScale != 1.0F) {
               poseStack.method_22905(defScale, defScale, defScale);
            }

            if (animatable.hasFigure()) {
               class_2960 texture = this.getGeoModel().getTextureResource(animatable);
               SkinModelDetector.SkinModel skinModel = SkinModelDetector.detectSkinModel(texture);
               boolean isSlim = skinModel == SkinModelDetector.SkinModel.SLIM;
               GeoBone rightArmSlim = (GeoBone)model.getBone("RightArmSlim").orElse(null);
               GeoBone leftArmSlim = (GeoBone)model.getBone("LeftArmSlim").orElse(null);
               GeoBone rightArmClassic = (GeoBone)model.getBone("RightArmClassic").orElse(null);
               GeoBone leftArmClassic = (GeoBone)model.getBone("LeftArmClassic").orElse(null);
               if (rightArmSlim != null) {
                  rightArmSlim.setHidden(!isSlim);
               }

               if (leftArmSlim != null) {
                  leftArmSlim.setHidden(!isSlim);
               }

               if (rightArmClassic != null) {
                  rightArmClassic.setHidden(isSlim);
               }

               if (leftArmClassic != null) {
                  leftArmClassic.setHidden(isSlim);
               }

               FigureDefinition figureDef = animatable.getFigureDefinition();
               BoxBlockRenderer.this.figureHiddenBones = figureDef != null
                  ? figureDef.getHiddenBonesForSkinIndex(animatable.getAlternativeSkinIndex())
                  : Collections.emptyList();
               if (figureDef != null) {
                  for (String boneName : figureDef.getAllVariantBoneNames()) {
                     model.getBone(boneName).ifPresent(bone -> {
                        bone.setHidden(false);
                        bone.setChildrenHidden(false);
                     });
                  }

                  for (String boneName : BoxBlockRenderer.this.figureHiddenBones) {
                     model.getBone(boneName).ifPresent(bone -> {
                        bone.setHidden(true);
                        bone.setChildrenHidden(true);
                     });
                  }

                  for (FigureDefinition.ExtraTexture extra : figureDef.getExtraTextures()) {
                     for (String boneName : extra.bones()) {
                        model.getBone(boneName).ifPresent(bone -> {
                           bone.setHidden(true);
                           bone.setChildrenHidden(false);
                        });
                     }
                  }
               }
            } else {
               BoxBlockRenderer.this.figureHiddenBones = Collections.emptyList();
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
            if (BoxBlockRenderer.this.figureHiddenBones.isEmpty() || !BoxBlockRenderer.this.figureHiddenBones.contains(bone.getName())) {
               super.renderRecursively(
                  poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour
               );
            }
         }
      };
      this.figureRenderer
         .addRenderLayer(new FigureBoneTextureLayer(this.figureRenderer, BoxBlockEntity::getFigureDefinition, BoxBlockEntity::getAlternativeSkinIndex));
   }

   public void actuallyRender(
      class_4587 poseStack,
      BoxBlockEntity animatable,
      BakedGeoModel model,
      class_1921 renderType,
      class_4597 bufferSource,
      class_4588 buffer,
      boolean isReRender,
      float partialTick,
      int packedLight,
      int packedOverlay,
      int colour
   ) {
      if (!isReRender) {
         FigureDefinition figureDef = animatable.getFigureDefinition();
         int skinIdx = animatable.getAlternativeSkinIndex();
         boolean hideFace = figureDef != null && !figureDef.getShowBoxFaceForSkinIndex(skinIdx);
         model.getBone("figure_face").ifPresent(bone -> {
            bone.setHidden(hideFace);
            bone.setChildrenHidden(hideFace);
         });
         model.getBone("figure_face_3d").ifPresent(bone -> {
            bone.setHidden(hideFace);
            bone.setChildrenHidden(hideFace);
         });
      }

      super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
      if (animatable.hasFigure()) {
         if (!animatable.isFigureExtracted()) {
            poseStack.method_22903();
            poseStack.method_22904(animatable.getFigureOffsetX(), animatable.getFigureOffsetY(), animatable.getFigureOffsetZ());
            poseStack.method_22905((float)animatable.getFigureScale(), (float)animatable.getFigureScale(), (float)animatable.getFigureScale());
            this.figureRenderer.method_3569(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.method_22909();
         }

         this.renderFigureFace(poseStack, animatable, model, bufferSource, partialTick, packedLight, packedOverlay);
      }

      this.renderLogo(poseStack, animatable, model, bufferSource, partialTick, packedLight, packedOverlay);
   }

   private void renderFigureFace(
      class_4587 poseStack, BoxBlockEntity animatable, BakedGeoModel model, class_4597 bufferSource, float partialTick, int packedLight, int packedOverlay
   ) {
      FigureDefinition figure = animatable.getFigureDefinition();
      if (figure != null) {
         int skinIdx = animatable.getAlternativeSkinIndex();
         float[] customUV = figure.getBoxFaceUVForSkinIndex(skinIdx);
         if (customUV != null || figure.getShowBoxFaceForSkinIndex(skinIdx)) {
            class_2960 skinTexture = this.figureRenderer.getGeoModel().getTextureResource(animatable);
            if (skinTexture != null) {
               class_1921 skinRenderType = class_1921.method_23689(skinTexture);
               class_4588 skinBuffer = bufferSource.getBuffer(skinRenderType);
               if (customUV != null) {
                  this.renderFaceBoneWithCustomUV(poseStack, model, skinBuffer, customUV, packedLight, packedOverlay);
               } else {
                  for (GeoBone bone : model.topLevelBones()) {
                     if (bone.getName().equals("figure_face") || bone.getName().equals("figure_face_3d")) {
                        poseStack.method_22903();
                        this.renderRecursively(
                           poseStack, animatable, bone, skinRenderType, bufferSource, skinBuffer, true, partialTick, packedLight, packedOverlay, -1
                        );
                        poseStack.method_22909();
                     }
                  }
               }
            }
         }
      }
   }

   private void renderFaceBoneWithCustomUV(class_4587 poseStack, BakedGeoModel model, class_4588 buffer, float[] customUV, int packedLight, int packedOverlay) {
      GeoBone faceBone = null;

      for (GeoBone bone : model.topLevelBones()) {
         if (bone.getName().equals("figure_face")) {
            faceBone = bone;
            break;
         }
      }

      if (faceBone != null) {
         float newMinU = customUV[0] / customUV[4];
         float newMaxU = (customUV[0] + customUV[2]) / customUV[4];
         float newMinV = customUV[1] / customUV[5];
         float newMaxV = (customUV[1] + customUV[3]) / customUV[5];
         float origMinU = 0.125F;
         float origMaxU = 0.25F;
         float origMinV = 0.125F;
         float origMaxV = 0.25F;
         float origRangeU = origMaxU - origMinU;
         float origRangeV = origMaxV - origMinV;
         poseStack.method_22903();
         RenderUtil.prepMatrixForBone(poseStack, faceBone);

         for (GeoCube cube : faceBone.getCubes()) {
            poseStack.method_22903();
            RenderUtil.translateToPivotPoint(poseStack, cube);
            RenderUtil.rotateMatrixAroundCube(poseStack, cube);
            RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
            Matrix3f normalisedPoseState = poseStack.method_23760().method_23762();
            Matrix4f poseState = new Matrix4f(poseStack.method_23760().method_23761());

            for (GeoQuad quad : cube.quads()) {
               if (quad != null && quad.direction() == class_2350.field_11034) {
                  Vector3f normal = normalisedPoseState.transform(new Vector3f(quad.normal()));
                  RenderUtil.fixInvertedFlatCube(cube, normal);

                  for (GeoVertex vertex : quad.vertices()) {
                     Vector4f pos = poseState.transform(new Vector4f(vertex.position(), 1.0F));
                     float t_u = (vertex.texU() - origMinU) / origRangeU;
                     float t_v = (vertex.texV() - origMinV) / origRangeV;
                     float remappedU = newMinU + t_u * (newMaxU - newMinU);
                     float remappedV = newMinV + t_v * (newMaxV - newMinV);
                     buffer.method_22912(pos.x(), pos.y(), pos.z())
                        .method_1336(255, 255, 255, 255)
                        .method_22913(remappedU, remappedV)
                        .method_22922(packedOverlay)
                        .method_60803(packedLight)
                        .method_22914(normal.x(), normal.y(), normal.z());
                  }
               }
            }

            poseStack.method_22909();
         }

         poseStack.method_22909();
      }
   }

   private void renderLogo(
      class_4587 poseStack, BoxBlockEntity animatable, BakedGeoModel model, class_4597 bufferSource, float partialTick, int packedLight, int packedOverlay
   ) {
      if (!animatable.isHideLogo()) {
         String collectionId = animatable.getCollectionId();
         FigureCollection collection = CollectionRegistry.getCollection(collectionId).orElse(null);
         if (collection != null) {
            FigureCollection.LogoConfig collectionLogoConfig = collection.getLogoConfig();
            if (collectionLogoConfig != null) {
               float logoPositionX = animatable.getLogoPositionX() != null ? animatable.getLogoPositionX().floatValue() : collectionLogoConfig.getPositionX();
               float logoPositionY = animatable.getLogoPositionY() != null ? animatable.getLogoPositionY().floatValue() : collectionLogoConfig.getPositionY();
               float logoPositionZ = animatable.getLogoPositionZ() != null ? animatable.getLogoPositionZ().floatValue() : collectionLogoConfig.getPositionZ();
               float logoScaleX = animatable.getLogoScaleX() != null ? animatable.getLogoScaleX().floatValue() : collectionLogoConfig.getScaleX();
               float logoScaleY = animatable.getLogoScaleY() != null ? animatable.getLogoScaleY().floatValue() : collectionLogoConfig.getScaleY();
               float logoScaleZ = animatable.getLogoScaleZ() != null ? animatable.getLogoScaleZ().floatValue() : collectionLogoConfig.getScaleZ();
               class_1921 logoRenderType = class_1921.method_23578(collectionLogoConfig.getTexture());
               class_4588 logoBuffer = bufferSource.getBuffer(logoRenderType);

               for (GeoBone bone : model.topLevelBones()) {
                  if (bone.getName().equals("logo")) {
                     poseStack.method_22903();
                     poseStack.method_46416(logoPositionX, logoPositionY, logoPositionZ);
                     poseStack.method_22905(logoScaleX, logoScaleY, logoScaleZ);
                     this.renderRecursively(
                        poseStack, animatable, bone, logoRenderType, bufferSource, logoBuffer, true, partialTick, packedLight, packedOverlay, -1
                     );
                     poseStack.method_22909();
                     break;
                  }
               }
            }
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
      String boneName = bone.getName();
      if (boneName.equals("figure_face") || boneName.equals("figure_face_3d")) {
         if (!isReRender) {
            return;
         }

         FigureDefinition fd = animatable.getFigureDefinition();
         if (fd != null && !fd.getShowBoxFaceForSkinIndex(animatable.getAlternativeSkinIndex())) {
            return;
         }
      }

      if (!boneName.equals("logo") || isReRender) {
         super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
      }
   }
}
