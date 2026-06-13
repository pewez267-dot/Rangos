package com.theplumteam.client.renderer;

import com.theplumteam.blockentity.FigureBlockEntity;
import com.theplumteam.client.model.FigureBlockModel;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.util.SkinModelDetector;
import java.util.Collections;
import java.util.List;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class FigureBlockRenderer extends GeoBlockRenderer<FigureBlockEntity> {
   private List<String> currentHiddenBones = Collections.emptyList();

   public FigureBlockRenderer() {
      super(new FigureBlockModel());
      this.addRenderLayer(new FigureBoneTextureLayer(this, FigureBlockEntity::getFigureDefinition, FigureBlockEntity::getAlternativeSkinIndex));
   }

   public void preRender(
      class_4587 poseStack,
      FigureBlockEntity animatable,
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
         this.currentHiddenBones = figureDef != null ? figureDef.getHiddenBonesForSkinIndex(animatable.getAlternativeSkinIndex()) : Collections.emptyList();
         if (figureDef != null) {
            for (String boneName : figureDef.getAllVariantBoneNames()) {
               model.getBone(boneName).ifPresent(bone -> {
                  bone.setHidden(false);
                  bone.setChildrenHidden(false);
               });
            }

            for (String boneName : this.currentHiddenBones) {
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
         }
      } else {
         this.currentHiddenBones = Collections.emptyList();
      }
   }

   public void renderRecursively(
      class_4587 poseStack,
      FigureBlockEntity animatable,
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
      if (this.currentHiddenBones.isEmpty() || !this.currentHiddenBones.contains(bone.getName())) {
         super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
      }
   }

   public void actuallyRender(
      class_4587 poseStack,
      FigureBlockEntity animatable,
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
      if (animatable.hasFigure()) {
         float defScale = animatable.getFigureDefinition() != null
            ? animatable.getFigureDefinition().getScaleForSkinIndex(animatable.getAlternativeSkinIndex())
            : 1.0F;
         if (defScale != 1.0F) {
            poseStack.method_22905(defScale, defScale, defScale);
         }

         super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
      }
   }
}
