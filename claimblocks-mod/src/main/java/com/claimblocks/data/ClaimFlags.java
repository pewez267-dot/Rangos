package com.claimblocks.data;

/**
 * The 8 protection flags. A flag value of {@code true} means the action is
 * BLOCKED for non-members (i.e. protection is ON). The exception is
 * {@code trespasserAlerts} which means "send an alert to the owner" when true.
 *
 * Default values follow the spec example JSON: most protections ON, but
 * mob spawning and mob damage are NOT blocked, and alerts are off.
 */
public class ClaimFlags {
    public boolean blockBuilding   = true;
    public boolean blockBreaking   = true;
    public boolean blockExplosions = true;
    public boolean blockFire       = true;
    public boolean blockMobSpawn   = false;
    public boolean blockPVP        = true;
    public boolean blockMobDamage  = false;
    public boolean trespasserAlerts = false;

    public boolean get(FlagId id) {
        return switch (id) {
            case BUILDING        -> blockBuilding;
            case BREAKING        -> blockBreaking;
            case EXPLOSIONS      -> blockExplosions;
            case FIRE            -> blockFire;
            case MOB_SPAWN       -> blockMobSpawn;
            case PVP             -> blockPVP;
            case MOB_DAMAGE      -> blockMobDamage;
            case ALERTS          -> trespasserAlerts;
        };
    }

    public void set(FlagId id, boolean value) {
        switch (id) {
            case BUILDING   -> blockBuilding   = value;
            case BREAKING   -> blockBreaking   = value;
            case EXPLOSIONS -> blockExplosions = value;
            case FIRE       -> blockFire       = value;
            case MOB_SPAWN  -> blockMobSpawn   = value;
            case PVP        -> blockPVP        = value;
            case MOB_DAMAGE -> blockMobDamage  = value;
            case ALERTS     -> trespasserAlerts = value;
        }
    }

    public void toggle(FlagId id) {
        set(id, !get(id));
    }

    public enum FlagId {
        BUILDING, BREAKING, EXPLOSIONS, FIRE, MOB_SPAWN, PVP, MOB_DAMAGE, ALERTS
    }
}
