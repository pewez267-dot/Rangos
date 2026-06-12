// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.crate;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import com.fscrates.item.CrateItems;
import com.fscrates.config.Rarity;
import java.util.Iterator;
import java.util.List;
import com.fscrates.animation.AnimationRegistry;
import com.fscrates.network.FSNetwork;
import com.fscrates.network.PlayAnimationPacket;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import com.fscrates.config.RewardEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import com.fscrates.config.CrateConfig;
import net.minecraft.server.level.ServerPlayer;
import java.util.Random;

public final class CrateOpeningService
{
    private static final Random RANDOM;
    
    private CrateOpeningService() {
    }
    
    public static Result open(final ServerPlayer player, final CrateConfig crate, final BlockPos pos, final ItemStack keyStack, final boolean skipAnimation) {
        final CooldownData cooldowns = CooldownData.get(player.m_284548_());
        if (crate.requiredPermission != null && !crate.requiredPermission.isBlank() && !player.m_20310_(4)) {
            player.m_213846_((Component)Component.m_237113_("§cNo tienes permiso para abrir esta crate."));
            return Result.NO_PERMISSION;
        }
        final long remaining = cooldowns.remainingSeconds(player.m_20148_(), crate.id);
        if (remaining > 0L) {
            player.m_213846_((Component)Component.m_237113_("§cDebes esperar §e" + remaining + "s§c antes de abrir esta crate de nuevo."));
            return Result.ON_COOLDOWN;
        }
        if (crate.rewards.isEmpty()) {
            player.m_213846_((Component)Component.m_237113_("§cEsta crate no tiene recompensas configuradas."));
            return Result.EMPTY;
        }
        if (crate.consumeKey && keyStack != null && !keyStack.m_41619_()) {
            keyStack.m_41774_(1);
        }
        final List<RewardEntry> rolled = LootEngine.roll(crate, CrateOpeningService.RANDOM);
        RewardEntry headline = null;
        for (final RewardEntry r : rolled) {
            if (!r.guaranteed) {
                headline = r;
            }
        }
        if (headline == null && !rolled.isEmpty()) {
            headline = rolled.get(rolled.size() - 1);
        }
        if (headline == null) {
            headline = crate.rewards.get(0);
        }
        final List<ItemStack> pool = new ArrayList<ItemStack>();
        for (final RewardEntry r2 : crate.rewards) {
            if (pool.size() >= 24) {
                break;
            }
            pool.add(iconFor(r2));
        }
        final ItemStack winnerIcon = iconFor(headline);
        int winnerIndex = crate.rewards.indexOf(headline);
        if (winnerIndex < 0 || winnerIndex >= pool.size()) {
            pool.add(winnerIcon);
            winnerIndex = pool.size() - 1;
        }
        if (pool.isEmpty()) {
            pool.add(winnerIcon.m_41619_() ? new ItemStack((ItemLike)Items.f_42516_) : winnerIcon);
            winnerIndex = 0;
        }
        final String animId = (skipAnimation && crate.allowSkip) ? "instant" : crate.animationId;
        final PlayAnimationPacket packet = new PlayAnimationPacket(pos, animId, crate.rarity.rgb(), winnerIndex, candidatesNbt(pool));
        FSNetwork.sendToNear(player.m_284548_(), pos, 48.0, packet);
        final int total = AnimationRegistry.get(animId).durationTicks();
        final int delay = animId.equals("instant") ? 2 : Math.max(2, Math.round(total * 0.9f));
        DelayedDelivery.schedule(player, crate, rolled, delay);
        cooldowns.startCooldown(player.m_20148_(), crate.id, crate.cooldownSeconds);
        return Result.OK;
    }
    
    private static ItemStack iconFor(final RewardEntry r) {
        if (r == null) {
            return new ItemStack((ItemLike)Items.f_42516_);
        }
        return switch (r.type) {
            default -> throw new IncompatibleClassChangeError();
            case ITEM -> (r.item != null && !r.item.m_41619_()) ? r.item.m_41777_() : new ItemStack((ItemLike)Items.f_42516_);
            case KEY -> CrateItems.buildKey(Rarity.byName(r.keyRarity));
            case XP -> new ItemStack((ItemLike)Items.f_42612_);
            case EFFECT -> new ItemStack((ItemLike)Items.f_42589_);
            case COMMAND -> new ItemStack((ItemLike)Items.f_42116_);
        };
    }
    
    private static CompoundTag candidatesNbt(final List<ItemStack> pool) {
        final CompoundTag wrap = new CompoundTag();
        final ListTag list = new ListTag();
        for (final ItemStack s : pool) {
            final CompoundTag t = new CompoundTag();
            ((s == null || s.m_41619_()) ? new ItemStack((ItemLike)Items.f_42516_) : s).m_41739_(t);
            list.add((Object)t);
        }
        wrap.m_128365_("items", (Tag)list);
        return wrap;
    }
    
    static {
        RANDOM = new Random();
    }
    
    public enum Result
    {
        OK, 
        ON_COOLDOWN, 
        NO_PERMISSION, 
        EMPTY;
    }
}
