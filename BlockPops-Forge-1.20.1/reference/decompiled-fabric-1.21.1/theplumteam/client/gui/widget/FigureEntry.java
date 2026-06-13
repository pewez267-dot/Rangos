package com.theplumteam.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.client.discovery.ClientDiscoveryManager;
import com.theplumteam.client.gui.SettingsScreen;
import com.theplumteam.client.gui.util.GuiScaleManager;
import com.theplumteam.client.model.FigureModel;
import com.theplumteam.client.renderer.FigureWidgetRenderer;
import com.theplumteam.figure.FigureDefinition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.class_1921;
import net.minecraft.class_2558;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_2960;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4608;
import net.minecraft.class_7833;
import net.minecraft.class_2558.class_2559;
import net.minecraft.class_4280.class_4281;
import net.minecraft.class_4597.class_4598;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class FigureEntry extends class_4281<FigureEntry> {
   private final class_310 mc;
   private final List<FigureDefinition> figures;
   private final String collectionId;
   private float modelScale = 1.0F;
   private float xRotation = 0.0F;
   private float yRotation = 70.0F;
   private float zRotation = 0.0F;
   private float xOffset = -60.0F;
   private float yOffset = 15.0F;
   private float zOffset = 0.0F;
   private static final int FIGURE_SIZE = 80;
   private static final int GRID_SPACING = 4;
   private static final int LINK_BUTTON_SIZE = 12;
   private static final class_2960 LINK_ICON = class_2960.method_60655("blockpops", "textures/gui/search_icon.png");
   private int hoveredLinkFigureIndex = -1;

   public FigureEntry(List<FigureDefinition> figures, String collectionId) {
      this.mc = class_310.method_1551();
      this.figures = new ArrayList<>(figures);
      this.collectionId = collectionId;
   }

   public void setConfiguration(float modelScale, float xRotation, float yRotation, float zRotation, float xOffset, float yOffset, float zOffset) {
      this.modelScale = modelScale;
      this.xRotation = xRotation;
      this.yRotation = yRotation;
      this.zRotation = zRotation;
      this.xOffset = xOffset;
      this.yOffset = yOffset;
      this.zOffset = zOffset;
   }

   public void method_25343(
      class_332 graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isMouseOver, float partialTick
   ) {
      this.hoveredLinkFigureIndex = -1;
      int effectiveFigureSize = 80;
      int effectiveSpacing = 4;
      if (GuiScaleManager.isUsingInverseScale()) {
         float scale = GuiScaleManager.getRenderScaleFactor();
         effectiveFigureSize = (int)(80.0F * scale);
         effectiveSpacing = (int)(4.0F * scale);
      }

      int totalFiguresWidth = this.figures.size() * effectiveFigureSize + (this.figures.size() - 1) * effectiveSpacing;
      int startX = x + (entryWidth - totalFiguresWidth) / 2;

      for (int i = 0; i < this.figures.size(); i++) {
         FigureDefinition figure = this.figures.get(i);
         int figureX = startX + i * (effectiveFigureSize + effectiveSpacing);
         String uniqueFigureId = this.collectionId + ":" + figure.getId();
         boolean isDiscovered = ClientDiscoveryManager.isDiscovered(uniqueFigureId);
         if (isDiscovered) {
            graphics.method_25294(figureX, y, figureX + effectiveFigureSize, y + effectiveFigureSize, 822083583);
         } else {
            graphics.method_25294(figureX, y, figureX + effectiveFigureSize, y + effectiveFigureSize, 1342177280);
         }

         boolean isFigureHovered = mouseX >= figureX && mouseX < figureX + effectiveFigureSize && mouseY >= y && mouseY < y + effectiveFigureSize;
         if (isFigureHovered) {
            if (isDiscovered) {
               graphics.method_25294(figureX, y, figureX + effectiveFigureSize, y + effectiveFigureSize, 1090519039);
            } else {
               graphics.method_25294(figureX, y, figureX + effectiveFigureSize, y + effectiveFigureSize, 1610612736);
            }
         }

         int borderColor = isDiscovered ? -2130706433 : 1619034240;
         graphics.method_25294(figureX, y, figureX + effectiveFigureSize, y + 1, borderColor);
         graphics.method_25294(figureX, y + effectiveFigureSize - 1, figureX + effectiveFigureSize, y + effectiveFigureSize, borderColor);
         graphics.method_25294(figureX, y, figureX + 1, y + effectiveFigureSize, borderColor);
         graphics.method_25294(figureX + effectiveFigureSize - 1, y, figureX + effectiveFigureSize, y + effectiveFigureSize, borderColor);
         if (!isDiscovered) {
            class_437 currentScreen = class_310.method_1551().field_1755;
            boolean isSettingsModalActive = currentScreen instanceof SettingsScreen;
            if (!isSettingsModalActive) {
               class_2561 questionMark = class_2561.method_43470("?");
               int qmWidth = this.mc.field_1772.method_27525(questionMark);
               int qmX = figureX + (effectiveFigureSize - qmWidth) / 2;
               int qmY = y + (effectiveFigureSize - 9) / 2;
               graphics.method_51439(this.mc.field_1772, questionMark, qmX, qmY, 8421504, false);
            }
         } else {
            this.render3DFigure(graphics, figure, figureX, y, effectiveFigureSize, partialTick);
            class_437 currentScreen = class_310.method_1551().field_1755;
            boolean isSettingsModalActive = currentScreen instanceof SettingsScreen;
            int guiScale = (Integer)this.mc.field_1690.method_42474().method_41753();
            if (guiScale < 3 && !isSettingsModalActive) {
               class_2561 figureName = class_2561.method_43470(figure.getName());
               int nameWidth = this.mc.field_1772.method_27525(figureName);
               if (nameWidth > effectiveFigureSize - 4) {
                  String truncated = figure.getName();

                  while (this.mc.field_1772.method_1727(truncated + "...") > effectiveFigureSize - 4 && truncated.length() > 0) {
                     truncated = truncated.substring(0, truncated.length() - 1);
                  }

                  figureName = class_2561.method_43470(truncated + "...");
               }

               int nameX = figureX + (effectiveFigureSize - this.mc.field_1772.method_27525(figureName)) / 2;
               int nameY = y + effectiveFigureSize - 9 - 2;
               graphics.method_51439(this.mc.field_1772, figureName, nameX, nameY, 16777215, true);
            }

            if (isFigureHovered && figure.hasAuthorUrl()) {
               int effectiveLinkSize = 12;
               if (GuiScaleManager.isUsingInverseScale()) {
                  effectiveLinkSize = (int)(12.0F * GuiScaleManager.getRenderScaleFactor());
               }

               int linkX = figureX + effectiveFigureSize - effectiveLinkSize - 2;
               int linkY = y + 2;
               boolean linkHovered = mouseX >= linkX && mouseX < linkX + effectiveLinkSize && mouseY >= linkY && mouseY < linkY + effectiveLinkSize;
               graphics.method_25294(linkX, linkY, linkX + effectiveLinkSize, linkY + effectiveLinkSize, linkHovered ? -4684277 : -1063686144);
               graphics.method_49601(linkX, linkY, effectiveLinkSize, effectiveLinkSize, linkHovered ? -2448096 : -4684277);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               int iconSize = effectiveLinkSize - 4;
               int iconX = linkX + 2;
               int iconY = linkY + 2;
               graphics.method_51448().method_22903();
               graphics.method_51448().method_46416((float)iconX, (float)iconY, 0.0F);
               float iconScale = (float)iconSize / 256.0F;
               graphics.method_51448().method_22905(iconScale, iconScale, 1.0F);
               graphics.method_25290(LINK_ICON, 0, 0, 0.0F, 0.0F, 256, 256, 256, 256);
               graphics.method_51448().method_22909();
               RenderSystem.disableBlend();
               if (linkHovered) {
                  this.hoveredLinkFigureIndex = i;
               }
            }
         }
      }
   }

   private void render3DFigure(class_332 graphics, FigureDefinition figure, int x, int y, int size, float partialTick) {
      BlockPopsMod.logDebug("render3DFigure called for figure: {} in collection: {}", figure.getId(), this.collectionId);
      class_437 currentScreen = class_310.method_1551().field_1755;
      if (!(currentScreen instanceof SettingsScreen)) {
         class_4587 poseStack = graphics.method_51448();
         poseStack.method_22903();
         BoxBlockEntity renderEntity = FigureWidgetRenderer.getOrCreateRenderEntity(figure, this.collectionId);
         if (renderEntity == null) {
            BlockPopsMod.LOGGER.warn("renderEntity is null for figure: {} in collection: {}", figure.getId(), this.collectionId);
            poseStack.method_22909();
         } else {
            graphics.method_44379(x, y, x + size, y + size);
            RenderSystem.disableDepthTest();
            float centerX = (float)x + (float)size / 2.0F + this.xOffset;
            float centerY = (float)y + (float)size * 0.6F + this.yOffset;
            float baseZ = 100.0F + this.zOffset;
            float centerZ = baseZ * ((float)size / 80.0F);
            class_308.method_24210();
            poseStack.method_46416(centerX, centerY, centerZ);
            float scale = (float)size * this.modelScale * figure.getGuiScale();
            poseStack.method_22905(scale, -scale, scale);
            poseStack.method_22907(class_7833.field_40716.rotationDegrees(this.yRotation));
            poseStack.method_22907(class_7833.field_40714.rotationDegrees(this.xRotation));
            poseStack.method_22907(class_7833.field_40718.rotationDegrees(this.zRotation));
            class_4598 bufferSource = this.mc.method_22940().method_23000();
            FigureModel figureModel = FigureWidgetRenderer.getModel();
            GeoBlockRenderer<BoxBlockEntity> figureRenderer = FigureWidgetRenderer.getRenderer();

            try {
               class_2960 modelResource = figureModel.getModelResource(renderEntity);
               if (modelResource == null) {
                  BlockPopsMod.LOGGER.warn("FigureEntry: modelResource is null");
                  RenderSystem.enableDepthTest();
                  graphics.method_44380();
                  poseStack.method_22909();
                  return;
               }

               BakedGeoModel bakedModel = figureModel.getBakedModel(modelResource);
               class_2960 textureResource = figureModel.getTextureResource(renderEntity);
               if (textureResource == null) {
                  RenderSystem.enableDepthTest();
                  graphics.method_44380();
                  poseStack.method_22909();
                  return;
               }

               FigureDefinition figureDef = renderEntity.getFigureDefinition();
               if (figureDef != null) {
                  for (String boneName : figureDef.getAllVariantBoneNames()) {
                     bakedModel.getBone(boneName).ifPresent(bone -> {
                        bone.setHidden(false);
                        bone.setChildrenHidden(false);
                     });
                  }

                  for (String boneName : figureDef.getHiddenBonesForSkinIndex(renderEntity.getAlternativeSkinIndex())) {
                     bakedModel.getBone(boneName).ifPresent(bone -> {
                        bone.setHidden(true);
                        bone.setChildrenHidden(true);
                     });
                  }

                  boolean usingAltModel = renderEntity.getAlternativeSkinIndex() > 0
                     && figureDef.getModelForSkinIndex(renderEntity.getAlternativeSkinIndex()) != null
                     && !figureDef.getModelForSkinIndex(renderEntity.getAlternativeSkinIndex()).equals(figureDef.getModelPath());
                  if (!usingAltModel) {
                     for (FigureDefinition.ExtraTexture extra : figureDef.getExtraTextures()) {
                        for (String boneName : extra.bones()) {
                           bakedModel.getBone(boneName).ifPresent(bone -> {
                              bone.setHidden(true);
                              bone.setChildrenHidden(false);
                           });
                        }
                     }
                  }
               }

               class_1921 renderType = figureModel.getRenderType(renderEntity, textureResource);
               class_4588 buffer = bufferSource.getBuffer(renderType);
               figureRenderer.actuallyRender(
                  poseStack, renderEntity, bakedModel, renderType, bufferSource, buffer, false, partialTick, 15728880, class_4608.field_21444, -1
               );
               if (figureDef != null && !figureDef.getExtraTextures().isEmpty()) {
                  boolean usingAltModel = renderEntity.getAlternativeSkinIndex() > 0
                     && figureDef.getModelForSkinIndex(renderEntity.getAlternativeSkinIndex()) != null
                     && !figureDef.getModelForSkinIndex(renderEntity.getAlternativeSkinIndex()).equals(figureDef.getModelPath());
                  if (!usingAltModel) {
                     for (FigureDefinition.ExtraTexture extra : figureDef.getExtraTextures()) {
                        Set<String> extraBoneNames = new HashSet<>(extra.bones());
                        class_1921 extraRT = class_1921.method_23578(extra.texture());
                        class_4588 extraBuffer = bufferSource.getBuffer(extraRT);

                        for (GeoBone topBone : bakedModel.topLevelBones()) {
                           renderExtraBoneTree(poseStack, topBone, extraBuffer, figureRenderer, 15728880, class_4608.field_21444, extraBoneNames);
                        }
                     }
                  }
               }

               bufferSource.method_22993();
            } catch (Exception var32) {
               BlockPopsMod.LOGGER.error("FigureEntry: Exception rendering figure: {}", var32.getMessage());
            }

            RenderSystem.enableDepthTest();
            graphics.method_44380();
            class_308.method_24211();
            poseStack.method_22909();
         }
      }
   }

   private static void renderExtraBoneTree(
      class_4587 poseStack,
      GeoBone bone,
      class_4588 buffer,
      GeoBlockRenderer<BoxBlockEntity> renderer,
      int packedLight,
      int packedOverlay,
      Set<String> extraBoneNames
   ) {
      poseStack.method_22903();
      RenderUtil.prepMatrixForBone(poseStack, bone);
      if (extraBoneNames.contains(bone.getName())) {
         boolean wasHidden = bone.isHidden();
         bone.setHidden(false);
         renderer.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, -1);
         bone.setHidden(wasHidden);
      }

      for (GeoBone child : bone.getChildBones()) {
         renderExtraBoneTree(poseStack, child, buffer, renderer, packedLight, packedOverlay, extraBoneNames);
      }

      poseStack.method_22909();
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (button == 0 && this.hoveredLinkFigureIndex >= 0 && this.hoveredLinkFigureIndex < this.figures.size()) {
         FigureDefinition figure = this.figures.get(this.hoveredLinkFigureIndex);
         if (figure.hasAuthorUrl()) {
            try {
               this.mc.field_1755.method_25430(class_2583.field_24360.method_10958(new class_2558(class_2559.field_11749, figure.getAuthorUrl())));
            } catch (Exception var8) {
               System.err.println("Failed to open URL: " + figure.getAuthorUrl());
            }

            return true;
         }
      }

      return false;
   }

   public class_2561 method_37006() {
      if (this.figures.isEmpty()) {
         return class_2561.method_43470("Empty row");
      } else if (this.figures.size() == 1) {
         FigureDefinition figure = this.figures.get(0);
         String uniqueFigureId = this.collectionId + ":" + figure.getId();
         boolean isDiscovered = ClientDiscoveryManager.isDiscovered(uniqueFigureId);
         return class_2561.method_43470(isDiscovered ? figure.getName() : "Undiscovered Figure");
      } else {
         int discoveredCount = 0;

         for (FigureDefinition figure : this.figures) {
            String uniqueFigureId = this.collectionId + ":" + figure.getId();
            if (ClientDiscoveryManager.isDiscovered(uniqueFigureId)) {
               discoveredCount++;
            }
         }

         return class_2561.method_43470(discoveredCount + " of " + this.figures.size() + " figures discovered");
      }
   }

   public List<FigureDefinition> getFigures() {
      return this.figures;
   }
}
