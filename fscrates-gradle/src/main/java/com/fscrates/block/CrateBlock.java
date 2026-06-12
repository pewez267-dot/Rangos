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
        super(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283825_).m_60913_(-1.0f, 3600000.0f).m_60918_(SoundType.f_56736_).m_60955_());
        this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_((Property)CrateBlock.FACING, (Comparable)Direction.NORTH));
    }
    
    protected void m_7926_(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[] { (Property)CrateBlock.FACING });
    }
    
    @Nullable
    public BlockState m_5573_(final BlockPlaceContext ctx) {
        return (BlockState)this.m_49966_().m_61124_((Property)CrateBlock.FACING, (Comparable)ctx.m_8125_().m_122424_());
    }
    
    public BlockEntity m_142194_(final BlockPos pos, final BlockState state) {
        return new CrateBlockEntity(pos, state);
    }
    
    public RenderShape m_7514_(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    
    public VoxelShape m_5940_(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext ctx) {
        return CrateBlock.SHAPE;
    }
    
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (!level.f_46443_ || type != ModRegistry.CRATE_BE.get()) {
            return null;
        }
        return (BlockEntityTicker<T>)((lvl, pos, st, be) -> CrateBlockEntity.clientTick(lvl, pos, st, (CrateBlockEntity)be));
    }
    
    public void m_6402_(final Level level, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer, final ItemStack stack) {
        super.m_6402_(level, pos, state, placer, stack);
        if (level.f_46443_) {
            return;
        }
        final CrateConfig cfg = CrateItems.readConfig(stack);
        if (cfg != null) {
            final BlockEntity blockEntity = level.m_7702_(pos);
            if (blockEntity instanceof final CrateBlockEntity be) {
                be.setConfig(cfg);
            }
        }
    }
    
    public InteractionResult m_6227_(final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.f_46443_) {
            return InteractionResult.SUCCESS;
        }
        final BlockEntity blockEntity = level.m_7702_(pos);
        if (!(blockEntity instanceof CrateBlockEntity)) {
            return InteractionResult.PASS;
        }
        final CrateBlockEntity be = (CrateBlockEntity)blockEntity;
        if (!(player instanceof ServerPlayer)) {
            return InteractionResult.PASS;
        }
        final ServerPlayer serverPlayer = (ServerPlayer)player;
        final CrateConfig crate = be.getConfig();
        final ItemStack key = player.m_21205_();
        final Rarity keyTier = CrateItems.keyRarity(key);
        if (keyTier == null) {
            serverPlayer.m_213846_((Component)Component.m_237113_("§eNecesitas una §fllave " + String.valueOf(crate.rarity.color()) + crate.rarity.displayName() + "§e en la mano para abrir esta crate."));
            return InteractionResult.CONSUME;
        }
        if (keyTier != crate.rarity) {
            serverPlayer.m_213846_((Component)Component.m_237113_("§cEsa llave es de tier " + String.valueOf(keyTier.color()) + keyTier.displayName() + "§c. Esta crate necesita una llave " + String.valueOf(crate.rarity.color()) + crate.rarity.displayName() + "§c."));
            return InteractionResult.CONSUME;
        }
        final boolean skip = crate.allowSkip && player.m_6144_();
        CrateOpeningService.open(serverPlayer, crate, pos, key, skip);
        return InteractionResult.CONSUME;
    }
    
    static {
        FACING = HorizontalDirectionalBlock.f_54117_;
        SHAPE = Block.m_49796_(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
    }
}
