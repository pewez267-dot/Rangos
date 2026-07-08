/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.nbt.CompoundTag
 */
package com.fantasticranks.data;

import com.fantasticranks.data.NametagStyle;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;

public final class PlayerRanksData {
    private double minutesActive;
    private int partialSeconds;
    private int currentRankIndex;
    private String activePackageId = "";
    private boolean previewActive;
    private String previewRankName = "";
    private int previewRankNumber;
    @Nullable
    private NametagStyle previewStyle;
    // Ultima "generacion de wipe" que este jugador ya aplico. Si es menor que la del servidor,
    // se le limpia el progreso (asi los jugadores offline tambien se limpian al reconectar).
    private long wipeSeen;

    public long getWipeSeen() {
        return this.wipeSeen;
    }

    public void setWipeSeen(long wipeSeen) {
        this.wipeSeen = wipeSeen;
    }

    public double getMinutesActive() {
        return this.minutesActive;
    }

    public void setMinutesActive(double minutesActive) {
        this.minutesActive = Math.max(0.0, minutesActive);
    }

    public double getHoursActive() {
        return this.minutesActive / 60.0;
    }

    public int addActiveSeconds(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        this.partialSeconds += seconds;
        int gained = this.partialSeconds / 60;
        this.partialSeconds %= 60;
        if (gained > 0) {
            this.minutesActive += (double)gained;
        }
        return gained;
    }

    public int getCurrentRankIndex() {
        return this.currentRankIndex;
    }

    public void setCurrentRankIndex(int currentRankIndex) {
        this.currentRankIndex = Math.max(0, currentRankIndex);
    }

    public String getActivePackageId() {
        return this.activePackageId;
    }

    public void setActivePackageId(String activePackageId) {
        this.activePackageId = activePackageId == null ? "" : activePackageId;
    }

    public void resetProgress(String newActivePackageId) {
        this.minutesActive = 0.0;
        this.partialSeconds = 0;
        this.currentRankIndex = 0;
        this.activePackageId = newActivePackageId == null ? "" : newActivePackageId;
    }

    public void copyFrom(PlayerRanksData other) {
        this.minutesActive = other.minutesActive;
        this.partialSeconds = other.partialSeconds;
        this.currentRankIndex = other.currentRankIndex;
        this.activePackageId = other.activePackageId;
    }

    public boolean isPreviewActive() {
        return this.previewActive && this.previewStyle != null;
    }

    public String getPreviewRankName() {
        return this.previewRankName;
    }

    public int getPreviewRankNumber() {
        return this.previewRankNumber;
    }

    @Nullable
    public NametagStyle getPreviewStyle() {
        return this.previewStyle;
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

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("minutesActive", this.minutesActive);
        tag.putInt("partialSeconds", this.partialSeconds);
        tag.putInt("currentRankIndex", this.currentRankIndex);
        tag.putString("activePackageId", this.activePackageId);
        tag.putLong("wipeSeen", this.wipeSeen);
        return tag;
    }

    public void fromNbt(CompoundTag tag) {
        this.minutesActive = tag.getDouble("minutesActive");
        this.partialSeconds = tag.getInt("partialSeconds");
        this.currentRankIndex = tag.getInt("currentRankIndex");
        this.activePackageId = tag.getString("activePackageId");
        this.wipeSeen = tag.getLong("wipeSeen");
    }
}

