package com.theplumteam.client.renderer;

import com.theplumteam.figure.FigureDefinition;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

public class FigureBoneTextureLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
   private final Function<T, FigureDefinition> defGetter;
   private final ToIntFunction<T> skinIndexGetter;

   public FigureBoneTextureLayer(GeoRenderer<T> renderer, Function<T, FigureDefinition> defGetter, ToIntFunction<T> skinIndexGetter) {
      super(renderer);
      this.defGetter = defGetter;
      this.skinIndexGetter = skinIndexGetter;
   }

   public void render(
      class_4587 poseStack,
      T animatable,
      BakedGeoModel bakedModel,
      class_1921 renderType,
      class_4597 bufferSource,
      class_4588 buffer,
      float partialTick,
      int packedLight,
      int packedOverlay
   ) {
      FigureDefinition def = this.defGetter.apply(animatable);
      if (def != null && !def.getExtraTextures().isEmpty()) {
         int skinIndex = this.skinIndexGetter.applyAsInt(animatable);
         if (skinIndex > 0) {
            class_2960 altModel = def.getModelForSkinIndex(skinIndex);
            if (altModel != null && !altModel.equals(def.getModelPath())) {
               return;
            }
         }

         for (FigureDefinition.ExtraTexture extra : def.getExtraTextures()) {
            Set<String> extraBoneNames = new HashSet<>(extra.bones());
            class_1921 extraRT = class_1921.method_23578(extra.texture());
            class_4588 extraBuffer = bufferSource.getBuffer(extraRT);

            for (GeoBone topBone : bakedModel.topLevelBones()) {
               this.renderBoneTree(poseStack, topBone, extraBuffer, packedLight, packedOverlay, extraBoneNames);
            }
         }
      }
   }

   private void renderBoneTree(class_4587 poseStack, GeoBone bone, class_4588 buffer, int packedLight, int packedOverlay, Set<String> extraBoneNames) {
      poseStack.method_22903();
      RenderUtil.prepMatrixForBone(poseStack, bone);
      if (extraBoneNames.contains(bone.getName())) {
         boolean wasHidden = bone.isHidden();
         bone.setHidden(false);
         this.renderer.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, -1);
         bone.setHidden(wasHidden);
      }

      for (GeoBone child : bone.getChildBones()) {
         this.renderBoneTree(poseStack, child, buffer, packedLight, packedOverlay, extraBoneNames);
      }

      poseStack.method_22909();
   }
}
