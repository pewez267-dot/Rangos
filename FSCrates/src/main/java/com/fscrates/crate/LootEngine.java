package com.fscrates.crate;

import com.fscrates.config.CrateConfig;
import com.fscrates.config.RewardEntry;
import com.fscrates.item.CrateItems;
import com.fscrates.config.Rarity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side loot resolution and reward delivery. Pure RNG by weight, plus
 * guaranteed rewards that always drop. Kept separate from GUI and animation so
 * it can be unit-reasoned and reused (e.g. by {@code /fscrate preview}).
 */
public final class LootEngine {

    private LootEngine() {}

    /** Rolls the rewards for one crate opening (guaranteed + weighted rolls). */
    public static List<RewardEntry> roll(CrateConfig crate, java.util.Random random) {
        List<RewardEntry> result = new ArrayList<>();

        // 1. guaranteed rewards always included
        for (RewardEntry r : crate.rewards) {
            if (r.guaranteed) {
                result.add(r);
            }
        }

        // 2. weighted rolls
        int totalWeight = crate.totalWeight();
        if (totalWeight > 0) {
            for (int i = 0; i < Math.max(1, crate.rolls); i++) {
                int pick = random.nextInt(totalWeight);
                int cursor = 0;
                for (RewardEntry r : crate.rewards) {
                    if (r.guaranteed) {
                        continue;
                    }
                    cursor += Math.max(0, r.weight);
                    if (pick < cursor) {
                        result.add(r);
                        break;
                    }
                }
            }
        }
        return result;
    }

    /** Delivers every rolled reward to the player on the server. */
    public static void deliver(ServerPlayer player, CrateConfig crate, List<RewardEntry> rolled) {
        ServerLevel level = player.serverLevel();
        java.util.Random random = new java.util.Random();

        for (RewardEntry r : rolled) {
            int amount = r.minAmount + (r.maxAmount > r.minAmount
                    ? random.nextInt(r.maxAmount - r.minAmount + 1) : 0);
            amount = Math.max(1, amount);

            switch (r.type) {
                case ITEM -> giveItem(player, r.item, amount);
                case KEY -> giveItem(player, CrateItems.buildKey(crate, Rarity.byName(r.keyRarity)), amount);
                case XP -> player.giveExperiencePoints(r.xp * amount);
                case EFFECT -> applyEffect(player, r);
                case COMMAND -> runCommand(player, r.command, amount);
            }
        }

        if (crate.broadcast && level.getServer() != null) {
            String rewards = rolled.isEmpty() ? "nada" : rolled.get(rolled.size() - 1).describe();
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("\u00A7d[Crates] \u00A7f" + player.getName().getString()
                            + " abri\u00f3 " + crate.displayName + "\u00A7f y obtuvo \u00A7e" + rewards),
                    false);
        }
    }

    private static void giveItem(ServerPlayer player, ItemStack template, int amount) {
        if (template == null || template.isEmpty()) {
            return;
        }
        int remaining = amount;
        int max = template.getMaxStackSize();
        while (remaining > 0) {
            int take = Math.min(remaining, max);
            remaining -= take;
            ItemStack stack = template.copy();
            stack.setCount(take);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private static void applyEffect(ServerPlayer player, RewardEntry r) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(safe(r.effectId));
        if (effect != null) {
            player.addEffect(new MobEffectInstance(effect, Math.max(1, r.effectDuration),
                    Math.max(0, r.effectAmplifier)));
        }
    }

    private static void runCommand(ServerPlayer player, String command, int times) {
        if (command == null || command.isBlank() || player.getServer() == null) {
            return;
        }
        String cmd = command.replace("{player}", player.getName().getString());
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        CommandSourceStack source = player.getServer().createCommandSourceStack()
                .withPermission(4).withSuppressedOutput();
        for (int i = 0; i < Math.max(1, times); i++) {
            player.getServer().getCommands().performPrefixedCommand(source, cmd);
        }
    }

    private static ResourceLocation safe(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id);
        return rl == null ? new ResourceLocation("minecraft", "luck") : rl;
    }
}
