package com.arthurleray.invhistory.config;

/** Config POJO (mirrors the original field-for-field). */
public class InvHistoryConfig {
    private int snapshotIntervalSeconds = 300;
    private int maxSnapshotsPerPlayer = 100;
    private boolean snapshotOnJoin = true;
    private boolean snapshotOnLeave = true;
    private boolean snapshotOnDeath = true;
    private String storage = "json";
    private int permissionLevel = 2;

    public int getSnapshotIntervalSeconds() {
        return this.snapshotIntervalSeconds;
    }

    public void setSnapshotIntervalSeconds(int snapshotIntervalSeconds) {
        this.snapshotIntervalSeconds = snapshotIntervalSeconds;
    }

    public int getMaxSnapshotsPerPlayer() {
        return this.maxSnapshotsPerPlayer;
    }

    public void setMaxSnapshotsPerPlayer(int maxSnapshotsPerPlayer) {
        this.maxSnapshotsPerPlayer = maxSnapshotsPerPlayer;
    }

    public boolean isSnapshotOnJoin() {
        return this.snapshotOnJoin;
    }

    public void setSnapshotOnJoin(boolean snapshotOnJoin) {
        this.snapshotOnJoin = snapshotOnJoin;
    }


    public boolean isSnapshotOnLeave() {
        return this.snapshotOnLeave;
    }

    public void setSnapshotOnLeave(boolean snapshotOnLeave) {
        this.snapshotOnLeave = snapshotOnLeave;
    }

    public boolean isSnapshotOnDeath() {
        return this.snapshotOnDeath;
    }

    public void setSnapshotOnDeath(boolean snapshotOnDeath) {
        this.snapshotOnDeath = snapshotOnDeath;
    }

    public String getStorage() {
        return this.storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public int getPermissionLevel() {
        return this.permissionLevel;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}
