package com.fantasticterraform.intelligent.dungeon.boss;

/** Configuracion del encuentro de jefe: entidad, cantidad y equipamiento basico. */
public final class BossRoomConfig {

    public final String entityId;
    public final int count;
    public final boolean equip;

    public BossRoomConfig(String entityId, int count, boolean equip) {
        this.entityId = entityId;
        this.count = Math.max(1, count);
        this.equip = equip;
    }
}
