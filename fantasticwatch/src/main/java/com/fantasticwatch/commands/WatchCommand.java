package com.fantasticwatch.commands;

import com.fantasticwatch.tracking.ItemTracker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Operator-only maintenance command for Fantastic Watch.
 *
 * <p>{@code /fantasticwatch heal} immediately strips the tracking mark from every loaded item that
 * should not carry one under the current {@code mark_mode} (i.e. stackable items left unstackable
 * by a previous version/mode), restoring their stacking without waiting for them to be encountered
 * passively. It sweeps all online players' inventories, ender chests, cursor and open container,
 * plus all loaded item entities, in every loaded level.</p>
 */
public final class WatchCommand {

    private WatchCommand() {
    }

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fantasticwatch")
                .requires(src -> src.hasPermission(4))
                .then(Commands.literal("heal").executes(WatchCommand::heal)));
    }

    private static int heal(final CommandContext<CommandSourceStack> ctx) {
        final MinecraftServer server = ctx.getSource().getServer();
        final ItemTracker tracker = ItemTracker.get();
        int healed = 0;

        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            healed += healPlayer(tracker, player);
        }
        for (final ServerLevel level : server.getAllLevels()) {
            for (final Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity item && tracker.healStacking(item.getItem())) {
                    healed++;
                }
            }
        }

        final int total = healed;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a[FantasticWatch] Reparados §e" + total + "§a stack(s); ya vuelven a apilarse."), true);
        return total;
    }

    private static int healPlayer(final ItemTracker tracker, final ServerPlayer player) {
        int n = 0;
        n += healContainer(tracker, player.getInventory());
        n += healContainer(tracker, player.getEnderChestInventory());

        final ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty() && tracker.healStacking(carried)) {
            n++;
        }
        // Slots of any container the player currently has open (e.g. a chest being viewed).
        for (final Slot slot : player.containerMenu.slots) {
            final ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && tracker.healStacking(stack)) {
                n++;
            }
        }

        // Re-sync so the client reflects the now-stackable items immediately.
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        return n;
    }

    private static int healContainer(final ItemTracker tracker, final Container container) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            final ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && tracker.healStacking(stack)) {
                n++;
            }
        }
        return n;
    }
}
