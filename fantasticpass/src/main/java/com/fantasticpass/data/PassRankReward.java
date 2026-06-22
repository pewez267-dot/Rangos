package com.fantasticpass.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A cosmetic pass rank awarded for completing a tier (typically only tier 100).
 * It carries a unique id, the display text shown under the player's name, and the
 * full {@link NametagStyle} used to render it.
 */
public final class PassRankReward {

    private String rankId;
    private String rankDisplayText;
    private NametagStyle style;

    public PassRankReward() {
        this.rankId = "";
        this.rankDisplayText = "";
        this.style = new NametagStyle();
    }

    public PassRankReward(String rankId, String rankDisplayText, NametagStyle style) {
        this.rankId = rankId == null ? "" : rankId;
        this.rankDisplayText = rankDisplayText == null ? "" : rankDisplayText;
        this.style = style == null ? new NametagStyle() : style;
    }

    public String getRankId() {
        return rankId;
    }

    public void setRankId(String rankId) {
        this.rankId = rankId == null ? "" : rankId;
    }

    public String getRankDisplayText() {
        return rankDisplayText;
    }

    public void setRankDisplayText(String rankDisplayText) {
        this.rankDisplayText = rankDisplayText == null ? "" : rankDisplayText;
    }

    public NametagStyle getStyle() {
        return style;
    }

    public void setStyle(NametagStyle style) {
        this.style = style == null ? new NametagStyle() : style;
    }

    public boolean isValid() {
        return rankId != null && !rankId.isEmpty();
    }

    public PassRankReward copy() {
        return new PassRankReward(rankId, rankDisplayText, style.copy());
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("rankId", rankId);
        tag.putString("rankDisplayText", rankDisplayText);
        tag.put("style", style.toNbt());
        return tag;
    }

    public static PassRankReward fromNbt(CompoundTag tag) {
        PassRankReward reward = new PassRankReward();
        reward.rankId = tag.getString("rankId");
        reward.rankDisplayText = tag.getString("rankDisplayText");
        reward.style = NametagStyle.fromNbt(tag.getCompound("style"));
        return reward;
    }

    public void toBuf(FriendlyByteBuf buf) {
        buf.writeUtf(rankId);
        buf.writeUtf(rankDisplayText);
        style.toBuf(buf);
    }

    public static PassRankReward fromBuf(FriendlyByteBuf buf) {
        PassRankReward reward = new PassRankReward();
        reward.rankId = buf.readUtf();
        reward.rankDisplayText = buf.readUtf();
        reward.style = NametagStyle.fromBuf(buf);
        return reward;
    }
}
