package com.theplumteam.block;

import com.mojang.serialization.MapCodec;
import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.client.particle.BlockParticleHelper;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.figure.FigureType;
import com.theplumteam.platform.PlatformHelper;
import com.theplumteam.registry.ModBlockEntities;
import com.theplumteam.registry.ModItems;
import net.minecraft.class_124;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1750;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1820;
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
import net.minecraft.class_2561;
import net.minecraft.class_2586;
import net.minecraft.class_2591;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_2753;
import net.minecraft.class_2769;
import net.minecraft.class_2960;
import net.minecraft.class_3417;
import net.minecraft.class_3419;
import net.minecraft.class_3726;
import net.minecraft.class_3965;
import net.minecraft.class_4538;
import net.minecraft.class_5558;
import net.minecraft.class_6862;
import net.minecraft.class_7924;
import net.minecraft.class_9062;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import net.minecraft.class_2689.class_2690;
import net.minecraft.class_4970.class_2251;
import org.jetbrains.annotations.Nullable;

public class BoxBlock extends class_2237 {
   private static final class_6862<class_1792> SHEARS_TAG = class_6862.method_40092(class_7924.field_41197, class_2960.method_60655("blockpops", "shears"));
   public static final MapCodec<BoxBlock> CODEC = method_54094(BoxBlock::new);
   public static final class_2753 FACING = class_2383.field_11177;
   private static final class_265 SHAPE = class_2248.method_9541(3.0, 0.0, 3.0, 13.0, 14.0, 13.0);

   public BoxBlock(class_2251 properties) {
      super(properties);
      this.method_9590((class_2680)((class_2680)this.field_10647.method_11664()).method_11657(FACING, class_2350.field_11043));
   }

   protected MapCodec<? extends class_2237> method_53969() {
      return CODEC;
   }

   public class_265 method_9530(class_2680 state, class_1922 level, class_2338 pos, class_3726 context) {
      class_265 baseShape = SHAPE;
      if (!(level.method_8321(pos) instanceof BoxBlockEntity boxBlockEntity)) {
         return baseShape;
      } else {
         double localOffsetX = boxBlockEntity.getHitboxOffsetX();
         double localOffsetY = boxBlockEntity.getHitboxOffsetY();
         double localOffsetZ = boxBlockEntity.getHitboxOffsetZ();
         double hitboxScaleX = boxBlockEntity.getHitboxScaleX();
         double hitboxScaleY = boxBlockEntity.getHitboxScaleY();
         double hitboxScaleZ = boxBlockEntity.getHitboxScaleZ();
         class_2350 facing = (class_2350)state.method_11654(FACING);
         class_265 scaledShape = baseShape;
         if (hitboxScaleX != 1.0 || hitboxScaleY != 1.0 || hitboxScaleZ != 1.0) {
            double centerX = 8.0;
            double centerY = 7.0;
            double centerZ = 8.0;
            double effectiveScaleX = hitboxScaleX;
            double effectiveScaleZ = hitboxScaleZ;
            if (facing == class_2350.field_11034 || facing == class_2350.field_11039) {
               effectiveScaleX = hitboxScaleZ;
               effectiveScaleZ = hitboxScaleX;
            }

            double minX = centerX + (3.0 - centerX) * effectiveScaleX;
            double minY = 0.0;
            double minZ = centerZ + (3.0 - centerZ) * effectiveScaleZ;
            double maxX = centerX + (13.0 - centerX) * effectiveScaleX;
            double maxY = 14.0 * hitboxScaleY;
            double maxZ = centerZ + (13.0 - centerZ) * effectiveScaleZ;
            scaledShape = class_2248.method_9541(minX, minY, minZ, maxX, maxY, maxZ);
         }

         if (localOffsetX == 0.0 && localOffsetY == 0.0 && localOffsetZ == 0.0) {
            return scaledShape;
         } else {
            double worldOffsetX = 0.0;
            double worldOffsetZ = 0.0;
            switch (facing) {
               case field_11043:
                  worldOffsetX = localOffsetX;
                  worldOffsetZ = -localOffsetZ;
                  break;
               case field_11035:
                  worldOffsetX = -localOffsetX;
                  worldOffsetZ = localOffsetZ;
                  break;
               case field_11034:
                  worldOffsetX = localOffsetZ;
                  worldOffsetZ = localOffsetX;
                  break;
               case field_11039:
                  worldOffsetX = -localOffsetZ;
                  worldOffsetZ = -localOffsetX;
            }

            return scaledShape.method_1096(worldOffsetX, localOffsetY, worldOffsetZ);
         }
      }
   }

