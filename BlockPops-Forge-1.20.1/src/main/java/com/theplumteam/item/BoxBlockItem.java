package com.theplumteam.item;

import com.theplumteam.block.PopBlockColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class BoxBlockItem extends GeoBlockItem {
   @Nullable
   private final String collectionId;
   @Nullable
   private final PopBlockColor color;

   public BoxBlockItem(Block block, Properties properties, String collectionId) {
      super(block, properties);
      this.collectionId = collectionId;
      this.color = null;
   }

   public BoxBlockItem(Block block, Properties properties, PopBlockColor color) {
      super(block, properties);
      this.collectionId = null;
      this.color = color;
   }

   @Override
   public ItemStack getDefaultInstance() {
      ItemStack stack = super.getDefaultInstance();
      CompoundTag blockEntityTag = new CompoundTag();
      if (this.collectionId != null) {
         blockEntityTag.putString("CollectionId", this.collectionId);
      }

      if (this.color != null) {
         blockEntityTag.putString("Color", this.color.getSerializedName());
      }

      if (!blockEntityTag.isEmpty()) {
         blockEntityTag.putString("id", "blockpops:box_block");
         stack.getOrCreateTag().put("BlockEntityTag", blockEntityTag);
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
