package com.fscrates.crate;

import com.fscrates.config.CrateConfig;
import com.fscrates.config.RewardEntry;
import com.fscrates.network.FSNetwork;
import com.fscrates.network.PlayAnimationPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Random;

/**
 * Orchestrates the full open flow on the server:
 * validate key/cooldown -> consume key -> roll loot -> send animation ->
 * deliver rewards -> start per-player cooldown.
 */
public final class CrateOpeningService {

    private CrateOpeningService() {}

    private static final Random RANDOM = new Random();

    public enum Result { OK, ON_COOLDOWN, NO_PERMISSION, EMPTY }

    public static Result open(ServerPlayer player, CrateConfig crate, ItemStack keyStack, boolean skipAnimation) {
        CooldownData cooldowns = CooldownData.get(player.serverLevel());

        // optional permission node
        if (crate.requiredPermission != null && !crate.requiredPermission.isBlank()
                && !player.hasPermissions(4)) {
            player.sendSystemMessage(Component.literal("\u00A7cNo tienes permiso para abrir esta crate."));
            return Result.NO_PERMISSION;
        }

        // per-player cooldown
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

        // consume the key
        if (crate.consumeKey && keyStack != null && !keyStack.isEmpty()) {
            keyStack.shrink(1);
        }

        // roll loot on the server (authoritative)
        List<RewardEntry> rolled = LootEngine.roll(crate, RANDOM);

        // pick a "headline" reward for the animation reveal (last rolled)
        RewardEntry headline = rolled.isEmpty() ? crate.rewards.get(0) : rolled.get(rolled.size() - 1);

        // send animation to the opener
        String animId = skipAnimation && crate.allowSkip ? "instant" : crate.animationId;
        FSNetwork.sendToClient(player, new PlayAnimationPacket(
                animId,
                crate.rarity.rgb(),
                rewardItemNbt(headline),
                candidatesNbt(crate),
                crate.allowSkip));

        // deliver rewards
        LootEngine.deliver(player, crate, rolled);

        // start cooldown for THIS player only
        cooldowns.startCooldown(player.getUUID(), crate.id, crate.cooldownSeconds);
        return Result.OK;
    }

    private static CompoundTag rewardItemNbt(RewardEntry headline) {
        CompoundTag tag = new CompoundTag();
        ItemStack icon = switch (headline.type) {
            case ITEM, KEY -> headline.item;
            default -> ItemStack.EMPTY;
        };
        if (icon != null && !icon.isEmpty()) {
            icon.save(tag);
        }
        tag.putString("label", headline.describe());
        return tag;
    }

    /** A small set of candidate item icons for the spinning reel visuals. */
    private static CompoundTag candidatesNbt(CrateConfig crate) {
        CompoundTag wrap = new CompoundTag();
        ListTag list = new ListTag();
        int count = 0;
        for (RewardEntry r : crate.rewards) {
            if (count >= 16) {
                break;
            }
            if (r.type == RewardEntry.Type.ITEM && r.item != null && !r.item.isEmpty()) {
                CompoundTag t = new CompoundTag();
                r.item.save(t);
                list.add(t);
                count++;
            }
        }
        wrap.put("items", list);
        return wrap;
    }
}
