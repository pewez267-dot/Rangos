package com.fantasticwatch.events;

import com.fantasticwatch.FantasticWatch;
import com.fantasticwatch.config.WatchConfig;
import com.fantasticwatch.tracking.ItemTracker;
import com.fantasticwatch.util.NbtUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tracks marked items as they move through the world: dropped, picked up (incl. ownership
 * transfer) and consumed/destroyed-by-use.
 *
 * <p>Non-operator interactions are only recorded for already-marked items, and only when
 * {@code log_non_op_interactions} is enabled — matching the rule that this mod observes normal
 * players solely when they touch an operator-spawned item.</p>
 */
@Mod.EventBusSubscriber(modid = FantasticWatch.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemTrackingHandler {

    private ItemTrackingHandler() {
    }

    private static boolean shouldLogForActor(ServerPlayer player) {
        return ItemTracker.isOp(player) || WatchConfig.LOG_NON_OP_INTERACTIONS.get();
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getEntity().getItem();
        ItemTracker tracker = ItemTracker.get();

        // A creative operator dropping an as-yet-unmarked item: this is the item leaving creative.
        if (player.isCreative() && ItemTracker.isOp(player) && !NbtUtil.isTracked(stack)) {
            tracker.markAndLogSpawn(player, stack, "drop");
        }

        if (!NbtUtil.isTracked(stack)) {
            return;
        }
        if (tracker.healStacking(stack)) {
            return;
        }
        if (tracker.stripIfExpired(stack)) {
            return;
        }
        if (shouldLogForActor(player)) {
            tracker.onDropped(stack, player);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItem().getItem();
        if (!NbtUtil.isTracked(stack)) {
            return;
        }
        ItemTracker tracker = ItemTracker.get();
        if (tracker.healStacking(stack)) {
            return;
        }
        if (tracker.stripIfExpired(stack)) {
            return;
        }
        if (shouldLogForActor(player)) {
            tracker.onPickedUp(stack, player);
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItem();
        if (!NbtUtil.isTracked(stack)) {
            return;
        }
        ItemTracker tracker = ItemTracker.get();
        if (tracker.stripIfExpired(stack)) {
            return;
        }
        if (shouldLogForActor(player)) {
            String method = stack.getItem().isEdible() ? "eaten" : "used";
            tracker.onConsumed(stack, player, method);
        }
    }
}
