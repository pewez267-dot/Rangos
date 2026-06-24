package com.fantasticaudit.events;

import com.fantasticaudit.FantasticAudit;
import com.fantasticaudit.config.AuditConfig;
import com.fantasticaudit.logging.AuditLogger;
import com.fantasticaudit.logging.BlockSummary;
import com.fantasticaudit.util.ItemSerializer;
import com.fantasticaudit.util.NbtSerializer;
import com.fantasticaudit.util.RecentBreaks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Captures the BLOCKS category: break, place and right-click interaction.
 *
 * <p>All handlers run server-side only and resolve every id through the registries, so blocks,
 * tools and drops from any installed mod are recorded with their full namespaced ids.</p>
 */
@Mod.EventBusSubscriber(modid = FantasticAudit.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlockEventHandler {

    private BlockEventHandler() {
    }

    /**
     * Logged at LOWEST priority so any higher-priority cancellation (protection mods, claims)
     * has already happened; we only record breaks that are actually going to succeed.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!AuditConfig.LOG_BLOCKS.get() || event.isCanceled()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        logBlockBreak(player, serverLevel, event.getPos(), event.getState(), player.getMainHandItem());
    }

    /**
     * Records a single successful block break. Shared by the Forge {@code BlockEvent.BreakEvent}
     * handler and the optional Architectury hook ({@link ArchitecturyAuditHook}) so area tools like
     * JustHammers (which break the extra blocks through Architectury) are captured too. A per-tick
     * de-duplication guard ensures the directly-hit block — which both paths can observe — is logged
     * exactly once.
     */
    public static void logBlockBreak(ServerPlayer player, ServerLevel serverLevel, BlockPos pos,
                                     BlockState state, ItemStack tool) {
        if (!AuditConfig.LOG_BLOCKS.get()) {
            return;
        }
        String dim = ItemSerializer.dimShort(serverLevel);
        if (!RecentBreaks.get().claim(player.getUUID(), dim, pos, serverLevel.getGameTime())) {
            return; // already logged by the other break handler in this tick
        }

        BlockEntity blockEntity = serverLevel.getBlockEntity(pos);

        // Block.getDrops returns the exact loot the break would yield given this tool and state,
        // honouring any modded loot tables. This is the authoritative drop list for the event.
        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, blockEntity, player, tool);

        String toolId = ItemSerializer.itemId(tool);
        String toolNbt = NbtSerializer.serializeStackTag(tool);

        StringBuilder data = new StringBuilder()
                .append(ItemSerializer.blockId(state)).append(" x1")
                .append(" @(").append(ItemSerializer.pos(pos)).append(") ")
                .append(dim)
                .append(" tool=").append(toolId);
        // Only include tool NBT when there actually is some (skip the noisy empty "{}").
        if (!"{}".equals(toolNbt)) {
            data.append(" nbt=").append(toolNbt);
        }
        data.append(" drops=").append(ItemSerializer.describeDrops(drops));

        String playerName = player.getGameProfile().getName();
        AuditLogger.get().record(player.getUUID(), playerName, "BLOCK_BREAK", data.toString());

        // Feed the cumulative per-player mined-blocks summary (block id + total + tool used).
        if (AuditConfig.BLOCK_SUMMARY.get()) {
            BlockSummary.get().record(player.getUUID(), playerName, ItemSerializer.blockId(state), toolId, 1L);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!AuditConfig.LOG_BLOCKS.get() || event.isCanceled()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        BlockState placed = event.getPlacedBlock();
        BlockPos pos = event.getPos();
        // The placed block consumes an item from a hand; main hand is the common case. We log the
        // current main-hand item as the used item (offhand placements are rare and indistinguishable
        // from this event alone).
        ItemStack used = player.getMainHandItem();

        String data = ItemSerializer.blockId(placed)
                + " @(" + ItemSerializer.pos(pos) + ") "
                + ItemSerializer.dimShort(player.level())
                + " item=" + ItemSerializer.itemId(used);

        AuditLogger.get().record(player.getUUID(), player.getGameProfile().getName(), "BLOCK_PLACE", data);
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        // Only main hand to avoid double-logging the off-hand pass of the same interaction.
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = player.level().getBlockState(pos);
        ItemStack inHand = event.getItemStack();
        String blockId = ItemSerializer.blockId(state);
        String dim = ItemSerializer.dimShort(player.level());

        // Remember the last block this player right-clicked so the container handler can attribute
        // a freshly opened container to the correct world position and block type.
        ContainerEventHandler.rememberInteractedBlock(player.getUUID(), pos, blockId, dim);

        if (!AuditConfig.LOG_BLOCKS.get()) {
            return;
        }

        String data = blockId
                + " @(" + ItemSerializer.pos(pos) + ") " + dim
                + " hand=" + ItemSerializer.itemId(inHand);

        AuditLogger.get().record(player.getUUID(), player.getGameProfile().getName(), "BLOCK_INTERACT", data);
    }
}
