package com.theplumteam.item;

import com.theplumteam.block.BoxBlock;
import com.theplumteam.block.FigureBlock;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.figure.FigureDefinition;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class GeoBlockItem extends BlockItem {
   public GeoBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   public BoxBlock getBoxBlock() {
      return (BoxBlock)this.getBlock();
   }

   @Nullable
   private CompoundTag getBlockEntityTag(ItemStack stack) {
      return BlockItem.getBlockEntityData(stack);
   }

   @Override
   public Component getName(ItemStack stack) {
      String collectionId = null;
      if (this.getBlock() instanceof BoxBlock) {
         CompoundTag blockEntityTag = this.getBlockEntityTag(stack);
         String figureId = "";
         if (blockEntityTag != null) {
            if (blockEntityTag.contains("CollectionId")) {
               collectionId = blockEntityTag.getString("CollectionId");
            }

            if (blockEntityTag.contains("FigureId")) {
               figureId = blockEntityTag.getString("FigureId");
            }
         }

         if (collectionId != null && !collectionId.isEmpty()) {
            FigureCollection collection = CollectionRegistry.getCollection(collectionId).orElse(null);
            if (collection == null) {
               return super.getName(stack);
            } else {
               String collectionName = collection.getName();
               if (figureId != null && !figureId.isEmpty()) {
                  FigureDefinition figure = collection.getFigure(figureId).orElse(null);
                  if (figure != null) {
                     return Component.literal(figure.getName()).withStyle(ChatFormatting.WHITE);
                  }
               }

               return Component.literal(collectionName + " Box");
            }
         } else {
            return super.getName(stack);
         }
      } else {
         if (this.getBlock() instanceof FigureBlock) {
            CompoundTag blockEntityTagx = this.getBlockEntityTag(stack);
            String figureIdx = "";
            if (blockEntityTagx != null) {
               if (blockEntityTagx.contains("CollectionId")) {
                  collectionId = blockEntityTagx.getString("CollectionId");
               }

               if (blockEntityTagx.contains("FigureId")) {
                  figureIdx = blockEntityTagx.getString("FigureId");
               }
            }

            if (collectionId == null || collectionId.isEmpty() || figureIdx.isEmpty()) {
               return super.getName(stack);
            }

            FigureCollection collection = CollectionRegistry.getCollection(collectionId).orElse(null);
            if (collection == null) {
               return super.getName(stack);
            }

            FigureDefinition figure = collection.getFigure(figureIdx).orElse(null);
            if (figure != null) {
               return Component.literal(figure.getName()).withStyle(ChatFormatting.WHITE);
            }
         }

         return super.getName(stack);
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, level, tooltip, flag);
      String collectionId = null;
      String figureId = "";
      CompoundTag blockEntityTag = this.getBlockEntityTag(stack);
      if (this.getBlock() instanceof BoxBlock) {
         if (blockEntityTag != null) {
            if (blockEntityTag.contains("CollectionId")) {
               collectionId = blockEntityTag.getString("CollectionId");
            }

            if (blockEntityTag.contains("FigureId")) {
               figureId = blockEntityTag.getString("FigureId");
            }
         }
      } else {
         if (!(this.getBlock() instanceof FigureBlock)) {
            return;
         }

         if (blockEntityTag != null) {
            if (blockEntityTag.contains("CollectionId")) {
               collectionId = blockEntityTag.getString("CollectionId");
            }

            if (blockEntityTag.contains("FigureId")) {
               figureId = blockEntityTag.getString("FigureId");
            }
         }
      }

      if (collectionId != null && !collectionId.isEmpty()) {
         FigureCollection collection = CollectionRegistry.getCollection(collectionId).orElse(null);
         if (collection != null && figureId != null && !figureId.isEmpty()) {
            tooltip.add(Component.literal(collection.getName()).withStyle(ChatFormatting.GRAY));
            FigureDefinition figure = collection.getFigure(figureId).orElse(null);
            if (figure != null && figure.hasAlternatives()) {
               tooltip.add(
                  Component.translatable("tooltip.blockpops.has_alternatives").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)
               );
            }

            if (this.getBlock() instanceof FigureBlock) {
               tooltip.add(Component.translatable("tooltip.blockpops.pose_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
         }
      }
   }
}
