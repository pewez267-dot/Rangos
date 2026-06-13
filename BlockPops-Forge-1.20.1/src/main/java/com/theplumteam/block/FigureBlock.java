package com.theplumteam.block;

import com.theplumteam.blockentity.FigureBlockEntity;
import com.theplumteam.client.particle.BlockParticleHelper;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.registry.ModBlockEntities;
import com.theplumteam.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import org.jetbrains.annotations.Nullable;

public class FigureBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 12.0, 11.0);

   public FigureBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
   }

   @Override
   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new FigureBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      return level.isClientSide ? createTickerHelper(blockEntityType, ModBlockEntities.FIGURE_BLOCK.get(), FigureBlockEntity::tick) : null;
   }

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!player.isShiftKeyDown()) {
         if (!level.isClientSide && level.getBlockEntity(pos) instanceof FigureBlockEntity figureBlockEntity && figureBlockEntity.hasFigure()) {
            figureBlockEntity.cycleAlternativeSkin();
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
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
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof FigureBlockEntity figureBlockEntity) {
         CompoundTag tag = BlockItem.getBlockEntityData(stack);
         if (tag != null) {
            if (tag.contains("QuickSkinId")) {
               figureBlockEntity.setQuickSkinId(tag.getString("QuickSkinId"));
            }

            if (tag.contains("SkinSnapshot")) {
               figureBlockEntity.setSkinSnapshot(tag.getString("SkinSnapshot"));
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
         || !(level.getBlockEntity(pos) instanceof FigureBlockEntity figureBlockEntity)
         || !BlockParticleHelper.spawnFigureDestroyParticles(level, pos, figureBlockEntity)) {
         if (level.getBlockEntity(pos) instanceof FigureBlockEntity figureBlockEntityx) {
            String collectionId = figureBlockEntityx.getCollectionId();
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
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof FigureBlockEntity figureBlockEntity) {
         ItemStack dropStack = new ItemStack(ModItems.FIGURE_BLOCK_ITEM.get());
         figureBlockEntity.saveToItem(dropStack);
         popResource(level, pos, dropStack);
      }

      super.playerWillDestroy(level, pos, state, player);
   }

   @Override
   public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
      ItemStack stack = super.getCloneItemStack(level, pos, state);
      if (level.getBlockEntity(pos) instanceof FigureBlockEntity figureBlockEntity) {
         figureBlockEntity.saveToItem(stack);
      }

      return stack;
   }
}
