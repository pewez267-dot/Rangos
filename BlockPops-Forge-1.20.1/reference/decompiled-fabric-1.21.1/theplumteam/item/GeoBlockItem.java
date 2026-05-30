package com.theplumteam.item;

import com.theplumteam.block.BoxBlock;
import com.theplumteam.block.FigureBlock;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.figure.FigureDefinition;
import java.util.List;
import net.minecraft.class_124;
import net.minecraft.class_1747;
import net.minecraft.class_1799;
import net.minecraft.class_1836;
import net.minecraft.class_2248;
import net.minecraft.class_2487;
import net.minecraft.class_2561;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import net.minecraft.class_1792.class_1793;
import net.minecraft.class_1792.class_9635;

public class GeoBlockItem extends class_1747 {
   public GeoBlockItem(class_2248 block, class_1793 properties) {
      super(block, properties);
   }

   public BoxBlock getBoxBlock() {
      return (BoxBlock)this.method_7711();
   }

   private class_2487 getBlockEntityTag(class_1799 stack) {
      class_9279 customData = (class_9279)stack.method_57824(class_9334.field_49611);
      return customData != null ? customData.method_57461() : null;
   }

   public class_2561 method_7864(class_1799 stack) {
      String collectionId = null;
      if (this.method_7711() instanceof BoxBlock boxBlock) {
         class_2487 blockEntityTag = this.getBlockEntityTag(stack);
         String figureId = "";
         if (blockEntityTag != null) {
            if (blockEntityTag.method_10545("CollectionId")) {
               collectionId = blockEntityTag.method_10558("CollectionId");
            }

            if (blockEntityTag.method_10545("FigureId")) {
               figureId = blockEntityTag.method_10558("FigureId");
            }
         }

         if (collectionId != null && !collectionId.isEmpty()) {
            FigureCollection collection = CollectionRegistry.getCollection(collectionId).orElse(null);
            if (collection == null) {
               return super.method_7864(stack);
            } else {
               String collectionName = collection.getName();
               if (figureId != null && !figureId.isEmpty()) {
                  FigureDefinition figure = collection.getFigure(figureId).orElse(null);
                  if (figure != null) {
                     return class_2561.method_43470(figure.getName()).method_27692(class_124.field_1068);
                  }
               }

               return class_2561.method_43470(collectionName + " Box");
            }
         } else {
            return super.method_7864(stack);
         }
      } else {
         if (this.method_7711() instanceof FigureBlock) {
            class_2487 blockEntityTagx = this.getBlockEntityTag(stack);
            String figureIdx = "";
            if (blockEntityTagx != null) {
               if (blockEntityTagx.method_10545("CollectionId")) {
                  collectionId = blockEntityTagx.method_10558("CollectionId");
               }

               if (blockEntityTagx.method_10545("FigureId")) {
                  figureIdx = blockEntityTagx.method_10558("FigureId");
               }
            }

            if (collectionId == null || collectionId.isEmpty() || figureIdx.isEmpty()) {
               return super.method_7864(stack);
            }

            FigureCollection collection = CollectionRegistry.getCollection(collectionId).orElse(null);
            if (collection == null) {
               return super.method_7864(stack);
            }

            FigureDefinition figure = collection.getFigure(figureIdx).orElse(null);
            if (figure != null) {
               return class_2561.method_43470(figure.getName()).method_27692(class_124.field_1068);
            }
         }

         return super.method_7864(stack);
      }
   }

   public void method_7851(class_1799 stack, class_9635 context, List<class_2561> tooltip, class_1836 flag) {
      super.method_7851(stack, context, tooltip, flag);
      String collectionId = null;
      String figureId = "";
      class_2487 blockEntityTag = this.getBlockEntityTag(stack);
      if (this.method_7711() instanceof BoxBlock boxBlock) {
         if (blockEntityTag != null) {
            if (blockEntityTag.method_10545("CollectionId")) {
               collectionId = blockEntityTag.method_10558("CollectionId");
            }

            if (blockEntityTag.method_10545("FigureId")) {
               figureId = blockEntityTag.method_10558("FigureId");
            }
         }
      } else {
         if (!(this.method_7711() instanceof FigureBlock)) {
            return;
         }

         if (blockEntityTag != null) {
            if (blockEntityTag.method_10545("CollectionId")) {
               collectionId = blockEntityTag.method_10558("CollectionId");
            }

            if (blockEntityTag.method_10545("FigureId")) {
               figureId = blockEntityTag.method_10558("FigureId");
            }
         }
      }

      if (collectionId != null && !collectionId.isEmpty()) {
         FigureCollection collection = CollectionRegistry.getCollection(collectionId).orElse(null);
         if (collection != null && figureId != null && !figureId.isEmpty()) {
            tooltip.add(class_2561.method_43470(collection.getName()).method_27692(class_124.field_1080));
            FigureDefinition figure = collection.getFigure(figureId).orElse(null);
            if (figure != null && figure.hasAlternatives()) {
               tooltip.add(
                  class_2561.method_43471("tooltip.blockpops.has_alternatives").method_27695(new class_124[]{class_124.field_1064, class_124.field_1056})
               );
            }

            if (this.method_7711() instanceof FigureBlock) {
               tooltip.add(class_2561.method_43471("tooltip.blockpops.pose_hint").method_27695(new class_124[]{class_124.field_1063, class_124.field_1056}));
            }
         }
      }
   }
}
