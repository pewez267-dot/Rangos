package com.fscrates.crate;

import com.fscrates.animation.AnimationRegistry;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.RewardEntry;
import com.fscrates.item.CrateItems;
import com.fscrates.config.Rarity;
import com.fscrates.network.FSNetwork;
import com.fscrates.network.PlayAnimationPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Orchestrates the open flow on the server. The KEY POINT: the reward is rolled
 * now, but the roulette's WINNING INDEX is sent explicitly so the reel lands on
 * exactly the reward that will be given, and delivery is scheduled for when the
 * animation finishes (so you receive the item the roulette stops on — no more
 * "shows netherite, gives diamond").
 */
public final class CrateOpeningService {

    private CrateOpeningService() {}

    private static final Random RANDOM = new Random();

    public enum Result { OK, ON_COOLDOWN, NO_PERMISSION, EMPTY }

    public static Result open(ServerPlayer player, CrateConfig crate, BlockPos pos,
                              ItemStack keyStack, boolean skipAnimation) {
        CooldownData cooldowns = CooldownData.get(player.serverLevel());

        if (crate.requiredPermission != null && !crate.requiredPermission.isBlank()
                && !player.hasPermissions(4)) {
            player.sendSystemMessage(Component.literal("\u00A7cNo tienes permiso para abrir esta crate."));
            return Result.NO_PERMISSION;
        }

        long remaining = cooldowns.remainingSeconds(player.getUUID(), crate.id);
        if (remaining > 0) {
            player.sendSystemMessage(Component.literal("\u00A7cDebes esperar \u00A7e" + remaining
                    + "s\u00A7c antes de abrir esta crate de nuevo."));
            return Result.ON_COOLDOWN;
        }

        if (crate.rewards.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00A7cEsta crate no tiene recompensas configuradas."));
            return Result.EMPTY;
        }

        if (crate.consumeKey && keyStack != null && !keyStack.isEmpty()) {
            keyStack.shrink(1);
        }

        // 1. Roll the actual rewards.
        List<RewardEntry> rolled = LootEngine.roll(crate, RANDOM);

        // 2. The reward to showcase = the rolled, non-guaranteed pick (the "spin"
        //    result). Fall back to any rolled reward.
        RewardEntry headline = null;
        for (RewardEntry r : rolled) {
            if (!r.guaranteed) {
                headline = r; // last non-guaranteed wins the showcase
            }
        }
        if (headline == null && !rolled.isEmpty()) {
            headline = rolled.get(rolled.size() - 1);
        }
        if (headline == null) {
            headline = crate.rewards.get(0);
        }

        // 3. Build the visual pool (icons) and find the winner index inside it.
        List<ItemStack> pool = new ArrayList<>();
        for (RewardEntry r : crate.rewards) {
            if (pool.size() >= 24) {
                break;
            }
            pool.add(iconFor(r));
        }
        ItemStack winnerIcon = iconFor(headline);
        int winnerIndex = crate.rewards.indexOf(headline);
        if (winnerIndex < 0 || winnerIndex >= pool.size()) {
            // headline not directly in the pool list: append it
            pool.add(winnerIcon);
            winnerIndex = pool.size() - 1;
        }
        if (pool.isEmpty()) {
            pool.add(winnerIcon.isEmpty() ? new ItemStack(Items.PAPER) : winnerIcon);
            winnerIndex = 0;
        }

        // 4. Tell nearby clients to play the animation, landing on winnerIndex.
        String animId = skipAnimation && crate.allowSkip ? "instant" : crate.animationId;
        PlayAnimationPacket packet = new PlayAnimationPacket(
                pos, animId, crate.rarity.rgb(), winnerIndex, candidatesNbt(pool));
        FSNetwork.sendToNear(player.serverLevel(), pos, 48, packet);

        // 5. Deliver when the reel stops (≈ reveal end). Instant = almost now.
        int total = AnimationRegistry.get(animId).durationTicks();
        int delay = animId.equals("instant") ? 2 : Math.max(2, Math.round(total * 0.90f));
        DelayedDelivery.schedule(player, crate, rolled, delay);

        cooldowns.startCooldown(player.getUUID(), crate.id, crate.cooldownSeconds);
        return Result.OK;
    }

    /** A representative display icon for any reward type. */
    private static ItemStack iconFor(RewardEntry r) {
        if (r == null) {
            return new ItemStack(Items.PAPER);
        }
        return switch (r.type) {
            case ITEM -> (r.item != null && !r.item.isEmpty()) ? r.item.copy() : new ItemStack(Items.PAPER);
            case KEY -> CrateItems.buildKey(Rarity.byName(r.keyRarity));
            case XP -> new ItemStack(Items.EXPERIENCE_BOTTLE);
            case EFFECT -> new ItemStack(Items.POTION);
            case COMMAND -> new ItemStack(Items.COMMAND_BLOCK);
        };
    }

    private static CompoundTag candidatesNbt(List<ItemStack> pool) {
        CompoundTag wrap = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack s : pool) {
            CompoundTag t = new CompoundTag();
            (s == null || s.isEmpty() ? new ItemStack(Items.PAPER) : s).save(t);
            list.add(t);
        }
        wrap.put("items", list);
        return wrap;
    }
}
