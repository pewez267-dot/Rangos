package com.fscrates.crate;

import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.config.RewardEntry;
import com.fscrates.item.CrateItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class LootEngine {
    private LootEngine() {
    }

    public static List<RewardEntry> roll(CrateConfig crate, Random random) {
        ArrayList<RewardEntry> result = new ArrayList<RewardEntry>();
        for (RewardEntry r : crate.rewards) {
            if (!r.guaranteed) continue;
            result.add(r);
        }
        double total = crate.totalChance();
        if (total > 0.0) {
            block1: for (int i = 0; i < Math.max(1, crate.rolls); ++i) {
                double pick = random.nextDouble() * total;
                double cursor = 0.0;
                for (RewardEntry r2 : crate.rewards) {
                    if (r2.guaranteed || !(pick < (cursor += Math.max(0.0, r2.chance)))) continue;
                    result.add(r2);
                    continue block1;
                }
            }
        }
        return result;
    }

    public static void deliver(ServerPlayer player, CrateConfig crate, List<RewardEntry> rolled) {
        ServerLevel level = player.serverLevel();
        Random random = new Random();
        for (RewardEntry r : rolled) {
            int amount = r.minAmount + (r.maxAmount > r.minAmount ? random.nextInt(r.maxAmount - r.minAmount + 1) : 0);
            amount = Math.max(1, amount);
            switch (r.type) {
                case ITEM: {
                    LootEngine.giveItem(player, r.item, amount);
                    break;
                }
                case KEY: {
                    LootEngine.giveItem(player, CrateItems.buildKey(Rarity.byName(r.keyRarity)), amount);
                    break;
                }
                case XP: {
                    player.giveExperiencePoints(r.xp * amount);
                    break;
                }
                case EFFECT: {
                    LootEngine.applyEffect(player, r);
                }
            }
        }
        if (crate.broadcast && level.getServer() != null) {
            String rewards = rolled.isEmpty() ? "nada" : rolled.get(rolled.size() - 1).describe();
            level.getServer().getPlayerList().broadcastSystemMessage((Component)Component.literal((String)("\u00a7d[Crates] \u00a7f" + player.getName().getString() + " abri\u00f3 " + crate.displayName + "\u00a7f y obtuvo \u00a7e" + rewards)), false);
        }
    }

    private static void giveItem(ServerPlayer player, ItemStack template, int amount) {
        if (template != null && !template.isEmpty()) {
            int take;
            int max = template.getMaxStackSize();
            for (int remaining = amount; remaining > 0; remaining -= take) {
                take = Math.min(remaining, max);
                ItemStack stack = template.copy();
                stack.setCount(take);
                if (player.getInventory().add(stack)) continue;
                player.drop(stack, false);
            }
        }
    }

    private static void applyEffect(ServerPlayer player, RewardEntry r) {
        MobEffect effect = (MobEffect)ForgeRegistries.MOB_EFFECTS.getValue(LootEngine.safe(r.effectId));
        if (effect != null) {
            player.addEffect(new MobEffectInstance(effect, Math.max(1, r.effectDuration), Math.max(0, r.effectAmplifier)));
        }
    }

    private static ResourceLocation safe(String id) {
        ResourceLocation rl = ResourceLocation.tryParse((String)(id == null ? "" : id));
        return rl == null ? new ResourceLocation("minecraft", "luck") : rl;
    }
}

