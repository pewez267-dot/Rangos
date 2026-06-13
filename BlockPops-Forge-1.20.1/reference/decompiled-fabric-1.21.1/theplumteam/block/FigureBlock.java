package com.theplumteam.block;

import com.mojang.serialization.MapCodec;
import com.theplumteam.blockentity.FigureBlockEntity;
import com.theplumteam.client.particle.BlockParticleHelper;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.registry.ModBlockEntities;
import com.theplumteam.registry.ModItems;
import net.minecraft.class_1269;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1750;
import net.minecraft.class_1799;
import net.minecraft.class_1922;
import net.minecraft.class_1935;
import net.minecraft.class_1937;
import net.minecraft.class_2237;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2383;
import net.minecraft.class_2464;
import net.minecraft.class_2487;
import net.minecraft.class_2586;
import net.minecraft.class_2591;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_2753;
import net.minecraft.class_2769;
import net.minecraft.class_3726;
import net.minecraft.class_3965;
import net.minecraft.class_4538;
import net.minecraft.class_5558;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import net.minecraft.class_2689.class_2690;
import net.minecraft.class_4970.class_2251;
import org.jetbrains.annotations.Nullable;

public class FigureBlock extends class_2237 {
   public static final MapCodec<FigureBlock> CODEC = method_54094(FigureBlock::new);
   public static final class_2753 FACING = class_2383.field_11177;
   private static final class_265 SHAPE = class_2248.method_9541(5.0, 0.0, 5.0, 11.0, 12.0, 11.0);

   public FigureBlock(class_2251 properties) {
      super(properties);
      this.method_9590((class_2680)((class_2680)this.field_10647.method_11664()).method_11657(FACING, class_2350.field_11043));
   }

   protected MapCodec<? extends class_2237> method_53969() {
      return CODEC;
   }

   public class_265 method_9530(class_2680 state, class_1922 level, class_2338 pos, class_3726 context) {
      return SHAPE;
   }

   @Nullable
   public class_2586 method_10123(class_2338 pos, class_2680 state) {
      return new FigureBlockEntity(pos, state);
   }

   @Nullable
   public <T extends class_2586> class_5558<T> method_31645(class_1937 level, class_2680 state, class_2591<T> blockEntityType) {
      return level.field_9236 ? method_31618(blockEntityType, (class_2591)ModBlockEntities.FIGURE_BLOCK.get(), FigureBlockEntity::tick) : null;
   }

   public class_2464 method_9604(class_2680 state) {
      return class_2464.field_11456;
   }

   protected class_1269 method_55766(class_2680 state, class_1937 level, class_2338 pos, class_1657 player, class_3965 hit) {
      if (!player.method_5715()) {
         if (!level.method_8608() && level.method_8321(pos) instanceof FigureBlockEntity figureBlockEntity && figureBlockEntity.hasFigure()) {
            figureBlockEntity.cycleAlternativeSkin();
         }

         return class_1269.method_29236(level.method_8608());
      } else {
         return class_1269.field_5811;
      }
   }

   @Nullable
   public class_2680 method_9605(class_1750 context) {
      class_2350 playerFacing = context.method_8042();
      class_2350 blockFacing = playerFacing.method_10153();
      return (class_2680)this.method_9564().method_11657(FACING, blockFacing);
   }

   public void method_9567(class_1937 level, class_2338 pos, class_2680 state, @Nullable class_1309 placer, class_1799 stack) {
      super.method_9567(level, pos, state, placer, stack);
      if (!level.field_9236 && level.method_8321(pos) instanceof FigureBlockEntity figureBlockEntity) {
         class_9279 customData = (class_9279)stack.method_57824(class_9334.field_49611);
         if (customData != null) {
            class_2487 tag = customData.method_57461();
            if (tag.method_10545("QuickSkinId")) {
               figureBlockEntity.setQuickSkinId(tag.method_10558("QuickSkinId"));
            }

            if (tag.method_10545("SkinSnapshot")) {
               figureBlockEntity.setSkinSnapshot(tag.method_10558("SkinSnapshot"));
            }
         }
      }
   }

   protected void method_9515(class_2690<class_2248, class_2680> builder) {
      builder.method_11667(new class_2769[]{FACING});
   }

   protected void method_33614(class_1937 level, class_1657 player, class_2338 pos, class_2680 state) {
      if (!level.field_9236
         || !(level.method_8321(pos) instanceof FigureBlockEntity figureBlockEntity)
         || !BlockParticleHelper.spawnFigureDestroyParticles(level, pos, figureBlockEntity)) {
         if (level.method_8321(pos) instanceof FigureBlockEntity figureBlockEntityx) {
            String collectionId = figureBlockEntityx.getCollectionId();
            FigureCollection collection = CollectionRegistry.getCollection(collectionId).orElse(null);
            if (collection != null && collection.hasBackgroundColor()) {
               int[] bgColor = collection.getBackgroundColor();
               class_2680 woolState = getClosestWoolBlock(bgColor[0], bgColor[1], bgColor[2]);
               level.method_8444(player, 2001, pos, class_2248.method_9507(woolState));
               return;
            }
         }

         super.method_33614(level, player, pos, state);
      }
   }

   private static class_2680 getClosestWoolBlock(int r, int g, int b) {
      int[][] woolColors = new int[][]{
         {233, 236, 236},
         {240, 118, 19},
         {189, 68, 179},
         {58, 175, 217},
         {248, 198, 39},
         {112, 185, 25},
         {237, 141, 172},
         {62, 68, 71},
         {142, 142, 134},
         {21, 137, 145},
         {121, 42, 172},
         {53, 57, 157},
         {114, 71, 40},
         {84, 109, 27},
         {161, 39, 34},
         {20, 21, 25}
      };
      class_2248[] woolBlocks = new class_2248[]{
         class_2246.field_10446,
         class_2246.field_10095,
         class_2246.field_10215,
         class_2246.field_10294,
         class_2246.field_10490,
         class_2246.field_10028,
         class_2246.field_10459,
         class_2246.field_10423,
         class_2246.field_10222,
         class_2246.field_10619,
         class_2246.field_10259,
         class_2246.field_10514,
         class_2246.field_10113,
         class_2246.field_10170,
         class_2246.field_10314,
         class_2246.field_10146
      };
      double minDist = Double.MAX_VALUE;
      int closestIdx = 0;

      for (int i = 0; i < woolColors.length; i++) {
         double dist = Math.pow((double)(r - woolColors[i][0]), 2.0)
            + Math.pow((double)(g - woolColors[i][1]), 2.0)
            + Math.pow((double)(b - woolColors[i][2]), 2.0);
         if (dist < minDist) {
            minDist = dist;
            closestIdx = i;
         }
      }

      return woolBlocks[closestIdx].method_9564();
   }

   public class_2680 method_9576(class_1937 level, class_2338 pos, class_2680 state, class_1657 player) {
      if (!level.field_9236 && level.method_8321(pos) instanceof FigureBlockEntity figureBlockEntity) {
         class_1799 dropStack = new class_1799((class_1935)ModItems.FIGURE_BLOCK_ITEM.get());
         figureBlockEntity.saveToItem(dropStack);
         method_9577(level, pos, dropStack);
      }

      return super.method_9576(level, pos, state, player);
   }

   public class_1799 method_9574(class_4538 level, class_2338 pos, class_2680 state) {
      class_1799 stack = super.method_9574(level, pos, state);
      if (level.method_8321(pos) instanceof FigureBlockEntity figureBlockEntity) {
         figureBlockEntity.saveToItem(stack);
      }

      return stack;
   }
}
