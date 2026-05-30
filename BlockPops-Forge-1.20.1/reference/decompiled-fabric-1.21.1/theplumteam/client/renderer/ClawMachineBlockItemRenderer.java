package com.theplumteam.client.renderer;

import com.theplumteam.block.ClawMachineBlock;
import com.theplumteam.blockentity.ClawMachineBlockEntity;
import net.minecraft.class_1799;
import net.minecraft.class_2248;
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

public class ClawMachineBlockItemRenderer extends class_756 {
   private final ClawMachineBlockRenderer renderer = new ClawMachineBlockRenderer();
   private ClawMachineBlockEntity renderEntity;

   public ClawMachineBlockItemRenderer() {
      super(class_310.method_1551().method_31975(), class_310.method_1551().method_31974());
   }

   public void method_3166(class_1799 stack, class_811 displayContext, class_4587 poseStack, class_4597 bufferSource, int packedLight, int packedOverlay) {
      if (class_2248.method_9503(stack.method_7909()) instanceof ClawMachineBlock clawMachineBlock) {
         if (this.renderEntity == null || !this.renderEntity.method_11010().method_27852(clawMachineBlock)) {
            this.renderEntity = new ClawMachineBlockEntity(class_2338.field_10980, clawMachineBlock.method_9564());
         }

         class_9279 customData = (class_9279)stack.method_57824(class_9334.field_49611);
         if (customData != null) {
            class_2487 blockEntityTag = customData.method_57461();
            this.renderEntity.loadFromItemNbt(blockEntityTag);
         }

         poseStack.method_22903();
         poseStack.method_22904(0.5, 0.0, 0.5);
         poseStack.method_22907(class_7833.field_40716.rotationDegrees(90.0F));
         poseStack.method_22904(-0.5, 0.0, -0.5);
         float baseScale = 0.33333334F;
         if (displayContext == class_811.field_4318) {
            poseStack.method_22904(0.5, 0.0, 0.5);
            poseStack.method_22905(baseScale * 0.7F, baseScale * 0.7F, baseScale * 0.7F);
            poseStack.method_22904(-0.5, 0.5, -0.5);
         } else if (displayContext == class_811.field_4317) {
            poseStack.method_22904(0.5, 0.0, 0.5);
            poseStack.method_22905(baseScale, baseScale, baseScale);
            poseStack.method_22904(-0.5, 0.0, -0.5);
         } else if (displayContext == class_811.field_4320 || displayContext == class_811.field_4323) {
            poseStack.method_22904(0.5, 0.0, 0.5);
            poseStack.method_22905(baseScale * 0.8F, baseScale * 0.8F, baseScale * 0.8F);
            poseStack.method_22904(-0.5, 1.0, -0.5);
         } else if (displayContext != class_811.field_4322 && displayContext != class_811.field_4321) {
            poseStack.method_22904(0.5, 0.0, 0.5);
            poseStack.method_22905(baseScale, baseScale, baseScale);
            poseStack.method_22904(-0.5, 0.0, -0.5);
         } else {
            poseStack.method_22904(0.5, 0.0, 0.5);
            poseStack.method_22905(baseScale * 1.0F, baseScale * 1.0F, baseScale * 1.0F);
            poseStack.method_22904(-0.5, 0.0, -0.5);
         }

         float partialTick = class_310.method_1551().method_60646().method_60637(true);
         this.renderer.method_3569(this.renderEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
         poseStack.method_22909();
      }
   }
}
