package com.fscrates.crate;

import com.fscrates.block.CrateBlockEntity;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.config.RewardEntry;
import com.fscrates.crate.CooldownData;
import com.fscrates.crate.DelayedDelivery;
import com.fscrates.crate.LootEngine;
import com.fscrates.item.CrateItems;
import com.fscrates.network.FSNetwork;
import com.fscrates.network.PlayAnimationPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CrateOpeningService {
    private static final Random RANDOM = new Random();

    private CrateOpeningService() {
    }

    public static Result open(ServerPlayer player, CrateConfig crate, BlockPos pos, ItemStack keyStack, boolean skipAnimation) {
        CrateBlockEntity b;
        CrateBlockEntity crateBe;
        CooldownData cooldowns = CooldownData.get(player.serverLevel());
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(pos);
        CrateBlockEntity crateBlockEntity = crateBe = blockEntity instanceof CrateBlockEntity ? (b = (CrateBlockEntity)blockEntity) : null;
        if (crate.requiredPermission != null && !crate.requiredPermission.isBlank() && !player.hasPermissions(4)) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7cNo tienes permiso para abrir esta crate."));
            return Result.NO_PERMISSION;
        }
        if (crate.openOncePerPlayer && crateBe != null && crateBe.hasOpenedBy(player.getUUID())) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7cYa abriste esta crate. Es de \u00a7eun solo uso por jugador\u00a7c."));
            return Result.ALREADY_OPENED;
        }
        long remaining = cooldowns.remainingSeconds(player.getUUID(), crate.id);
        if (remaining > 0L) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7cDebes esperar \u00a7e" + remaining + "s\u00a7c antes de abrir esta crate de nuevo.")));
            return Result.ON_COOLDOWN;
        }
        if (crate.rewards.isEmpty()) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7cEsta crate no tiene recompensas configuradas."));
            return Result.EMPTY;
        }
        if (crate.consumeKey && keyStack != null && !keyStack.isEmpty()) {
            keyStack.shrink(1);
        }
        List<RewardEntry> rolled = LootEngine.roll(crate, RANDOM);
        RewardEntry headline = null;
        for (RewardEntry r : rolled) {
            if (r.guaranteed) continue;
            headline = r;
        }
        if (headline == null && !rolled.isEmpty()) {
            headline = rolled.get(rolled.size() - 1);
        }
        if (headline == null) {
            headline = crate.rewards.get(0);
        }
        ArrayList<ItemStack> pool = new ArrayList<ItemStack>();
        ArrayList<Integer> poolRarities = new ArrayList<Integer>();
        for (RewardEntry r2 : crate.rewards) {
            if (pool.size() >= 24) break;
            pool.add(CrateOpeningService.iconFor(r2));
            poolRarities.add(r2.effectiveRarity(crate.rarity).ordinal());
        }
        ItemStack winnerIcon = CrateOpeningService.iconFor(headline);
        int winnerIndex = crate.rewards.indexOf(headline);
        if (winnerIndex < 0 || winnerIndex >= pool.size()) {
            pool.add(winnerIcon);
            poolRarities.add(headline.effectiveRarity(crate.rarity).ordinal());
            winnerIndex = pool.size() - 1;
        }
        if (pool.isEmpty()) {
            pool.add(winnerIcon.isEmpty() ? new ItemStack((ItemLike)Items.PAPER) : winnerIcon);
            poolRarities.add(crate.rarity.ordinal());
            winnerIndex = 0;
        }
        String animId = skipAnimation && crate.allowSkip ? "instant" : crate.animationId;
        Rarity effectRarity = headline.effectiveRarity(crate.rarity);
        PlayAnimationPacket packet = new PlayAnimationPacket(pos, animId, effectRarity.rgb(), winnerIndex, effectRarity.ordinal(), CrateOpeningService.candidatesNbt(pool, poolRarities), player.getUUID().getMostSignificantBits(), player.getUUID().getLeastSignificantBits());
        FSNetwork.sendToNear(player.serverLevel(), pos, 48.0, packet);
        int delay = animId.equals("instant") ? 4 : 254;
        DelayedDelivery.schedule(player, crate, rolled, delay);
        cooldowns.startCooldown(player.getUUID(), crate.id, crate.cooldownSeconds);
        if (crate.openOncePerPlayer && crateBe != null) {
            crateBe.markOpenedBy(player.getUUID());
        }
        return Result.OK;
    }

    private static ItemStack iconFor(RewardEntry r) {
        if (r == null) {
            return new ItemStack((ItemLike)Items.PAPER);
        }
        return switch (r.type) {
            default -> throw new IncompatibleClassChangeError();
            case ITEM -> {
                if (r.item != null && !r.item.isEmpty()) {
                    yield r.item.copy();
                }
                yield new ItemStack((ItemLike)Items.PAPER);
            }
            case KEY -> CrateItems.buildKey(Rarity.byName(r.keyRarity));
            case XP -> new ItemStack((ItemLike)Items.EXPERIENCE_BOTTLE);
            case EFFECT -> new ItemStack((ItemLike)Items.POTION);
            case COMMAND -> new ItemStack((ItemLike)Items.COMMAND_BLOCK);
        };
    }

    private static CompoundTag candidatesNbt(List<ItemStack> pool, List<Integer> rarities) {
        CompoundTag wrap = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack s : pool) {
            CompoundTag t = new CompoundTag();
            (s != null && !s.isEmpty() ? s : new ItemStack((ItemLike)Items.PAPER)).save(t);
            list.add(t);
        }
        wrap.put("items", (Tag)list);
        int[] rar = new int[rarities.size()];
        for (int i = 0; i < rar.length; ++i) {
            rar[i] = rarities.get(i);
        }
        wrap.putIntArray("rar", rar);
        return wrap;
    }

    public static enum Result {
        OK,
        ON_COOLDOWN,
        NO_PERMISSION,
        ALREADY_OPENED,
        EMPTY;

    }
}

