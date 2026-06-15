package com.fantastic.kits.kits;

import net.minecraft.nbt.CompoundTag;

/**
 * Per-kit security overrides. These complement the global {@code antiexploit.*}
 * options and let an administrator harden a single high-value kit (e.g. an
 * Owner-only kit) without changing global defaults.
 */
public final class KitSecurityConfig {

    /** Strict primary group match (always true, here for explicit auditing). */
    public boolean strictGroupMatching = true;
    /** Block claims if the inventory has no room. */
    public boolean blockOnFullInventory = true;
    /** Refuse claims when the player is dead, in spectator or in a portal. */
    public boolean blockUnsafeContexts = true;
    /** Re-validate inventory snapshot on the server before delivery. */
    public boolean validateInventorySync = true;
    /** Reject the claim if the client packet contained tampered fields. */
    public boolean rejectForgedClient = true;

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("strictGroupMatching", strictGroupMatching);
        t.putBoolean("blockOnFullInventory", blockOnFullInventory);
        t.putBoolean("blockUnsafeContexts", blockUnsafeContexts);
        t.putBoolean("validateInventorySync", validateInventorySync);
        t.putBoolean("rejectForgedClient", rejectForgedClient);
        return t;
    }

    public static KitSecurityConfig load(CompoundTag t) {
        KitSecurityConfig c = new KitSecurityConfig();
        if (t == null || t.isEmpty()) return c;
        if (t.contains("strictGroupMatching")) c.strictGroupMatching = t.getBoolean("strictGroupMatching");
        if (t.contains("blockOnFullInventory")) c.blockOnFullInventory = t.getBoolean("blockOnFullInventory");
        if (t.contains("blockUnsafeContexts")) c.blockUnsafeContexts = t.getBoolean("blockUnsafeContexts");
        if (t.contains("validateInventorySync")) c.validateInventorySync = t.getBoolean("validateInventorySync");
        if (t.contains("rejectForgedClient")) c.rejectForgedClient = t.getBoolean("rejectForgedClient");
        return c;
    }
}
