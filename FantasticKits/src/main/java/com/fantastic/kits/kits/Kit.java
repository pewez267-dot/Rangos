package com.fantastic.kits.kits;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Authoritative representation of a kit. The class is mutable so it can be
 * edited live from the GUI, but every external mutation is funneled through
 * {@link KitManager} which applies validation, audit logging and persistence.
 */
public final class Kit {

    private final UUID uuid;
    private final String id;            // stable, lowercase, used in permissions and filenames
    private String displayName;         // shown to players
    private String description;
    private String ownerGroup;          // LuckPerms group required to claim/use
    private ItemStack icon;             // GUI icon
    private final List<ItemStack> contents = new ArrayList<>();
    private CompoundTag customNbt;      // free-form NBT bag carried alongside contents
    private final List<String> commands = new ArrayList<>();
    private final long createdAtEpochMs;
    private long lastEditedEpochMs;
    private KitSecurityConfig security;

    public Kit(String id, String displayName, String ownerGroup) {
        this.uuid = UUID.randomUUID();
        this.id = sanitizeId(id);
        this.displayName = displayName == null || displayName.isBlank() ? id : displayName;
        this.ownerGroup = ownerGroup == null ? "" : ownerGroup;
        this.icon = new ItemStack(Items.CHEST);
        this.customNbt = new CompoundTag();
        this.security = new KitSecurityConfig();
        long now = Instant.now().toEpochMilli();
        this.createdAtEpochMs = now;
        this.lastEditedEpochMs = now;
        this.description = "";
    }

    private Kit(UUID uuid, String id, long createdAtEpochMs) {
        this.uuid = uuid;
        this.id = id;
        this.createdAtEpochMs = createdAtEpochMs;
        this.security = new KitSecurityConfig();
        this.customNbt = new CompoundTag();
        this.icon = new ItemStack(Items.CHEST);
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public UUID uuid() { return uuid; }
    public String id() { return id; }
    public String displayName() { return displayName; }
    public String description() { return description; }
    public String ownerGroup() { return ownerGroup; }
    public ItemStack icon() { return icon; }
    public List<ItemStack> contents() { return contents; }
    public CompoundTag customNbt() { return customNbt; }
    public List<String> commands() { return Collections.unmodifiableList(commands); }
    public long createdAt() { return createdAtEpochMs; }
    public long lastEdited() { return lastEditedEpochMs; }
    public KitSecurityConfig security() { return security; }

    // ------------------------------------------------------------------
    // Mutations - return the kit to allow chaining inside KitManager.
    // ------------------------------------------------------------------

    public Kit displayName(String v) { this.displayName = v == null ? id : v; touch(); return this; }
    public Kit description(String v) { this.description = v == null ? "" : v; touch(); return this; }
    public Kit ownerGroup(String v) { this.ownerGroup = v == null ? "" : v; touch(); return this; }
    public Kit icon(ItemStack v) { this.icon = v == null ? new ItemStack(Items.CHEST) : v.copy(); touch(); return this; }
    public Kit customNbt(CompoundTag v) { this.customNbt = v == null ? new CompoundTag() : v.copy(); touch(); return this; }
    public Kit security(KitSecurityConfig v) { this.security = v == null ? new KitSecurityConfig() : v; touch(); return this; }

    public void replaceContents(List<ItemStack> stacks) {
        contents.clear();
        if (stacks != null) {
            for (ItemStack s : stacks) {
                contents.add(s == null ? ItemStack.EMPTY : s.copy());
            }
        }
        touch();
    }

    public void replaceCommands(List<String> cmds) {
        commands.clear();
        if (cmds != null) {
            for (String c : cmds) {
                if (c == null) continue;
                String trimmed = c.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
                commands.add(trimmed);
            }
        }
        touch();
    }

    private void touch() {
        this.lastEditedEpochMs = Instant.now().toEpochMilli();
    }

    // ------------------------------------------------------------------
    // Serialisation - NBT roundtrip preserves ItemStacks bit-for-bit.
    // ------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("uuid", uuid);
        t.putString("id", id);
        t.putString("displayName", displayName == null ? id : displayName);
        t.putString("description", description == null ? "" : description);
        t.putString("ownerGroup", ownerGroup == null ? "" : ownerGroup);
        t.putLong("createdAt", createdAtEpochMs);
        t.putLong("lastEdited", lastEditedEpochMs);

        CompoundTag iconTag = new CompoundTag();
        icon.save(iconTag);
        t.put("icon", iconTag);

        ListTag list = new ListTag();
        for (ItemStack s : contents) {
            if (s == null || s.isEmpty()) continue;
            CompoundTag ct = new CompoundTag();
            s.save(ct);
            list.add(ct);
        }
        t.put("contents", list);

        ListTag cmds = new ListTag();
        for (String c : commands) cmds.add(StringTag.valueOf(c));
        t.put("commands", cmds);

        t.put("nbt", customNbt == null ? new CompoundTag() : customNbt.copy());
        t.put("security", security.save());
        return t;
    }

    public static Kit load(CompoundTag t) {
        Objects.requireNonNull(t, "kit tag");
        UUID uuid = t.hasUUID("uuid") ? t.getUUID("uuid") : UUID.randomUUID();
        String id = t.getString("id");
        if (id == null || id.isBlank()) id = "kit_" + uuid.toString().substring(0, 8);
        long createdAt = t.contains("createdAt") ? t.getLong("createdAt") : Instant.now().toEpochMilli();

        Kit k = new Kit(uuid, id, createdAt);
        k.displayName = t.contains("displayName") ? t.getString("displayName") : id;
        k.description = t.contains("description") ? t.getString("description") : "";
        k.ownerGroup = t.contains("ownerGroup") ? t.getString("ownerGroup") : "";
        k.lastEditedEpochMs = t.contains("lastEdited") ? t.getLong("lastEdited") : createdAt;

        if (t.contains("icon")) {
            k.icon = ItemStack.of(t.getCompound("icon"));
            if (k.icon.isEmpty()) k.icon = new ItemStack(Items.CHEST);
        }

        if (t.contains("contents")) {
            ListTag list = t.getList("contents", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack s = ItemStack.of(list.getCompound(i));
                if (!s.isEmpty()) k.contents.add(s);
            }
        }

        if (t.contains("commands")) {
            ListTag cmds = t.getList("commands", Tag.TAG_STRING);
            for (int i = 0; i < cmds.size(); i++) k.commands.add(cmds.getString(i));
        }

        if (t.contains("nbt")) k.customNbt = t.getCompound("nbt").copy();
        if (t.contains("security")) k.security = KitSecurityConfig.load(t.getCompound("security"));
        return k;
    }

    public static String sanitizeId(String raw) {
        if (raw == null) return "kit";
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        if (s.isEmpty()) s = "kit";
        if (s.length() > 64) s = s.substring(0, 64);
        return s;
    }

    @Override
    public String toString() {
        return "Kit{" + id + " uuid=" + uuid + " group=" + ownerGroup + " items=" + contents.size() + "}";
    }
}
