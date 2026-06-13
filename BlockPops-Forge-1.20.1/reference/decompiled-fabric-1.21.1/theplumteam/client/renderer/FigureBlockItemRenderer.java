package com.theplumteam.client.renderer;

import com.theplumteam.block.FigureBlock;
import com.theplumteam.blockentity.FigureBlockEntity;
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

public class FigureBlockItemRenderer extends class_756 {
   private final FigureBlockRenderer renderer = new FigureBlockRenderer();
   private FigureBlockEntity renderEntity;

   public FigureBlockItemRenderer() {
      super(class_310.method_1551().method_31975(), class_310.method_1551().method_31974());
   }

   public void method_3166(class_1799 stack, class_811 displayContext, class_4587 poseStack, class_4597 bufferSource, int packedLight, int packedOverlay) {
      if (stack.method_7909() instanceof GeoBlockItem geoBlockItem) {
         if (!(geoBlockItem.method_7711() instanceof FigureBlock figureBlock)) {
            return;
         }

         if (this.renderEntity == null || !this.renderEntity.method_11010().method_27852(figureBlock)) {
            this.renderEntity = new FigureBlockEntity(class_2338.field_10980, figureBlock.method_9564());
         }

         class_9279 customData = (class_9279)stack.method_57824(class_9334.field_49611);
         if (customData != null) {
            class_2487 blockEntityTag = customData.method_57461();
            this.renderEntity.loadFromItemNbt(blockEntityTag);
         }

         poseStack.method_22903();
         poseStack.method_22904(0.5, 0.0, 0.5);
         poseStack.method_22907(class_7833.field_40716.rotationDegrees(180.0F));
         poseStack.method_22904(-0.5, 0.0, -0.5);
         if (displayContext == class_811.field_4317) {
            poseStack.method_22904(0.5, 0.0, 0.5);
            poseStack.method_22905(1.2F, 1.2F, 1.2F);
            poseStack.method_22904(-0.5, 0.0375F, -0.5);
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
