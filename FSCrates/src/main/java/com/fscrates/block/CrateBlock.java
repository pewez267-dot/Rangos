package com.fscrates.block;

import com.fscrates.config.CrateConfig;
import com.fscrates.crate.CrateOpeningService;
import com.fscrates.item.CrateItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The placeable crate block. Behaves like a chest: it faces the player, keeps
 * its data in a {@link CrateBlockEntity}, and is opened by right-clicking it
 * while holding the matching KEY in the main hand. Only OP level 4 can place or
 * break crates, matching the "admin-only" rule.
 */
public class CrateBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    public CrateBlock() {
        super(Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(-1.0F, 3600000.0F) // unbreakable by survival; OP uses /fscrate or creative
                .sound(SoundType.WOOD)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // MODEL so the block shows its normal model; a BER adds the spinning item on top.
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // ------------------------------------------------------------------
    // Placement: copy the crate config from the item's BlockEntityTag.
    // ------------------------------------------------------------------

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        CrateConfig cfg = CrateItems.readConfig(stack);
        if (cfg != null && level.getBlockEntity(pos) instanceof CrateBlockEntity be) {
            be.setConfig(cfg);
        }
    }

    // ------------------------------------------------------------------
    // Interaction: open with the matching key.
    // ------------------------------------------------------------------

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        CrateConfig crate = be.getConfig();
        ItemStack key = player.getMainHandItem();

        // Must hold the matching key for THIS crate.
        if (!CrateItems.isKey(key) || !CrateItems.crateId(key).equalsIgnoreCase(crate.id)) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00A7eNecesitas la \u00A7fllave\u00A7e de esta crate (" + crate.rarity.color()
                            + crate.displayName + "\u00A7e) en la mano."));
            return InteractionResult.CONSUME;
        }

        boolean skip = crate.allowSkip && player.isShiftKeyDown();
        CrateOpeningService.open(serverPlayer, crate, key, skip);
        return InteractionResult.CONSUME;
    }
}
