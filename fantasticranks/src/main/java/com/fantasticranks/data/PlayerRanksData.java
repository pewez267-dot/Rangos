package com.fantasticranks.data;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/**
 * Per-player rank progress, persisted via a Forge capability (NBT). Also holds a
 * transient "test preview" (set by {@code /fsranks test}) that overrides the rendered
 * nametag without touching real progress and is intentionally never serialized, so it
 * clears on reconnect.
 */
public final class PlayerRanksData {

    private double minutesActive;
    /** Active seconds accumulated towards the next whole minute (0..59). */
    private int partialSeconds;
    private int currentRankIndex;
    private String activePackageId = "";

    // ---- Transient preview (not serialized) ----
    private boolean previewActive;
    private String previewRankName = "";
    private int previewRankNumber;
    @Nullable
    private NametagStyle previewStyle;

    public double getMinutesActive() {
        return minutesActive;
    }

    public void setMinutesActive(double minutesActive) {
        this.minutesActive = Math.max(0.0D, minutesActive);
    }

    public double getHoursActive() {
        return minutesActive / 60.0D;
    }

    /**
     * Adds active seconds and rolls whole minutes into {@link #minutesActive}.
     *
     * @return the number of whole minutes that were added
     */
    public int addActiveSeconds(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        partialSeconds += seconds;
        int gained = partialSeconds / 60;
        partialSeconds %= 60;
        if (gained > 0) {
            minutesActive += gained;
        }
        return gained;
    }

    public int getCurrentRankIndex() {
        return currentRankIndex;
    }

    public void setCurrentRankIndex(int currentRankIndex) {
        this.currentRankIndex = Math.max(0, currentRankIndex);
    }

    public String getActivePackageId() {
        return activePackageId;
    }

    public void setActivePackageId(String activePackageId) {
        this.activePackageId = activePackageId == null ? "" : activePackageId;
    }

    public void resetProgress(String newActivePackageId) {
        this.minutesActive = 0.0D;
        this.partialSeconds = 0;
        this.currentRankIndex = 0;
        this.activePackageId = newActivePackageId == null ? "" : newActivePackageId;
    }

    public void copyFrom(PlayerRanksData other) {
        this.minutesActive = other.minutesActive;
        this.partialSeconds = other.partialSeconds;
        this.currentRankIndex = other.currentRankIndex;
        this.activePackageId = other.activePackageId;
        // Preview is intentionally not copied (session-only).
    }

    // ---- Preview ----

    public boolean isPreviewActive() {
        return previewActive && previewStyle != null;
    }

    public String getPreviewRankName() {
        return previewRankName;
    }

    public int getPreviewRankNumber() {
        return previewRankNumber;
    }

    @Nullable
    public NametagStyle getPreviewStyle() {
        return previewStyle;
    }

    public void setPreview(String rankName, int rankNumber, NametagStyle style) {
        this.previewActive = true;
        this.previewRankName = rankName == null ? "" : rankName;
        this.previewRankNumber = rankNumber;
        this.previewStyle = style == null ? null : style.copy();
    }

    public void clearPreview() {
        this.previewActive = false;
        this.previewRankName = "";
        this.previewRankNumber = 0;
        this.previewStyle = null;
    }

    // ---- NBT (preview excluded on purpose) ----

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("minutesActive", minutesActive);
        tag.putInt("partialSeconds", partialSeconds);
        tag.putInt("currentRankIndex", currentRankIndex);
        tag.putString("activePackageId", activePackageId);
        return tag;
    }

    public void fromNbt(CompoundTag tag) {
        this.minutesActive = tag.getDouble("minutesActive");
        this.partialSeconds = tag.getInt("partialSeconds");
        this.currentRankIndex = tag.getInt("currentRankIndex");
        this.activePackageId = tag.getString("activePackageId");
    }
}
