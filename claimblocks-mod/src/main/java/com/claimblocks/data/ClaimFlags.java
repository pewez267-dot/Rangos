package com.claimblocks.data;

/**
 * 26 protection flags. Boolean flags follow the spec naming exactly.
 * The welcome-message string lives here too because it's part of the
 * "showWelcome" feature.
 *
 * Default values: most "block*" flags ON; mob spawning/damage and alerts
 * OFF; pvpAll/publicMode/showWelcome OFF; effect/perk flags (REGEN/RESIST/
 * SPEED/FLIGHT) OFF.
 */
public class ClaimFlags {
    // 8 originales (v2.2)
    public boolean blockBuilding    = true;
    public boolean blockBreaking    = true;
    public boolean blockExplosions  = true;
    public boolean blockFire        = true;
    public boolean blockMobSpawn    = false;
    public boolean blockPVP         = true;
    public boolean blockMobDamage   = false;
    public boolean trespasserAlerts = false;

    // 8 nuevas (v3.0)
    public boolean blockItemUse        = true;
    public boolean blockEntityInteract = true;
    public boolean blockTrampling      = true;
    public boolean blockFluids         = true;
    public boolean pvpAll              = false;
    public boolean blockTreeChopping   = true;
    public boolean publicMode          = false;
    public boolean showWelcome         = false;
    public String  welcomeMessage      = "";

    // 3 effect flags (v4.0) - paid tiers only
    public boolean effectRegeneration  = false;
    public boolean effectResistance    = false;
    public boolean effectSpeed         = false;

    // 6 nuevas v5.0 + 1 paid-only (allowFlight)
    public boolean blockAnimalKilling = true;
    public boolean blockChestAccess   = true;
    public boolean blockCropHarvest   = true;
    public boolean blockAnvilUse      = true;
    public boolean blockEnderPearl    = true;
    public boolean blockSignEditing   = true;
    public boolean allowFlight        = false; // paid-only

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
            case ITEM_USE        -> blockItemUse;
            case ENTITY_INTERACT -> blockEntityInteract;
            case TRAMPLING       -> blockTrampling;
            case FLUIDS          -> blockFluids;
            case PVP_ALL         -> pvpAll;
            case TREE_CHOPPING   -> blockTreeChopping;
            case PUBLIC_MODE     -> publicMode;
            case SHOW_WELCOME    -> showWelcome;
            case EFFECT_REGEN    -> effectRegeneration;
            case EFFECT_RESIST   -> effectResistance;
            case EFFECT_SPEED    -> effectSpeed;
            case ANIMAL_KILLING  -> blockAnimalKilling;
            case CHEST_ACCESS    -> blockChestAccess;
            case CROP_HARVEST    -> blockCropHarvest;
            case ANVIL_USE       -> blockAnvilUse;
            case ENDER_PEARL     -> blockEnderPearl;
            case SIGN_EDITING    -> blockSignEditing;
            case ALLOW_FLIGHT    -> allowFlight;
        };
    }

    public void set(FlagId id, boolean value) {
        switch (id) {
            case BUILDING        -> blockBuilding    = value;
            case BREAKING        -> blockBreaking    = value;
            case EXPLOSIONS      -> blockExplosions  = value;
            case FIRE            -> blockFire        = value;
            case MOB_SPAWN       -> blockMobSpawn    = value;
            case PVP             -> blockPVP         = value;
            case MOB_DAMAGE      -> blockMobDamage   = value;
            case ALERTS          -> trespasserAlerts = value;
            case ITEM_USE        -> blockItemUse        = value;
            case ENTITY_INTERACT -> blockEntityInteract = value;
            case TRAMPLING       -> blockTrampling      = value;
            case FLUIDS          -> blockFluids         = value;
            case PVP_ALL         -> pvpAll              = value;
            case TREE_CHOPPING   -> blockTreeChopping   = value;
            case PUBLIC_MODE     -> publicMode          = value;
            case SHOW_WELCOME    -> showWelcome         = value;
            case EFFECT_REGEN    -> effectRegeneration  = value;
            case EFFECT_RESIST   -> effectResistance    = value;
            case EFFECT_SPEED    -> effectSpeed         = value;
            case ANIMAL_KILLING  -> blockAnimalKilling  = value;
            case CHEST_ACCESS    -> blockChestAccess    = value;
            case CROP_HARVEST    -> blockCropHarvest    = value;
            case ANVIL_USE       -> blockAnvilUse       = value;
            case ENDER_PEARL     -> blockEnderPearl     = value;
            case SIGN_EDITING    -> blockSignEditing    = value;
            case ALLOW_FLIGHT    -> allowFlight         = value;
        }
    }

    public void toggle(FlagId id) { set(id, !get(id)); }

    /** True when this flag id is one of the paid-tier-only perks. */
    public static boolean isPaidOnly(FlagId id) {
        return id == FlagId.EFFECT_REGEN
            || id == FlagId.EFFECT_RESIST
            || id == FlagId.EFFECT_SPEED
            || id == FlagId.ALLOW_FLIGHT;
    }

    public enum FlagId {
        BUILDING, BREAKING, EXPLOSIONS, FIRE, MOB_SPAWN, PVP, MOB_DAMAGE, ALERTS,
        ITEM_USE, ENTITY_INTERACT, TRAMPLING, FLUIDS, PVP_ALL, TREE_CHOPPING,
        PUBLIC_MODE, SHOW_WELCOME, EFFECT_REGEN, EFFECT_RESIST, EFFECT_SPEED,
        ANIMAL_KILLING, CHEST_ACCESS, CROP_HARVEST, ANVIL_USE, ENDER_PEARL,
        SIGN_EDITING, ALLOW_FLIGHT
    }
}
