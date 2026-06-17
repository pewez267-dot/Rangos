package com.claimblocks.data;

import net.minecraft.nbt.NbtCompound;

/**
 * Holds the protection flags for a single Claim.
 * All flags default to "deny" (i.e. only the owner / members can do the action),
 * so a true value means "allow non-members to do this".
 *
 * TRESPASSER_ALERTS is special: when true, the owner is notified when a
 * non-authorised player enters the claim.
 */
public class ClaimFlags {
    // "Allow others to..." flags. False = protected, True = anyone can do it.
    private boolean creeping;          // place blocks
    private boolean breaking;          // break blocks
    private boolean explosions;        // explosions allowed inside
    private boolean fire;              // fire / lava spread allowed
    private boolean mobs;              // mob spawning allowed
    private boolean pvp;               // pvp allowed
    private boolean mobDamage;         // mobs can damage players inside
    // Owner-side notification toggle
    private boolean trespasserAlerts;  // notify owner of intruders

    public ClaimFlags() {
        this.creeping = false;
        this.breaking = false;
        this.explosions = false;
        this.fire = false;
        this.mobs = true;          // mobs spawn by default (vanilla behaviour)
        this.pvp = false;
        this.mobDamage = true;     // mobs damage by default
        this.trespasserAlerts = true;
    }

    public boolean isCreeping() { return creeping; }
    public void setCreeping(boolean v) { creeping = v; }

    public boolean isBreaking() { return breaking; }
    public void setBreaking(boolean v) { breaking = v; }

    public boolean isExplosions() { return explosions; }
    public void setExplosions(boolean v) { explosions = v; }

    public boolean isFire() { return fire; }
    public void setFire(boolean v) { fire = v; }

    public boolean isMobs() { return mobs; }
    public void setMobs(boolean v) { mobs = v; }

    public boolean isPvp() { return pvp; }
    public void setPvp(boolean v) { pvp = v; }

    public boolean isMobDamage() { return mobDamage; }
    public void setMobDamage(boolean v) { mobDamage = v; }

    public boolean isTrespasserAlerts() { return trespasserAlerts; }
    public void setTrespasserAlerts(boolean v) { trespasserAlerts = v; }

    public void toggle(String key) {
        switch (key) {
            case "CREEPING" -> creeping = !creeping;
            case "BREAKING" -> breaking = !breaking;
            case "EXPLOSIONS" -> explosions = !explosions;
            case "FIRE" -> fire = !fire;
            case "MOBS" -> mobs = !mobs;
            case "PVP" -> pvp = !pvp;
            case "MOB_DAMAGE" -> mobDamage = !mobDamage;
            case "TRESPASSER_ALERTS" -> trespasserAlerts = !trespasserAlerts;
            default -> {}
        }
    }

    public boolean get(String key) {
        return switch (key) {
            case "CREEPING" -> creeping;
            case "BREAKING" -> breaking;
            case "EXPLOSIONS" -> explosions;
            case "FIRE" -> fire;
            case "MOBS" -> mobs;
            case "PVP" -> pvp;
            case "MOB_DAMAGE" -> mobDamage;
            case "TRESPASSER_ALERTS" -> trespasserAlerts;
            default -> false;
        };
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("creeping", creeping);
        nbt.putBoolean("breaking", breaking);
        nbt.putBoolean("explosions", explosions);
        nbt.putBoolean("fire", fire);
        nbt.putBoolean("mobs", mobs);
        nbt.putBoolean("pvp", pvp);
        nbt.putBoolean("mobDamage", mobDamage);
        nbt.putBoolean("trespasserAlerts", trespasserAlerts);
        return nbt;
    }

    public static ClaimFlags fromNbt(NbtCompound nbt) {
        ClaimFlags f = new ClaimFlags();
        f.creeping = nbt.getBoolean("creeping");
        f.breaking = nbt.getBoolean("breaking");
        f.explosions = nbt.getBoolean("explosions");
        f.fire = nbt.getBoolean("fire");
        f.mobs = nbt.contains("mobs") ? nbt.getBoolean("mobs") : true;
        f.pvp = nbt.getBoolean("pvp");
        f.mobDamage = nbt.contains("mobDamage") ? nbt.getBoolean("mobDamage") : true;
        f.trespasserAlerts = nbt.contains("trespasserAlerts") ? nbt.getBoolean("trespasserAlerts") : true;
        return f;
    }
}
