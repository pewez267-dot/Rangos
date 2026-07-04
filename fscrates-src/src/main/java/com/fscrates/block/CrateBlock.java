package com.fscrates.block;

import com.fscrates.block.CrateBlockEntity;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.crate.CrateOpeningService;
import com.fscrates.item.CrateItems;
import com.fscrates.network.FSNetwork;
import com.fscrates.network.OpenEditorPacket;
import com.fscrates.registry.ModRegistry;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CrateBlock
extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box((double)1.0, (double)0.0, (double)1.0, (double)15.0, (double)14.0, (double)15.0);

    public CrateBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0f, 3600000.0f).sound(SoundType.WOOD).noOcclusion());
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)FACING, (Comparable)Direction.NORTH));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)ctx.getHorizontalDirection().getOpposite());
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(pos, state);
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide && type == ModRegistry.CRATE_BE.get() ? (lvl, pos, st, be) -> CrateBlockEntity.clientTick(lvl, pos, st, (CrateBlockEntity)be) : null;
    }

    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockEntity blockEntity;
        CrateConfig cfg;
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && (cfg = CrateItems.readConfig(stack)) != null && (blockEntity = level.getBlockEntity(pos)) instanceof CrateBlockEntity) {
            CrateBlockEntity be = (CrateBlockEntity)blockEntity;
            be.setConfig(cfg);
        }
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CrateBlockEntity)) {
            return InteractionResult.PASS;
        }
        CrateBlockEntity be = (CrateBlockEntity)blockEntity;
        if (!(player instanceof ServerPlayer)) {
            return InteractionResult.PASS;
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        CrateConfig crate = be.getConfig();
        ItemStack mainHand = player.getMainHandItem();
        if (CrateItems.isEditorWand(mainHand)) {
            if (!serverPlayer.hasPermissions(4)) {
                serverPlayer.sendSystemMessage((Component)Component.literal((String)"\u00a7cSolo administradores pueden usar la Varita del Editor."));
                return InteractionResult.CONSUME;
            }
            FSNetwork.sendToClient(serverPlayer, new OpenEditorPacket(crate.save(), pos));
            serverPlayer.sendSystemMessage((Component)Component.literal((String)("\u00a7dEditor abierto para el cofre \u00a7f" + crate.id + "\u00a7d. Guarda para aplicar los cambios aqu\u00ed.")));
            return InteractionResult.CONSUME;
        }
        ItemStack key = player.getMainHandItem();
        // Llave UNIVERSAL: cualquier Fantastic Key abre cualquier crate (ya no hay match
        // de tier). La rareza del premio la decide la tabla de rarezas de la crate al abrir.
        if (!CrateItems.isKey(key)) {
            serverPlayer.sendSystemMessage((Component)Component.literal((String)"\u00a7eNecesitas una \u00a7d\u2726 Fantastic Key \u2726\u00a7e en la mano para abrir esta crate."));
            return InteractionResult.CONSUME;
        }
        boolean skip = crate.allowSkip && player.isShiftKeyDown();
        CrateOpeningService.open(serverPlayer, crate, pos, key, skip);
        return InteractionResult.CONSUME;
    }
}

