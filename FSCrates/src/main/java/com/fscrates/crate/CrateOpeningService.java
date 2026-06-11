package com.fscrates.crate;

import com.fscrates.config.CrateConfig;
import com.fscrates.config.RewardEntry;
import com.fscrates.network.FSNetwork;
import com.fscrates.network.PlayAnimationPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Random;

/**
 * Orchestrates the full open flow on the server:
 * validate cooldown/permission -> consume key -> roll loot -> tell nearby
 * clients to play the in-world animation on the crate -> deliver rewards ->
 * start the per-player cooldown.
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

        List<RewardEntry> rolled = LootEngine.roll(crate, RANDOM);
        RewardEntry headline = rolled.isEmpty() ? crate.rewards.get(0) : rolled.get(rolled.size() - 1);

        String animId = skipAnimation && crate.allowSkip ? "instant" : crate.animationId;
        PlayAnimationPacket packet = new PlayAnimationPacket(
                pos, animId, crate.rarity.rgb(),
                rewardItemNbt(headline), candidatesNbt(crate));
        // Everyone nearby sees the crate animation.
        FSNetwork.sendToNear(player.serverLevel(), pos, 48, packet);

        LootEngine.deliver(player, crate, rolled);
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
