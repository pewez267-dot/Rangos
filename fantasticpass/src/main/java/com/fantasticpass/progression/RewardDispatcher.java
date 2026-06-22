package com.fantasticpass.progression;

import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.network.NametagSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side reward granting with full validation. All checks (tier unlocked, not
 * already claimed, premium status) happen here so a spoofed client packet cannot
 * grant anything the player has not earned.
 */
public final class RewardDispatcher {

    /** Outcome of a claim attempt, used by the caller to message the player. */
    public enum ClaimResult {
        SUCCESS,
        NO_ACTIVE_PASS,
        NOT_UNLOCKED,
        ALREADY_CLAIMED,
        INVENTORY_FULL,
        INVALID_TIER
    }

    private RewardDispatcher() {
    }

    public static ClaimResult claim(ServerPlayer player, int tierNumber) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return ClaimResult.NO_ACTIVE_PASS;
        }

        PassSavedData saved = PassSavedData.get(server);
        PassDefinition pass = saved.getActivePass();
        if (pass == null) {
            return ClaimResult.NO_ACTIVE_PASS;
        }

        PlayerPassData data = PassCapability.getData(player);
        if (data == null) {
            return ClaimResult.NO_ACTIVE_PASS;
        }

        if (tierNumber < 1 || tierNumber > PassDefinition.TIER_COUNT) {
            return ClaimResult.INVALID_TIER;
        }
        if (tierNumber > data.getCurrentTier()) {
            return ClaimResult.NOT_UNLOCKED;
        }
        if (data.isTierClaimed(tierNumber)) {
            return ClaimResult.ALREADY_CLAIMED;
        }

        TierDefinition tier = pass.getTier(tierNumber);
        if (tier == null) {
            return ClaimResult.INVALID_TIER;
        }

        boolean premium = data.isPremium();

        // Collect every item that must be granted, free plus (optionally) premium.
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : tier.getFreeRewards()) {
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        if (premium) {
            for (ItemStack stack : tier.getPremiumRewards()) {
                if (!stack.isEmpty()) {
                    items.add(stack.copy());
                }
            }
        }

        // Atomic inventory check: if not everything fits, grant nothing and leave the
        // tier unclaimed so the player can try again after freeing space.
        if (!items.isEmpty() && !canFit(player, items)) {
            return ClaimResult.INVENTORY_FULL;
        }

        // Commit items.
        for (ItemStack stack : items) {
            ItemStack toAdd = stack.copy();
            boolean added = player.getInventory().add(toAdd);
            if (!added || !toAdd.isEmpty()) {
                // Defensive: should not happen because canFit verified capacity.
                player.drop(toAdd, false);
            }
        }

        // Run commands as the server console, substituting {player}.
        runCommands(server, player, tier.getFreeCommands());
        if (premium) {
            runCommands(server, player, tier.getPremiumCommands());
        }

        // Pass-rank reward (history persists across seasons).
        if (tier.hasRankReward()) {
            data.addEarnedRank(tier.getRankReward());
        }

        data.markClaimed(tierNumber);

        // Refresh the nametag in case progress display changed.
        NametagSync.syncPlayer(player);

        return ClaimResult.SUCCESS;
    }

    private static void runCommands(MinecraftServer server, ServerPlayer player, List<String> commands) {
        if (commands.isEmpty()) {
            return;
        }
        String playerName = player.getGameProfile().getName();
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        for (String raw : commands) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String command = raw.replace("{player}", playerName).trim();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            server.getCommands().performPrefixedCommand(source, command);
        }
    }

    /**
     * Simulates inserting {@code rewards} into the player's 36 main inventory slots,
     * respecting stacking, to determine whether all items fit without dropping.
     */
    private static boolean canFit(ServerPlayer player, List<ItemStack> rewards) {
        NonNullList<ItemStack> source = player.getInventory().items;
        int maxStack = player.getInventory().getMaxStackSize();

        ItemStack[] sim = new ItemStack[source.size()];
        for (int i = 0; i < source.size(); i++) {
            sim[i] = source.get(i).copy();
        }

        for (ItemStack reward : rewards) {
            ItemStack remaining = reward.copy();

            // Merge into existing matching stacks first.
            for (int i = 0; i < sim.length && !remaining.isEmpty(); i++) {
                ItemStack slot = sim[i];
                if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, remaining)) {
                    int cap = Math.min(maxStack, slot.getMaxStackSize());
                    int space = cap - slot.getCount();
                    if (space > 0) {
                        int move = Math.min(space, remaining.getCount());
                        slot.grow(move);
                        remaining.shrink(move);
                    }
                }
            }

            // Then place into empty slots.
            for (int i = 0; i < sim.length && !remaining.isEmpty(); i++) {
                if (sim[i].isEmpty()) {
                    int cap = Math.min(maxStack, remaining.getMaxStackSize());
                    int move = Math.min(cap, remaining.getCount());
                    ItemStack placed = remaining.copy();
                    placed.setCount(move);
                    sim[i] = placed;
                    remaining.shrink(move);
                }
            }

            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Human-readable system message component for a claim result. */
    public static Component messageFor(ClaimResult result, int tier) {
        return switch (result) {
            case SUCCESS -> Component.translatable("fantasticpass.msg.claimed", tier);
            case INVENTORY_FULL -> Component.translatable("fantasticpass.msg.inventory_full");
            case NOT_UNLOCKED -> Component.translatable("fantasticpass.msg.not_unlocked");
            case ALREADY_CLAIMED -> Component.translatable("fantasticpass.msg.already_claimed");
            case NO_ACTIVE_PASS, INVALID_TIER -> Component.translatable("fantasticpass.msg.no_active_pass");
        };
    }
}
