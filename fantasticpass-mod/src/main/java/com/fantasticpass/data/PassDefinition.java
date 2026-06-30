package com.fantasticpass.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;

public final class PassDefinition {
   public static final int TIER_COUNT = 100;
   private String id;
   private String name;
   private final TierDefinition[] tiers = new TierDefinition[100];
   private int minutesPerTierOverride;
   private int tierCount = 100;

   public PassDefinition(String id, String name) {
      this.id = id == null ? "" : id;
      this.name = name == null ? "" : name;
      this.minutesPerTierOverride = 0;

      for (int i = 0; i < 100; i++) {
         this.tiers[i] = new TierDefinition(i + 1);
      }
   }

   /** Number of tiers actually used/shown by this pass (1-100). */
   public int getTierCount() {
      return this.tierCount < 1 || this.tierCount > 100 ? 100 : this.tierCount;
   }

   public void setTierCount(int tierCount) {
      this.tierCount = Math.max(1, Math.min(100, tierCount));
   }

   public String getId() {
      return this.id;
   }

   public void setId(String id) {
      this.id = id == null ? "" : id;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name == null ? "" : name;
   }

   public int getMinutesPerTierOverride() {
      return this.minutesPerTierOverride;
   }

   public void setMinutesPerTierOverride(int minutesPerTierOverride) {
      this.minutesPerTierOverride = minutesPerTierOverride;
   }

   public TierDefinition[] getTiers() {
      return this.tiers;
   }

   public TierDefinition getTier(int tierNumber) {
      return tierNumber >= 1 && tierNumber <= 100 ? this.tiers[tierNumber - 1] : null;
   }

   public void setTier(int tierNumber, TierDefinition definition) {
      if (tierNumber >= 1 && tierNumber <= 100 && definition != null) {
         this.tiers[tierNumber - 1] = definition;
      }
   }

   public PassDefinition copy() {
      PassDefinition copy = new PassDefinition(this.id, this.name);
      copy.minutesPerTierOverride = this.minutesPerTierOverride;
      copy.tierCount = this.tierCount;

      for (int i = 0; i < 100; i++) {
         copy.tiers[i] = this.tiers[i].copy();
      }

      return copy;
   }

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putString("id", this.id);
      tag.putString("name", this.name);
      tag.putInt("minutesPerTierOverride", this.minutesPerTierOverride);
      tag.putInt("tierCount", this.tierCount);
      ListTag list = new ListTag();

      for (TierDefinition tier : this.tiers) {
         list.add(tier.toNbt());
      }

      tag.put("tiers", list);
      return tag;
   }

   public static PassDefinition fromNbt(CompoundTag tag) {
      PassDefinition pass = new PassDefinition(tag.getString("id"), tag.getString("name"));
      pass.minutesPerTierOverride = tag.getInt("minutesPerTierOverride");
      pass.tierCount = tag.contains("tierCount") ? tag.getInt("tierCount") : 100;
      ListTag list = tag.getList("tiers", 10);

      for (int i = 0; i < list.size(); i++) {
         TierDefinition tier = TierDefinition.fromNbt(list.getCompound(i));
         int idx = tier.getTierNumber() - 1;
         if (idx >= 0 && idx < 100) {
            pass.tiers[idx] = tier;
         }
      }

      return pass;
   }

   public void toBuf(FriendlyByteBuf buf) {
      buf.writeUtf(this.id);
      buf.writeUtf(this.name);
      buf.writeVarInt(this.minutesPerTierOverride);
      buf.writeVarInt(this.tierCount);

      for (TierDefinition tier : this.tiers) {
         tier.toBuf(buf);
      }
   }

   public static PassDefinition fromBuf(FriendlyByteBuf buf) {
      PassDefinition pass = new PassDefinition(buf.readUtf(), buf.readUtf());
      pass.minutesPerTierOverride = buf.readVarInt();
      pass.tierCount = buf.readVarInt();

      for (int i = 0; i < 100; i++) {
         TierDefinition tier = TierDefinition.fromBuf(buf);
         int idx = tier.getTierNumber() - 1;
         if (idx >= 0 && idx < 100) {
            pass.tiers[idx] = tier;
         }
      }

      return pass;
   }
}
