package com.fantasticpass.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class TierDefinition {
   private final int tierNumber;
   private final List<ItemStack> freeRewards = new ArrayList<>();
   private final List<String> freeCommands = new ArrayList<>();
   private final List<ItemStack> premiumRewards = new ArrayList<>();
   private final List<String> premiumCommands = new ArrayList<>();
   private PassRankReward rankReward;

   public TierDefinition(int tierNumber) {
      this.tierNumber = tierNumber;
   }

   public int getTierNumber() {
      return this.tierNumber;
   }

   public List<ItemStack> getFreeRewards() {
      return this.freeRewards;
   }

   public List<String> getFreeCommands() {
      return this.freeCommands;
   }

   public List<ItemStack> getPremiumRewards() {
      return this.premiumRewards;
   }

   public List<String> getPremiumCommands() {
      return this.premiumCommands;
   }

   public PassRankReward getRankReward() {
      return this.rankReward;
   }

   public void setRankReward(PassRankReward rankReward) {
      this.rankReward = rankReward;
   }

   public boolean hasRankReward() {
      return this.rankReward != null && this.rankReward.isValid();
   }

   public boolean isEmpty() {
      return this.freeRewards.isEmpty()
         && this.freeCommands.isEmpty()
         && this.premiumRewards.isEmpty()
         && this.premiumCommands.isEmpty()
         && !this.hasRankReward();
   }

   public TierDefinition copy() {
      TierDefinition copy = new TierDefinition(this.tierNumber);

      for (ItemStack stack : this.freeRewards) {
         copy.freeRewards.add(stack.copy());
      }

      copy.freeCommands.addAll(this.freeCommands);

      for (ItemStack stack : this.premiumRewards) {
         copy.premiumRewards.add(stack.copy());
      }

      copy.premiumCommands.addAll(this.premiumCommands);
      if (this.rankReward != null) {
         copy.rankReward = this.rankReward.copy();
      }

      return copy;
   }

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("tier", this.tierNumber);
      tag.put("freeRewards", writeItems(this.freeRewards));
      tag.put("freeCommands", writeStrings(this.freeCommands));
      tag.put("premiumRewards", writeItems(this.premiumRewards));
      tag.put("premiumCommands", writeStrings(this.premiumCommands));
      if (this.rankReward != null) {
         tag.put("rankReward", this.rankReward.toNbt());
      }

      return tag;
   }

   public static TierDefinition fromNbt(CompoundTag tag) {
      TierDefinition tier = new TierDefinition(tag.getInt("tier"));
      readItems(tag.getList("freeRewards", 10), tier.freeRewards);
      readStrings(tag.getList("freeCommands", 8), tier.freeCommands);
      readItems(tag.getList("premiumRewards", 10), tier.premiumRewards);
      readStrings(tag.getList("premiumCommands", 8), tier.premiumCommands);
      if (tag.contains("rankReward", 10)) {
         tier.rankReward = PassRankReward.fromNbt(tag.getCompound("rankReward"));
      }

      return tier;
   }

   private static ListTag writeItems(List<ItemStack> items) {
      ListTag list = new ListTag();

      for (ItemStack stack : items) {
         if (!stack.isEmpty()) {
            list.add(stack.save(new CompoundTag()));
         }
      }

      return list;
   }

   private static void readItems(ListTag list, List<ItemStack> target) {
      target.clear();

      for (int i = 0; i < list.size(); i++) {
         ItemStack stack = ItemStack.of(list.getCompound(i));
         if (!stack.isEmpty()) {
            target.add(stack);
         }
      }
   }

   private static ListTag writeStrings(List<String> strings) {
      ListTag list = new ListTag();

      for (String s : strings) {
         list.add(StringTag.valueOf(s));
      }

      return list;
   }

   private static void readStrings(ListTag list, List<String> target) {
      target.clear();

      for (int i = 0; i < list.size(); i++) {
         target.add(list.getString(i));
      }
   }

   public void toBuf(FriendlyByteBuf buf) {
      buf.writeVarInt(this.tierNumber);
      buf.writeVarInt(this.freeRewards.size());

      for (ItemStack stack : this.freeRewards) {
         buf.writeItem(stack);
      }

      buf.writeVarInt(this.freeCommands.size());

      for (String s : this.freeCommands) {
         buf.writeUtf(s);
      }

      buf.writeVarInt(this.premiumRewards.size());

      for (ItemStack stack : this.premiumRewards) {
         buf.writeItem(stack);
      }

      buf.writeVarInt(this.premiumCommands.size());

      for (String s : this.premiumCommands) {
         buf.writeUtf(s);
      }

      buf.writeBoolean(this.rankReward != null);
      if (this.rankReward != null) {
         this.rankReward.toBuf(buf);
      }
   }

   public static TierDefinition fromBuf(FriendlyByteBuf buf) {
      TierDefinition tier = new TierDefinition(buf.readVarInt());
      int free = buf.readVarInt();

      for (int i = 0; i < free; i++) {
         tier.freeRewards.add(buf.readItem());
      }

      int freeCmd = buf.readVarInt();

      for (int i = 0; i < freeCmd; i++) {
         tier.freeCommands.add(buf.readUtf());
      }

      int prem = buf.readVarInt();

      for (int i = 0; i < prem; i++) {
         tier.premiumRewards.add(buf.readItem());
      }

      int premCmd = buf.readVarInt();

      for (int i = 0; i < premCmd; i++) {
         tier.premiumCommands.add(buf.readUtf());
      }

      if (buf.readBoolean()) {
         tier.rankReward = PassRankReward.fromBuf(buf);
      }

      return tier;
   }
}