   @Nullable
   public class_2586 method_10123(class_2338 pos, class_2680 state) {
      return new BoxBlockEntity(pos, state);
   }

   @Nullable
   public <T extends class_2586> class_5558<T> method_31645(class_1937 level, class_2680 state, class_2591<T> blockEntityType) {
      return level.field_9236 ? method_31618(blockEntityType, (class_2591)ModBlockEntities.BOX_BLOCK.get(), BoxBlockEntity::tick) : null;
   }

   public class_2464 method_9604(class_2680 state) {
      return class_2464.field_11456;
   }

   protected class_9062 method_55765(
      class_1799 heldItem, class_2680 state, class_1937 level, class_2338 pos, class_1657 player, class_1268 hand, class_3965 hit
   ) {
      if (level.method_8321(pos) instanceof BoxBlockEntity boxBlockEntity) {
         if (player.method_5715()) {
            if (boxBlockEntity.isOpen() && !level.field_9236) {
               boxBlockEntity.toggleOpen();
               return class_9062.field_47728;
            } else {
               if (!boxBlockEntity.isOpen()) {
                  if (!level.field_9236) {
                     boxBlockEntity.cycleAlternativeSkin();
                     return class_9062.field_47728;
                  }

                  if (PlatformHelper.isDevelopmentEnvironment()) {
                     PlatformHelper.openBoxFigureScreen(pos, boxBlockEntity);
                     return class_9062.field_47728;
                  }
               }

               return class_9062.method_55644(level.field_9236);
            }
         } else {
            if (!level.field_9236) {
               if (!boxBlockEntity.isOpen()) {
                  if (!heldItem.method_31573(SHEARS_TAG) && !(heldItem.method_7909() instanceof class_1820)) {
                     FigureDefinition figureDef = boxBlockEntity.getFigureDefinition();
                     if (figureDef != null && figureDef.hasAlternatives()) {
                        player.method_7353(
                           class_2561.method_43470("Use Shears to open | Shift+Right-click to change skin").method_27692(class_124.field_1080), true
                        );
                     } else {
                        player.method_7353(class_2561.method_43470("Use Shears to open").method_27692(class_124.field_1080), true);
                     }

                     return class_9062.field_47728;
                  }

                  boxBlockEntity.toggleOpen();
                  level.method_8396(null, pos, class_3417.field_14975, class_3419.field_15245, 1.0F, 1.0F);
                  return class_9062.field_47728;
               }

               if (heldItem.method_7909() == ModItems.FIGURE_BLOCK_ITEM.get() && boxBlockEntity.isFigureExtracted()) {
                  class_9279 customData = (class_9279)heldItem.method_57824(class_9334.field_49611);
                  if (customData != null) {
                     class_2487 blockEntityTag = customData.method_57461();
                     String heldFigureId = blockEntityTag.method_10558("FigureId");
                     String heldCollectionId = blockEntityTag.method_10558("CollectionId");
                     if (heldFigureId.equals(boxBlockEntity.getFigureId()) && heldCollectionId.equals(boxBlockEntity.getCollectionId())) {
                        if (blockEntityTag.method_10545("QuickSkinId")) {
                           boxBlockEntity.setQuickSkinId(blockEntityTag.method_10558("QuickSkinId"));
                        }

                        if (blockEntityTag.method_10545("SkinSnapshot")) {
                           boxBlockEntity.setSkinSnapshot(blockEntityTag.method_10558("SkinSnapshot"));
                        }

                        boxBlockEntity.setFigureExtracted(false);
                        boxBlockEntity.toggleOpen();
                        heldItem.method_7934(1);
                        level.method_8396(null, pos, class_3417.field_14667, class_3419.field_15245, 1.0F, 1.0F);
                        return class_9062.field_47728;
                     }
                  }
               } else if (boxBlockEntity.hasFigure() && !boxBlockEntity.isFigureExtracted()) {
                  class_1799 figureBlockItem = new class_1799((class_1935)ModItems.FIGURE_BLOCK_ITEM.get());
                  class_2487 blockEntityTag = new class_2487();
                  blockEntityTag.method_10582("FigureId", boxBlockEntity.getFigureId());
                  blockEntityTag.method_10582("CollectionId", boxBlockEntity.getCollectionId());
                  blockEntityTag.method_10569("AlternativeSkinIndex", boxBlockEntity.getAlternativeSkinIndex());
                  blockEntityTag.method_10569("PoseIndex", boxBlockEntity.getPoseIndex());
                  blockEntityTag.method_10549("FigureOffsetX", boxBlockEntity.getFigureOffsetX());
                  blockEntityTag.method_10549("FigureOffsetY", boxBlockEntity.getFigureOffsetY());
                  blockEntityTag.method_10549("FigureOffsetZ", boxBlockEntity.getFigureOffsetZ());
                  blockEntityTag.method_10549("FigureScale", boxBlockEntity.getFigureScale());
                  FigureDefinition figureDef = boxBlockEntity.getFigureDefinition();
                  if (figureDef != null && figureDef.getType() == FigureType.PLAYER) {
                     String snapshot = boxBlockEntity.getSkinSnapshot();
                     if (snapshot != null && !snapshot.isEmpty()) {
                        blockEntityTag.method_10582("SkinSnapshot", snapshot);
                     }

                     String quickSkinId = boxBlockEntity.getQuickSkinId();
                     if (quickSkinId != null && !quickSkinId.isEmpty()) {
                        blockEntityTag.method_10582("QuickSkinId", quickSkinId);
                     }
                  }

                  blockEntityTag.method_10582("id", "blockpops:figure_block");
                  figureBlockItem.method_57379(class_9334.field_49611, class_9279.method_57456(blockEntityTag));
                  if (!player.method_31548().method_7394(figureBlockItem)) {
                     player.method_7328(figureBlockItem, false);
                  }

                  boxBlockEntity.setFigureExtracted(true);
                  level.method_8396(null, pos, class_3417.field_14770, class_3419.field_15245, 1.0F, 1.0F);
                  return class_9062.field_47728;
               }
            }

            return class_9062.method_55644(level.field_9236);
         }
      } else {
         return class_9062.field_47731;
      }
   }

