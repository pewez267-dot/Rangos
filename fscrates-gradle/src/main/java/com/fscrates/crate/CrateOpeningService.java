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
        final CooldownData cooldowns = CooldownData.get(player.serverLevel());
        if (crate.requiredPermission != null && !crate.requiredPermission.isBlank() && !player.hasPermissions(4)) {
            player.sendSystemMessage((Component)Component.literal("§cNo tienes permiso para abrir esta crate."));
            return Result.NO_PERMISSION;
        }
        final long remaining = cooldowns.remainingSeconds(player.getUUID(), crate.id);
        if (remaining > 0L) {
            player.sendSystemMessage((Component)Component.literal("§cDebes esperar §e" + remaining + "s§c antes de abrir esta crate de nuevo."));
            return Result.ON_COOLDOWN;
        }
        if (crate.rewards.isEmpty()) {
            player.sendSystemMessage((Component)Component.literal("§cEsta crate no tiene recompensas configuradas."));
            return Result.EMPTY;
        }
        if (crate.consumeKey && keyStack != null && !keyStack.isEmpty()) {
            keyStack.shrink(1);
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
            pool.add(winnerIcon.isEmpty() ? new ItemStack((ItemLike)Items.PAPER) : winnerIcon);
            winnerIndex = 0;
        }
        final String animId = (skipAnimation && crate.allowSkip) ? "instant" : crate.animationId;
        // Rareza EFECTIVA del premio ganador: la del item (si se le asigno una en
        // el pool) o, si esta vacia, la de la crate. Define color de luz, sonido
        // y particulas en el cliente.
        final Rarity effectRarity = headline.effectiveRarity(crate.rarity);
        final PlayAnimationPacket packet = new PlayAnimationPacket(pos, animId, effectRarity.rgb(), winnerIndex, effectRarity.ordinal(), candidatesNbt(pool));
        FSNetwork.sendToNear(player.serverLevel(), pos, 48.0, packet);
        final int total = AnimationRegistry.get(animId).durationTicks();
        // La ruleta deja de girar y para en el premio en P_REVEAL_END (88% de la
        // animacion); ahi tambien suena el impacto de victoria. Entregamos la
        // recompensa EXACTAMENTE en ese instante para que ruleta + sonido +
        // entrega esten sincronizados. (Antes era 100%, lo que dejaba un hueco
        // y hacia que el sonido del bloque musical sonara tras parar la ruleta.)
        final int delay = animId.equals("instant")
                ? 2
                : Math.max(2, Math.round(total * com.fscrates.block.CrateBlockEntity.P_REVEAL_END));
        DelayedDelivery.schedule(player, crate, rolled, delay);
        cooldowns.startCooldown(player.getUUID(), crate.id, crate.cooldownSeconds);
        return Result.OK;
    }
    
    private static ItemStack iconFor(final RewardEntry r) {
        if (r == null) {
            return new ItemStack((ItemLike)Items.PAPER);
        }
        return switch (r.type) {
            default -> throw new IncompatibleClassChangeError();
            case ITEM -> (r.item != null && !r.item.isEmpty()) ? r.item.copy() : new ItemStack((ItemLike)Items.PAPER);
            case KEY -> CrateItems.buildKey(Rarity.byName(r.keyRarity));
            case XP -> new ItemStack((ItemLike)Items.EXPERIENCE_BOTTLE);
            case EFFECT -> new ItemStack((ItemLike)Items.POTION);
            case COMMAND -> new ItemStack((ItemLike)Items.COMMAND_BLOCK);
        };
    }
    
    private static CompoundTag candidatesNbt(final List<ItemStack> pool) {
        final CompoundTag wrap = new CompoundTag();
        final ListTag list = new ListTag();
        for (final ItemStack s : pool) {
            final CompoundTag t = new CompoundTag();
            ((s == null || s.isEmpty()) ? new ItemStack((ItemLike)Items.PAPER) : s).save(t);
            list.add(t);
        }
        wrap.put("items", (Tag)list);
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
