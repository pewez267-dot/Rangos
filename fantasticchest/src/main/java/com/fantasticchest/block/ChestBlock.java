package com.fantasticchest.block;

import com.fantasticchest.data.ChestDefinition;
import com.fantasticchest.data.ChestRegistry;
import com.fantasticchest.gui.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Fantastic Chest block. Holds a {@link ChestBlockEntity} (pure state, no tick).
 *
 * <p>Right-clicking always opens the <em>terminal</em> (Interface 2), even for OPs —
 * editing is only reachable with the editor wand (Interface 1). Placement and removal
 * keep the in-memory {@link ChestRegistry} in sync.</p>
 */
public final class ChestBlock extends BaseEntityBlock {

    public ChestBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ChestBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos,
                                 final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        final BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ChestBlockEntity chest)) {
            return InteractionResult.PASS;
        }
        // Right-click is ALWAYS the terminal (Interface 2), even for OPs.
        if (!chest.canAccess(serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(Component.literal("§cNo tienes permiso para abrir este cofre."));
            return InteractionResult.CONSUME;
        }
        ModMenus.openTerminal(serverPlayer, chest);
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
                            @Nullable final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(level instanceof ServerLevel)) {
            return;
        }
        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ChestBlockEntity chest) {
            if (placer instanceof ServerPlayer sp) {
                chest.ensureOwner(sp.getUUID());
            }
            chest.onPlaced();
        }
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos,
                         final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            final BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity chest && level instanceof ServerLevel && !chest.getChestId().isBlank()) {
                final ChestDefinition def = ChestRegistry.get().get(chest.getChestId());
                if (def != null) {
                    def.placed = false;
                    ChestRegistry.get().put(def);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
