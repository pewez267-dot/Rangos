package com.fantasticranks.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A single rank within a {@link RanksPackage}: its display name, the real (non-AFK)
 * hours of play required to reach it, and the full visual {@link NametagStyle}. The
 * {@code rankNumber} is the 1-based position shown in the nametag ("Lvl N").
 */
public final class RankDefinition {

    private int rankNumber;
    private String rankName;
    private double hoursRequired;
    private NametagStyle style;

    public RankDefinition(int rankNumber, String rankName, double hoursRequired, NametagStyle style) {
        this.rankNumber = rankNumber;
        this.rankName = rankName == null ? "" : rankName;
        this.hoursRequired = Math.max(0.0D, hoursRequired);
        this.style = style == null ? new NametagStyle() : style;
    }

    public int getRankNumber() {
        return rankNumber;
    }

    public void setRankNumber(int rankNumber) {
        this.rankNumber = rankNumber;
    }

    public String getRankName() {
        return rankName;
    }

    public void setRankName(String rankName) {
        this.rankName = rankName == null ? "" : rankName;
    }

    public double getHoursRequired() {
        return hoursRequired;
    }

    public void setHoursRequired(double hoursRequired) {
        this.hoursRequired = Math.max(0.0D, hoursRequired);
    }

    public NametagStyle getStyle() {
        return style;
    }

    public void setStyle(NametagStyle style) {
        this.style = style == null ? new NametagStyle() : style;
    }

    public RankDefinition copy() {
        return new RankDefinition(rankNumber, rankName, hoursRequired, style.copy());
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("rankNumber", rankNumber);
        tag.putString("rankName", rankName);
        tag.putDouble("hoursRequired", hoursRequired);
        tag.put("style", style.toNbt());
        return tag;
    }

    public static RankDefinition fromNbt(CompoundTag tag) {
        return new RankDefinition(
                tag.getInt("rankNumber"),
                tag.getString("rankName"),
                tag.getDouble("hoursRequired"),
                NametagStyle.fromNbt(tag.getCompound("style")));
    }

    public void toBuf(FriendlyByteBuf buf) {
        buf.writeVarInt(rankNumber);
        buf.writeUtf(rankName);
        buf.writeDouble(hoursRequired);
        style.toBuf(buf);
    }

    public static RankDefinition fromBuf(FriendlyByteBuf buf) {
        int number = buf.readVarInt();
        String name = buf.readUtf();
        double hours = buf.readDouble();
        NametagStyle style = NametagStyle.fromBuf(buf);
        return new RankDefinition(number, name, hours, style);
    }
}
