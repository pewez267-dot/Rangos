package com.fantasticchest.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain serialisable definition of a chest, mirroring an entry of {@code chests.json}.
 * Quantities are {@code Long} (serialised as JSON numbers, never truncated).
 */
public final class ChestDefinition {

    public static final class Pos {
        public int x;
        public int y;
        public int z;

        public Pos() {
        }

        public Pos(final int x, final int y, final int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public String id = "";
    public String name = "";
    @SerializedName("owner_uuid")
    public String ownerUuid = "";
    public List<String> permitted = new ArrayList<>();
    public String world = "";
    public Pos pos = null;
    public boolean placed = false;
    public Map<String, Long> inventory = new LinkedHashMap<>();
    @SerializedName("original_stock")
    public Map<String, Long> originalStock = new LinkedHashMap<>();
    @SerializedName("created_at")
    public String createdAt = "";
    @SerializedName("created_by")
    public String createdBy = "";

    public ChestDefinition copy() {
        final ChestDefinition c = new ChestDefinition();
        c.id = this.id;
        c.name = this.name;
        c.ownerUuid = this.ownerUuid;
        c.permitted = new ArrayList<>(this.permitted == null ? List.of() : this.permitted);
        c.world = this.world;
        c.pos = this.pos == null ? null : new Pos(this.pos.x, this.pos.y, this.pos.z);
        c.placed = this.placed;
        c.inventory = new LinkedHashMap<>(this.inventory == null ? Map.of() : this.inventory);
        c.originalStock = new LinkedHashMap<>(this.originalStock == null ? Map.of() : this.originalStock);
        c.createdAt = this.createdAt;
        c.createdBy = this.createdBy;
        return c;
    }
}
