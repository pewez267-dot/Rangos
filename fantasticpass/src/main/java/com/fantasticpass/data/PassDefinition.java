package com.fantasticpass.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A saved Battle Pass template. Always contains exactly {@link #TIER_COUNT} tiers.
 */
public final class PassDefinition {

    public static final int TIER_COUNT = 100;

    private String id;
    private String name;
    private final TierDefinition[] tiers = new TierDefinition[TIER_COUNT];

    /** Per-pass override of minutes/tier. {@code <= 0} means "use the global config value". */
    private int minutesPerTierOverride;

    public PassDefinition(String id, String name) {
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name;
        this.minutesPerTierOverride = 0;
        for (int i = 0; i < TIER_COUNT; i++) {
            tiers[i] = new TierDefinition(i + 1);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public int getMinutesPerTierOverride() {
        return minutesPerTierOverride;
    }

    public void setMinutesPerTierOverride(int minutesPerTierOverride) {
        this.minutesPerTierOverride = minutesPerTierOverride;
    }

    public TierDefinition[] getTiers() {
        return tiers;
    }

    /**
     * @param tierNumber 1-based tier number (1..100)
     * @return the tier, or {@code null} if out of range
     */
    public TierDefinition getTier(int tierNumber) {
        if (tierNumber < 1 || tierNumber > TIER_COUNT) {
            return null;
        }
        return tiers[tierNumber - 1];
    }

    public void setTier(int tierNumber, TierDefinition definition) {
        if (tierNumber >= 1 && tierNumber <= TIER_COUNT && definition != null) {
            tiers[tierNumber - 1] = definition;
        }
    }

    public PassDefinition copy() {
        PassDefinition copy = new PassDefinition(id, name);
        copy.minutesPerTierOverride = minutesPerTierOverride;
        for (int i = 0; i < TIER_COUNT; i++) {
            copy.tiers[i] = tiers[i].copy();
        }
        return copy;
    }

    // ---- NBT ----

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);
        tag.putInt("minutesPerTierOverride", minutesPerTierOverride);
        ListTag list = new ListTag();
        for (TierDefinition tier : tiers) {
            list.add(tier.toNbt());
        }
        tag.put("tiers", list);
        return tag;
    }

    public static PassDefinition fromNbt(CompoundTag tag) {
        PassDefinition pass = new PassDefinition(tag.getString("id"), tag.getString("name"));
        pass.minutesPerTierOverride = tag.getInt("minutesPerTierOverride");
        ListTag list = tag.getList("tiers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            TierDefinition tier = TierDefinition.fromNbt(list.getCompound(i));
            int idx = tier.getTierNumber() - 1;
            if (idx >= 0 && idx < TIER_COUNT) {
                pass.tiers[idx] = tier;
            }
        }
        return pass;
    }

    // ---- Network ----

    public void toBuf(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeUtf(name);
        buf.writeVarInt(minutesPerTierOverride);
        for (TierDefinition tier : tiers) {
            tier.toBuf(buf);
        }
    }

    public static PassDefinition fromBuf(FriendlyByteBuf buf) {
        PassDefinition pass = new PassDefinition(buf.readUtf(), buf.readUtf());
        pass.minutesPerTierOverride = buf.readVarInt();
        for (int i = 0; i < TIER_COUNT; i++) {
            TierDefinition tier = TierDefinition.fromBuf(buf);
            int idx = tier.getTierNumber() - 1;
            if (idx >= 0 && idx < TIER_COUNT) {
                pass.tiers[idx] = tier;
            }
        }
        return pass;
    }
}
