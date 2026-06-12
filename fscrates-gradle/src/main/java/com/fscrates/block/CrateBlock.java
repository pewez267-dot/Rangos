// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.block;

import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import com.fscrates.config.Rarity;
import com.fscrates.crate.CrateOpeningService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import com.fscrates.config.CrateConfig;
import com.fscrates.item.CrateItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import com.fscrates.registry.ModRegistry;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.BaseEntityBlock;

public class CrateBlock extends BaseEntityBlock
{
    public static final DirectionProperty FACING;
    private static final VoxelShape SHAPE;
    
    public CrateBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0f, 3600000.0f).sound(SoundType.WOOD).noOcclusion());
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)CrateBlock.FACING, (Comparable)Direction.NORTH));
    }
    
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[] { (Property)CrateBlock.FACING });
    }
    
    @Nullable
    public BlockState getStateForPlacement(final BlockPlaceContext ctx) {
        return (BlockState)this.defaultBlockState().setValue((Property)CrateBlock.FACING, (Comparable)ctx.getHorizontalDirection().getOpposite());
    }
    
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new CrateBlockEntity(pos, state);
    }
    
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext ctx) {
        return CrateBlock.SHAPE;
    }
    
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (!level.isClientSide || type != ModRegistry.CRATE_BE.get()) {
            return null;
        }
        return (BlockEntityTicker<T>)((lvl, pos, st, be) -> CrateBlockEntity.clientTick(lvl, pos, st, (CrateBlockEntity)be));
    }
    
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        final CrateConfig cfg = CrateItems.readConfig(stack);
        if (cfg != null) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof final CrateBlockEntity be) {
                be.setConfig(cfg);
            }
        }
    }
    
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CrateBlockEntity)) {
            return InteractionResult.PASS;
        }
        final CrateBlockEntity be = (CrateBlockEntity)blockEntity;
        if (!(player instanceof ServerPlayer)) {
            return InteractionResult.PASS;
        }
        final ServerPlayer serverPlayer = (ServerPlayer)player;
        final CrateConfig crate = be.getConfig();
        final ItemStack key = player.getMainHandItem();
        final Rarity keyTier = CrateItems.keyRarity(key);
        if (keyTier == null) {
            serverPlayer.sendSystemMessage((Component)Component.literal("§eNecesitas una §fllave " + String.valueOf(crate.rarity.color()) + crate.rarity.displayName() + "§e en la mano para abrir esta crate."));
            return InteractionResult.CONSUME;
        }
        if (keyTier != crate.rarity) {
            serverPlayer.sendSystemMessage((Component)Component.literal("§cEsa llave es de tier " + String.valueOf(keyTier.color()) + keyTier.displayName() + "§c. Esta crate necesita una llave " + String.valueOf(crate.rarity.color()) + crate.rarity.displayName() + "§c."));
            return InteractionResult.CONSUME;
        }
        final boolean skip = crate.allowSkip && player.isShiftKeyDown();
        CrateOpeningService.open(serverPlayer, crate, pos, key, skip);
        return InteractionResult.CONSUME;
    }
    
    static {
        FACING = HorizontalDirectionalBlock.FACING;
        SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
    }
}
