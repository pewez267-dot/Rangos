package com.fantasticpass.progression;

import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.network.NametagSync;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RewardDispatcher {
   private RewardDispatcher() {
   }

   public static RewardDispatcher.ClaimResult claim(ServerPlayer player, int tierNumber) {
      MinecraftServer server = player.getServer();
      if (server == null) {
         return RewardDispatcher.ClaimResult.NO_ACTIVE_PASS;
      } else {
         PassSavedData saved = PassSavedData.get(server);
         PassDefinition pass = saved.getActivePass();
         if (pass == null) {
            return RewardDispatcher.ClaimResult.NO_ACTIVE_PASS;
         } else {
            PlayerPassData data = PassCapability.getData(player);
            if (data == null) {
               return RewardDispatcher.ClaimResult.NO_ACTIVE_PASS;
            } else if (tierNumber >= 1 && tierNumber <= pass.getTierCount()) {
               if (tierNumber > data.getCurrentTier()) {
                  return RewardDispatcher.ClaimResult.NOT_UNLOCKED;
               } else if (!data.isTestMode() && data.isTierClaimed(tierNumber)) {
                  return RewardDispatcher.ClaimResult.ALREADY_CLAIMED;
               } else {
                  TierDefinition tier = pass.getTier(tierNumber);
                  if (tier == null) {
                     return RewardDispatcher.ClaimResult.INVALID_TIER;
                  } else {
                     boolean premium = data.isPremium();
                     List<ItemStack> items = new ArrayList<>();

                     for (ItemStack stack : tier.getFreeRewards()) {
                        if (!stack.isEmpty()) {
                           items.add(stack.copy());
                        }
                     }

                     if (premium) {
                        for (ItemStack stackx : tier.getPremiumRewards()) {
                           if (!stackx.isEmpty()) {
                              items.add(stackx.copy());
                           }
                        }
                     }

                     if (!items.isEmpty() && !canFit(player, items)) {
                        return RewardDispatcher.ClaimResult.INVENTORY_FULL;
                     } else {
                        for (ItemStack stackxx : items) {
                           ItemStack toAdd = stackxx.copy();
                           boolean added = player.getInventory().add(toAdd);
                           if (!added || !toAdd.isEmpty()) {
                              player.drop(toAdd, false);
                           }
                        }

                        runCommands(server, player, tier.getFreeCommands());
                        if (premium) {
                           runCommands(server, player, tier.getPremiumCommands());
                        }

                        if (tier.hasRankReward()) {
                           data.addEarnedRank(tier.getRankReward());
                        }

                        // In test mode never persist the claim so rewards stay re-claimable.
                        if (!data.isTestMode()) {
                           data.markClaimed(tierNumber);
                        }
                        NametagSync.syncPlayer(player);
                        return RewardDispatcher.ClaimResult.SUCCESS;
                     }
                  }
               }
            } else {
               return RewardDispatcher.ClaimResult.INVALID_TIER;
            }
         }
      }
   }

   private static void runCommands(MinecraftServer server, ServerPlayer player, List<String> commands) {
      if (!commands.isEmpty()) {
         String playerName = player.getGameProfile().getName();
         CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();

         for (String raw : commands) {
            if (raw != null && !raw.isBlank()) {
               String command = raw.replace("{player}", playerName).trim();
               if (command.startsWith("/")) {
                  command = command.substring(1);
               }

               server.getCommands().performPrefixedCommand(source, command);
            }
         }
      }
   }

   private static boolean canFit(ServerPlayer player, List<ItemStack> rewards) {
      NonNullList<ItemStack> source = player.getInventory().items;
      int maxStack = player.getInventory().getMaxStackSize();
      ItemStack[] sim = new ItemStack[source.size()];

      for (int i = 0; i < source.size(); i++) {
         sim[i] = ((ItemStack)source.get(i)).copy();
      }

      for (ItemStack reward : rewards) {
         ItemStack remaining = reward.copy();

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

         for (int ix = 0; ix < sim.length && !remaining.isEmpty(); ix++) {
            if (sim[ix].isEmpty()) {
               int cap = Math.min(maxStack, remaining.getMaxStackSize());
               int move = Math.min(cap, remaining.getCount());
               ItemStack placed = remaining.copy();
               placed.setCount(move);
               sim[ix] = placed;
               remaining.shrink(move);
            }
         }

         if (!remaining.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public static Component messageFor(RewardDispatcher.ClaimResult result, int tier) {
      return switch (result) {
         case SUCCESS -> Component.translatable("fantasticpass.msg.claimed", new Object[]{tier});
         case INVENTORY_FULL -> Component.translatable("fantasticpass.msg.inventory_full");
         case NOT_UNLOCKED -> Component.translatable("fantasticpass.msg.not_unlocked");
         case ALREADY_CLAIMED -> Component.translatable("fantasticpass.msg.already_claimed");
         case NO_ACTIVE_PASS, INVALID_TIER -> Component.translatable("fantasticpass.msg.no_active_pass");
      };
   }

   public static enum ClaimResult {
      SUCCESS,
      NO_ACTIVE_PASS,
      NOT_UNLOCKED,
      ALREADY_CLAIMED,
      INVENTORY_FULL,
      INVALID_TIER;
   }
}
