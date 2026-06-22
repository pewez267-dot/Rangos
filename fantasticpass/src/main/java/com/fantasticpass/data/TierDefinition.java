package com.fantasticpass.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A single tier within a {@link PassDefinition}. Holds the free and premium item
 * rewards, the free and premium reward commands, and an optional pass-rank reward.
 */
public final class TierDefinition {

    private final int tierNumber;
    private final List<ItemStack> freeRewards = new ArrayList<>();
    private final List<String> freeCommands = new ArrayList<>();
    private final List<ItemStack> premiumRewards = new ArrayList<>();
    private final List<String> premiumCommands = new ArrayList<>();
    private PassRankReward rankReward; // nullable

    public TierDefinition(int tierNumber) {
        this.tierNumber = tierNumber;
    }

    public int getTierNumber() {
        return tierNumber;
    }

    public List<ItemStack> getFreeRewards() {
        return freeRewards;
    }

    public List<String> getFreeCommands() {
        return freeCommands;
    }

    public List<ItemStack> getPremiumRewards() {
        return premiumRewards;
    }

    public List<String> getPremiumCommands() {
        return premiumCommands;
    }

    public PassRankReward getRankReward() {
        return rankReward;
    }

    public void setRankReward(PassRankReward rankReward) {
        this.rankReward = rankReward;
    }

    public boolean hasRankReward() {
        return rankReward != null && rankReward.isValid();
    }

    public boolean isEmpty() {
        return freeRewards.isEmpty() && freeCommands.isEmpty()
                && premiumRewards.isEmpty() && premiumCommands.isEmpty()
                && !hasRankReward();
    }

    public TierDefinition copy() {
        TierDefinition copy = new TierDefinition(tierNumber);
        for (ItemStack stack : freeRewards) {
            copy.freeRewards.add(stack.copy());
        }
        copy.freeCommands.addAll(freeCommands);
        for (ItemStack stack : premiumRewards) {
            copy.premiumRewards.add(stack.copy());
        }
        copy.premiumCommands.addAll(premiumCommands);
        if (rankReward != null) {
            copy.rankReward = rankReward.copy();
        }
        return copy;
    }

    // ---- NBT ----

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("tier", tierNumber);
        tag.put("freeRewards", writeItems(freeRewards));
        tag.put("freeCommands", writeStrings(freeCommands));
        tag.put("premiumRewards", writeItems(premiumRewards));
        tag.put("premiumCommands", writeStrings(premiumCommands));
        if (rankReward != null) {
            tag.put("rankReward", rankReward.toNbt());
        }
        return tag;
    }

    public static TierDefinition fromNbt(CompoundTag tag) {
        TierDefinition tier = new TierDefinition(tag.getInt("tier"));
        readItems(tag.getList("freeRewards", Tag.TAG_COMPOUND), tier.freeRewards);
        readStrings(tag.getList("freeCommands", Tag.TAG_STRING), tier.freeCommands);
        readItems(tag.getList("premiumRewards", Tag.TAG_COMPOUND), tier.premiumRewards);
        readStrings(tag.getList("premiumCommands", Tag.TAG_STRING), tier.premiumCommands);
        if (tag.contains("rankReward", Tag.TAG_COMPOUND)) {
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

    // ---- Network ----

    public void toBuf(FriendlyByteBuf buf) {
        buf.writeVarInt(tierNumber);

        buf.writeVarInt(freeRewards.size());
        for (ItemStack stack : freeRewards) {
            buf.writeItem(stack);
        }
        buf.writeVarInt(freeCommands.size());
        for (String s : freeCommands) {
            buf.writeUtf(s);
        }
        buf.writeVarInt(premiumRewards.size());
        for (ItemStack stack : premiumRewards) {
            buf.writeItem(stack);
        }
        buf.writeVarInt(premiumCommands.size());
        for (String s : premiumCommands) {
            buf.writeUtf(s);
        }

        buf.writeBoolean(rankReward != null);
        if (rankReward != null) {
            rankReward.toBuf(buf);
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
