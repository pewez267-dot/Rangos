package com.theplumteam.client.renderer;

import com.theplumteam.block.BoxBlock;
import com.theplumteam.block.PopBlockColor;
import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.item.BoxBlockItem;
import com.theplumteam.item.GeoBlockItem;
import net.minecraft.class_1799;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_756;
import net.minecraft.class_7833;
import net.minecraft.class_811;
import net.minecraft.class_9279;
import net.minecraft.class_9334;

public class BoxBlockItemRenderer extends class_756 {
   private final BoxBlockRenderer renderer = new BoxBlockRenderer();
   private BoxBlockEntity renderEntity;

   public BoxBlockItemRenderer() {
      super(class_310.method_1551().method_31975(), class_310.method_1551().method_31974());
   }

   public void method_3166(class_1799 stack, class_811 displayContext, class_4587 poseStack, class_4597 bufferSource, int packedLight, int packedOverlay) {
      if (stack.method_7909() instanceof GeoBlockItem geoBlockItem) {
         BoxBlock boxBlock = geoBlockItem.getBoxBlock();
         this.renderEntity = new BoxBlockEntity(class_2338.field_10980, boxBlock.method_9564());
         this.renderEntity.method_31662(class_310.method_1551().field_1687);
         if (stack.method_7909() instanceof BoxBlockItem boxBlockItem) {
            PopBlockColor color = boxBlockItem.getColor();
            if (color != null) {
               this.renderEntity.setColorOverride(color.method_15434());
            }

            String collectionId = boxBlockItem.getCollectionId();
            if (collectionId != null) {
               this.renderEntity.setCollectionIdOverride(collectionId);
            }
         }

         class_9279 customData = (class_9279)stack.method_57824(class_9334.field_49611);
         if (customData != null) {
            class_2487 blockEntityTag = customData.method_57461();
            this.renderEntity.loadFromItemNbt(blockEntityTag);
            if (blockEntityTag.method_10545("CollectionId") && !blockEntityTag.method_10545("Color")) {
               this.renderEntity.setColorOverride(null);
            }
         }

         poseStack.method_22903();
         if (displayContext == class_811.field_4317) {
            poseStack.method_22904(0.5, 0.5, 0.5);
            poseStack.method_22907(class_7833.field_40716.rotationDegrees(180.0F));
            poseStack.method_22904(-0.5, -0.4375, -0.5);
         }

         if (displayContext == class_811.field_4318) {
            poseStack.method_22904(0.5, 0.0, 0.5);
            poseStack.method_22905(0.7F, 0.7F, 0.7F);
            poseStack.method_22904(-0.5, 0.5, -0.5);
         }

         if (displayContext == class_811.field_4320 || displayContext == class_811.field_4323) {
            poseStack.method_22904(0.5, 0.0, 0.5);
            poseStack.method_22905(0.4F, 0.4F, 0.4F);
            poseStack.method_22904(-0.5, 1.0, -0.5);
         }

         float partialTick = class_310.method_1551().method_60646().method_60637(true);
         this.renderer.method_3569(this.renderEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
         poseStack.method_22909();
      }
   }
}
