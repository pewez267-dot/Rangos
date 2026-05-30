package com.theplumteam.item;

import com.theplumteam.block.PopBlockColor;
import net.minecraft.class_1799;
import net.minecraft.class_2248;
import net.minecraft.class_2487;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import net.minecraft.class_1792.class_1793;
import org.jetbrains.annotations.Nullable;

public class BoxBlockItem extends GeoBlockItem {
   @Nullable
   private final String collectionId;
   @Nullable
   private final PopBlockColor color;

   public BoxBlockItem(class_2248 block, class_1793 properties, String collectionId) {
      super(block, properties);
      this.collectionId = collectionId;
      this.color = null;
   }

   public BoxBlockItem(class_2248 block, class_1793 properties, PopBlockColor color) {
      super(block, properties);
      this.collectionId = null;
      this.color = color;
   }

   public class_1799 method_7854() {
      class_1799 stack = super.method_7854();
      class_2487 blockEntityTag = new class_2487();
      if (this.collectionId != null) {
         blockEntityTag.method_10582("CollectionId", this.collectionId);
      }

      if (this.color != null) {
         blockEntityTag.method_10582("Color", this.color.method_15434());
      }

      if (!blockEntityTag.method_33133()) {
         blockEntityTag.method_10582("id", "blockpops:box_block");
         stack.method_57379(class_9334.field_49611, class_9279.method_57456(blockEntityTag));
      }

      return stack;
   }

   @Nullable
   public String getCollectionId() {
      return this.collectionId;
   }

   @Nullable
   public PopBlockColor getColor() {
      return this.color;
   }
}
