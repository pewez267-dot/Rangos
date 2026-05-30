package com.theplumteam.block;

import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.client.particle.BlockParticleHelper;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.figure.FigureType;
import com.theplumteam.platform.PlatformHelper;
import com.theplumteam.registry.ModBlockEntities;
import com.theplumteam.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.registries.Registries;
import org.jetbrains.annotations.Nullable;

public class BoxBlock extends BaseEntityBlock {
   private static final TagKey<Item> SHEARS_TAG = TagKey.create(Registries.ITEM, new ResourceLocation("blockpops", "shears"));
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 14.0, 13.0);

   public BoxBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
   }

   @Override
   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      VoxelShape baseShape = SHAPE;
      if (!(level.getBlockEntity(pos) instanceof BoxBlockEntity boxBlockEntity)) {
         return baseShape;
      } else {
         double localOffsetX = boxBlockEntity.getHitboxOffsetX();
         double localOffsetY = boxBlockEntity.getHitboxOffsetY();
         double localOffsetZ = boxBlockEntity.getHitboxOffsetZ();
         double hitboxScaleX = boxBlockEntity.getHitboxScaleX();
         double hitboxScaleY = boxBlockEntity.getHitboxScaleY();
         double hitboxScaleZ = boxBlockEntity.getHitboxScaleZ();
         Direction facing = state.getValue(FACING);
         VoxelShape scaledShape = baseShape;
         if (hitboxScaleX != 1.0 || hitboxScaleY != 1.0 || hitboxScaleZ != 1.0) {
            double centerX = 8.0;
            double centerZ = 8.0;
            double effectiveScaleX = hitboxScaleX;
            double effectiveScaleZ = hitboxScaleZ;
            if (facing == Direction.EAST || facing == Direction.WEST) {
               effectiveScaleX = hitboxScaleZ;
               effectiveScaleZ = hitboxScaleX;
            }

            double minX = centerX + (3.0 - centerX) * effectiveScaleX;
            double minY = 0.0;
            double minZ = centerZ + (3.0 - centerZ) * effectiveScaleZ;
            double maxX = centerX + (13.0 - centerX) * effectiveScaleX;
            double maxY = 14.0 * hitboxScaleY;
            double maxZ = centerZ + (13.0 - centerZ) * effectiveScaleZ;
            scaledShape = Block.box(minX, minY, minZ, maxX, maxY, maxZ);
         }

         if (localOffsetX == 0.0 && localOffsetY == 0.0 && localOffsetZ == 0.0) {
            return scaledShape;
         } else {
            double worldOffsetX = 0.0;
            double worldOffsetZ = 0.0;
            switch (facing) {
               case NORTH:
                  worldOffsetX = localOffsetX;
                  worldOffsetZ = -localOffsetZ;
                  break;
               case SOUTH:
                  worldOffsetX = -localOffsetX;
                  worldOffsetZ = localOffsetZ;
                  break;
               case EAST:
                  worldOffsetX = localOffsetZ;
                  worldOffsetZ = localOffsetX;
                  break;
               case WEST:
                  worldOffsetX = -localOffsetZ;
                  worldOffsetZ = -localOffsetX;
               default:
            }

            return scaledShape.move(worldOffsetX, localOffsetY, worldOffsetZ);
         }
      }
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new BoxBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      return level.isClientSide ? createTickerHelper(blockEntityType, ModBlockEntities.BOX_BLOCK.get(), BoxBlockEntity::tick) : null;
   }

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.getBlockEntity(pos) instanceof BoxBlockEntity boxBlockEntity) {
         ItemStack heldItem = player.getItemInHand(hand);
         if (player.isShiftKeyDown()) {
            if (boxBlockEntity.isOpen() && !level.isClientSide) {
               boxBlockEntity.toggleOpen();
               return InteractionResult.SUCCESS;
            } else {
               if (!boxBlockEntity.isOpen()) {
                  if (!level.isClientSide) {
                     boxBlockEntity.cycleAlternativeSkin();
                     return InteractionResult.SUCCESS;
                  }

                  if (PlatformHelper.isDevelopmentEnvironment()) {
                     PlatformHelper.openBoxFigureScreen(pos, boxBlockEntity);
                     return InteractionResult.SUCCESS;
                  }
               }

               return InteractionResult.sidedSuccess(level.isClientSide);
            }
         } else {
            if (!level.isClientSide) {
               if (!boxBlockEntity.isOpen()) {
                  if (!heldItem.is(SHEARS_TAG) && !(heldItem.getItem() instanceof ShearsItem)) {
                     FigureDefinition figureDef = boxBlockEntity.getFigureDefinition();
                     if (figureDef != null && figureDef.hasAlternatives()) {
                        player.displayClientMessage(
                           Component.literal("Use Shears to open | Shift+Right-click to change skin").withStyle(ChatFormatting.GRAY), true
                        );
                     } else {
                        player.displayClientMessage(Component.literal("Use Shears to open").withStyle(ChatFormatting.GRAY), true);
                     }

                     return InteractionResult.SUCCESS;
                  }

                  boxBlockEntity.toggleOpen();
                  level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
                  return InteractionResult.SUCCESS;
               }

               if (heldItem.getItem() == ModItems.FIGURE_BLOCK_ITEM.get() && boxBlockEntity.isFigureExtracted()) {
                  CompoundTag blockEntityTag = BlockItem.getBlockEntityData(heldItem);
                  if (blockEntityTag != null) {
                     String heldFigureId = blockEntityTag.getString("FigureId");
                     String heldCollectionId = blockEntityTag.getString("CollectionId");
                     if (heldFigureId.equals(boxBlockEntity.getFigureId()) && heldCollectionId.equals(boxBlockEntity.getCollectionId())) {
                        if (blockEntityTag.contains("QuickSkinId")) {
                           boxBlockEntity.setQuickSkinId(blockEntityTag.getString("QuickSkinId"));
                        }

                        if (blockEntityTag.contains("SkinSnapshot")) {
                           boxBlockEntity.setSkinSnapshot(blockEntityTag.getString("SkinSnapshot"));
                        }

                        boxBlockEntity.setFigureExtracted(false);
                        boxBlockEntity.toggleOpen();
                        heldItem.shrink(1);
                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                        return InteractionResult.SUCCESS;
                     }
                  }
               } else if (boxBlockEntity.hasFigure() && !boxBlockEntity.isFigureExtracted()) {
                  ItemStack figureBlockItem = new ItemStack(ModItems.FIGURE_BLOCK_ITEM.get());
                  CompoundTag blockEntityTag = new CompoundTag();
                  blockEntityTag.putString("FigureId", boxBlockEntity.getFigureId());
                  blockEntityTag.putString("CollectionId", boxBlockEntity.getCollectionId());
                  blockEntityTag.putInt("AlternativeSkinIndex", boxBlockEntity.getAlternativeSkinIndex());
                  blockEntityTag.putInt("PoseIndex", boxBlockEntity.getPoseIndex());
                  blockEntityTag.putDouble("FigureOffsetX", boxBlockEntity.getFigureOffsetX());
                  blockEntityTag.putDouble("FigureOffsetY", boxBlockEntity.getFigureOffsetY());
                  blockEntityTag.putDouble("FigureOffsetZ", boxBlockEntity.getFigureOffsetZ());
                  blockEntityTag.putDouble("FigureScale", boxBlockEntity.getFigureScale());
                  FigureDefinition figureDef = boxBlockEntity.getFigureDefinition();
                  if (figureDef != null && figureDef.getType() == FigureType.PLAYER) {
                     String snapshot = boxBlockEntity.getSkinSnapshot();
                     if (snapshot != null && !snapshot.isEmpty()) {
                        blockEntityTag.putString("SkinSnapshot", snapshot);
                     }

                     String quickSkinId = boxBlockEntity.getQuickSkinId();
                     if (quickSkinId != null && !quickSkinId.isEmpty()) {
                        blockEntityTag.putString("QuickSkinId", quickSkinId);
                     }
                  }

                  blockEntityTag.putString("id", "blockpops:figure_block");
                  figureBlockItem.getOrCreateTag().put("BlockEntityTag", blockEntityTag);
                  if (!player.getInventory().add(figureBlockItem)) {
                     player.drop(figureBlockItem, false);
                  }

                  boxBlockEntity.setFigureExtracted(true);
                  level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                  return InteractionResult.SUCCESS;
               }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction playerFacing = context.getHorizontalDirection();
      Direction blockFacing = playerFacing.getOpposite();
      return this.defaultBlockState().setValue(FACING, blockFacing);
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof BoxBlockEntity boxBlockEntity) {
         CompoundTag tag = BlockItem.getBlockEntityData(stack);
         if (tag != null) {
            if (tag.contains("QuickSkinId")) {
               boxBlockEntity.setQuickSkinId(tag.getString("QuickSkinId"));
            }

            if (tag.contains("SkinSnapshot")) {
               boxBlockEntity.setSkinSnapshot(tag.getString("SkinSnapshot"));
            }
         }
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Override
   public void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
      if (!level.isClientSide
         || !(level.getBlockEntity(pos) instanceof BoxBlockEntity boxBlockEntity)
         || !BlockParticleHelper.spawnBoxDestroyParticles(level, pos, boxBlockEntity)) {
         if (level.getBlockEntity(pos) instanceof BoxBlockEntity boxBlockEntityx) {
            String collectionId = boxBlockEntityx.getCollectionId();
            FigureCollection collection = CollectionRegistry.getCollection(collectionId).orElse(null);
            if (collection != null && collection.hasBackgroundColor()) {
               int[] bgColor = collection.getBackgroundColor();
               BlockState woolState = getClosestWoolBlock(bgColor[0], bgColor[1], bgColor[2]);
               level.levelEvent(player, 2001, pos, Block.getId(woolState));
               return;
            }
         }

         super.spawnDestroyParticles(level, player, pos, state);
      }
   }

   private static BlockState getClosestWoolBlock(int r, int g, int b) {
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
      Block[] woolBlocks = new Block[]{
         Blocks.WHITE_WOOL,
         Blocks.ORANGE_WOOL,
         Blocks.MAGENTA_WOOL,
         Blocks.LIGHT_BLUE_WOOL,
         Blocks.YELLOW_WOOL,
         Blocks.LIME_WOOL,
         Blocks.PINK_WOOL,
         Blocks.GRAY_WOOL,
         Blocks.LIGHT_GRAY_WOOL,
         Blocks.CYAN_WOOL,
         Blocks.PURPLE_WOOL,
         Blocks.BLUE_WOOL,
         Blocks.BROWN_WOOL,
         Blocks.GREEN_WOOL,
         Blocks.RED_WOOL,
         Blocks.BLACK_WOOL
      };
      double minDist = Double.MAX_VALUE;
      int closestIdx = 0;

      for (int i = 0; i < woolColors.length; i++) {
         double dist = Math.pow(r - woolColors[i][0], 2.0)
            + Math.pow(g - woolColors[i][1], 2.0)
            + Math.pow(b - woolColors[i][2], 2.0);
         if (dist < minDist) {
            minDist = dist;
            closestIdx = i;
         }
      }

      return woolBlocks[closestIdx].defaultBlockState();
   }

   @Override
   public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof BoxBlockEntity boxBlockEntity) {
         String collectionId = boxBlockEntity.getCollectionId();
         PopBlockColor color = boxBlockEntity.getColor();
         ItemStack dropStack;
         if (color != null) {
            dropStack = new ItemStack(ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(color).get());
         } else if (collectionId != null && !collectionId.isEmpty() && ModItems.BOX_BLOCK_ITEMS.containsKey(collectionId)) {
            dropStack = new ItemStack(ModItems.BOX_BLOCK_ITEMS.get(collectionId).get());
         } else {
            dropStack = new ItemStack(this.asItem());
         }

         boxBlockEntity.saveToItem(dropStack);
         popResource(level, pos, dropStack);
      }

      super.playerWillDestroy(level, pos, state, player);
   }

   @Override
   public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
      ItemStack stack = super.getCloneItemStack(level, pos, state);
      if (level.getBlockEntity(pos) instanceof BoxBlockEntity boxBlockEntity) {
         boxBlockEntity.saveToItem(stack);
      }

      return stack;
   }
}
