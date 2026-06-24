// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.crate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import com.fscrates.item.CrateItems;
import com.fscrates.config.Rarity;
import net.minecraft.server.level.ServerPlayer;
import java.util.Iterator;
import java.util.ArrayList;
import com.fscrates.config.RewardEntry;
import java.util.List;
import java.util.Random;
import com.fscrates.config.CrateConfig;

public final class LootEngine
{
    private LootEngine() {
    }
    
    public static List<RewardEntry> roll(final CrateConfig crate, final Random random) {
        final List<RewardEntry> result = new ArrayList<RewardEntry>();
        for (final RewardEntry r : crate.rewards) {
            if (r.guaranteed) {
                result.add(r);
            }
        }
        final double total = crate.totalChance();
        if (total > 0.0) {
            for (int i = 0; i < Math.max(1, crate.rolls); ++i) {
                final double pick = random.nextDouble() * total;
                double cursor = 0.0;
                for (final RewardEntry r2 : crate.rewards) {
                    if (r2.guaranteed) {
                        continue;
                    }
                    cursor += Math.max(0.0, r2.chance);
                    if (pick < cursor) {
                        result.add(r2);
                        break;
                    }
                }
            }
        }
        return result;
    }
    
    public static void deliver(final ServerPlayer player, final CrateConfig crate, final List<RewardEntry> rolled) {
        final ServerLevel level = player.serverLevel();
        final Random random = new Random();
        for (final RewardEntry r : rolled) {
            int amount = r.minAmount + ((r.maxAmount > r.minAmount) ? random.nextInt(r.maxAmount - r.minAmount + 1) : 0);
            amount = Math.max(1, amount);
            switch (r.type) {
                case ITEM: {
                    giveItem(player, r.item, amount);
                    continue;
                }
                case KEY: {
                    giveItem(player, CrateItems.buildKey(Rarity.byName(r.keyRarity)), amount);
                    continue;
                }
                case XP: {
                    player.giveExperiencePoints(r.xp * amount);
                    continue;
                }
                case EFFECT: {
                    applyEffect(player, r);
                    continue;
                }
                case COMMAND: {
                    continue;
                }
                default: {
                    continue;
                }
            }
        }
        if (crate.broadcast && level.getServer() != null) {
            final String rewards = rolled.isEmpty() ? "nada" : rolled.get(rolled.size() - 1).describe();
            level.getServer().getPlayerList().broadcastSystemMessage((Component)Component.literal("§d[Crates] §f" + player.getName().getString() + " abri\u00f3 " + crate.displayName + "§f y obtuvo §e" + rewards), false);
        }
    }
    
    private static void giveItem(final ServerPlayer player, final ItemStack template, final int amount) {
        if (template == null || template.isEmpty()) {
            return;
        }
        int remaining = amount;
        final int max = template.getMaxStackSize();
        while (remaining > 0) {
            final int take = Math.min(remaining, max);
            remaining -= take;
            final ItemStack stack = template.copy();
            stack.setCount(take);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }
    
    private static void applyEffect(final ServerPlayer player, final RewardEntry r) {
        final MobEffect effect = (MobEffect)ForgeRegistries.MOB_EFFECTS.getValue(safe(r.effectId));
        if (effect != null) {
            player.addEffect(new MobEffectInstance(effect, Math.max(1, r.effectDuration), Math.max(0, r.effectAmplifier)));
        }
    }
    
    private static ResourceLocation safe(final String id) {
        final ResourceLocation rl = ResourceLocation.tryParse((id == null) ? "" : id);
        return (rl == null) ? new ResourceLocation("minecraft", "luck") : rl;
    }
}