   protected class_1269 method_55766(class_2680 state, class_1937 level, class_2338 pos, class_1657 player, class_3965 hitResult) {
      if (level.method_8321(pos) instanceof BoxBlockEntity boxBlockEntity) {
         if (player.method_5715()) {
            if (boxBlockEntity.isOpen() && !level.field_9236) {
               boxBlockEntity.toggleOpen();
               return class_1269.field_5812;
            }

            if (!boxBlockEntity.isOpen() && level.field_9236 && PlatformHelper.isDevelopmentEnvironment()) {
               PlatformHelper.openBoxFigureScreen(pos, boxBlockEntity);
               return class_1269.field_5812;
            }
         }

         return class_1269.method_29236(level.field_9236);
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
      if (!level.field_9236 && level.method_8321(pos) instanceof BoxBlockEntity boxBlockEntity) {
         class_9279 customData = (class_9279)stack.method_57824(class_9334.field_49611);
         if (customData != null) {
            class_2487 tag = customData.method_57461();
            if (tag.method_10545("QuickSkinId")) {
               boxBlockEntity.setQuickSkinId(tag.method_10558("QuickSkinId"));
            }

            if (tag.method_10545("SkinSnapshot")) {
               boxBlockEntity.setSkinSnapshot(tag.method_10558("SkinSnapshot"));
            }
         }
      }
   }

   protected void method_9515(class_2690<class_2248, class_2680> builder) {
      builder.method_11667(new class_2769[]{FACING});
   }

   protected void method_33614(class_1937 level, class_1657 player, class_2338 pos, class_2680 state) {
      if (!level.field_9236
         || !(level.method_8321(pos) instanceof BoxBlockEntity boxBlockEntity)
         || !BlockParticleHelper.spawnBoxDestroyParticles(level, pos, boxBlockEntity)) {
         if (level.method_8321(pos) instanceof BoxBlockEntity boxBlockEntityx) {
            String collectionId = boxBlockEntityx.getCollectionId();
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
      if (!level.field_9236 && level.method_8321(pos) instanceof BoxBlockEntity boxBlockEntity) {
         String collectionId = boxBlockEntity.getCollectionId();
         PopBlockColor color = boxBlockEntity.getColor();
         class_1799 dropStack;
         if (color != null) {
            dropStack = new class_1799((class_1935)ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(color).get());
         } else if (collectionId != null && !collectionId.isEmpty() && ModItems.BOX_BLOCK_ITEMS.containsKey(collectionId)) {
            dropStack = new class_1799((class_1935)ModItems.BOX_BLOCK_ITEMS.get(collectionId).get());
         } else {
            dropStack = new class_1799(this.method_8389());
         }

         boxBlockEntity.saveToItem(dropStack);
         method_9577(level, pos, dropStack);
      }

      return super.method_9576(level, pos, state, player);
   }

   public class_1799 method_9574(class_4538 level, class_2338 pos, class_2680 state) {
      class_1799 stack = super.method_9574(level, pos, state);
      if (level.method_8321(pos) instanceof BoxBlockEntity boxBlockEntity) {
         boxBlockEntity.saveToItem(stack);
      }

      return stack;
   }
}
