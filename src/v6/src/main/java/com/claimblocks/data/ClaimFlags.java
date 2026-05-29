/*
 * Decompiled with CFR 0.152.
 */
package com.claimblocks.data;

public class ClaimFlags {
    public boolean blockBuilding = true;
    public boolean blockBreaking = true;
    public boolean blockExplosions = true;
    public boolean blockFire = true;
    public boolean blockMobSpawn = false;
    public boolean blockPVP = true;
    public boolean blockMobDamage = false;
    public boolean trespasserAlerts = false;
    public boolean blockItemUse = true;
    public boolean blockEntityInteract = true;
    public boolean blockTrampling = true;
    public boolean blockFluids = true;
    public boolean pvpAll = false;
    public boolean blockTreeChopping = true;
    public boolean publicMode = false;
    public boolean showWelcome = false;
    public String welcomeMessage = "";
    public boolean effectRegeneration = false;
    public boolean effectResistance = false;
    public boolean effectSpeed = false;
    public boolean blockAnimalKilling = true;
    public boolean blockChestAccess = true;
    public boolean blockCropHarvest = true;
    public boolean blockAnvilUse = true;
    public boolean blockEnderPearl = true;
    public boolean blockSignEditing = true;
    public boolean allowFlight = false;
    public boolean blockDoorsAccess = true;

    public boolean get(FlagId id) {
        return switch (id.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> this.blockBuilding;
            case 1 -> this.blockBreaking;
            case 2 -> this.blockExplosions;
            case 3 -> this.blockFire;
            case 4 -> this.blockMobSpawn;
            case 5 -> this.blockPVP;
            case 6 -> this.blockMobDamage;
            case 7 -> this.trespasserAlerts;
            case 8 -> this.blockItemUse;
            case 9 -> this.blockEntityInteract;
            case 10 -> this.blockTrampling;
            case 11 -> this.blockFluids;
            case 12 -> this.pvpAll;
            case 13 -> this.blockTreeChopping;
            case 14 -> this.publicMode;
            case 15 -> this.showWelcome;
            case 16 -> this.effectRegeneration;
            case 17 -> this.effectResistance;
            case 18 -> this.effectSpeed;
            case 19 -> this.blockAnimalKilling;
            case 20 -> this.blockChestAccess;
            case 21 -> this.blockCropHarvest;
            case 22 -> this.blockAnvilUse;
            case 23 -> this.blockEnderPearl;
            case 24 -> this.blockSignEditing;
            case 25 -> this.allowFlight;
            case 26 -> this.blockDoorsAccess;
        };
    }

    public void set(FlagId id, boolean value) {
        switch (id.ordinal()) {
            case 0: {
                this.blockBuilding = value;
                break;
            }
            case 1: {
                this.blockBreaking = value;
                break;
            }
            case 2: {
                this.blockExplosions = value;
                break;
            }
            case 3: {
                this.blockFire = value;
                break;
            }
            case 4: {
                this.blockMobSpawn = value;
                break;
            }
            case 5: {
                this.blockPVP = value;
                break;
            }
            case 6: {
                this.blockMobDamage = value;
                break;
            }
            case 7: {
                this.trespasserAlerts = value;
                break;
            }
            case 8: {
                this.blockItemUse = value;
                break;
            }
            case 9: {
                this.blockEntityInteract = value;
                break;
            }
            case 10: {
                this.blockTrampling = value;
                break;
            }
            case 11: {
                this.blockFluids = value;
                break;
            }
            case 12: {
                this.pvpAll = value;
                break;
            }
            case 13: {
                this.blockTreeChopping = value;
                break;
            }
            case 14: {
                this.publicMode = value;
                break;
            }
            case 15: {
                this.showWelcome = value;
                break;
            }
            case 16: {
                this.effectRegeneration = value;
                break;
            }
            case 17: {
                this.effectResistance = value;
                break;
            }
            case 18: {
                this.effectSpeed = value;
                break;
            }
            case 19: {
                this.blockAnimalKilling = value;
                break;
            }
            case 20: {
                this.blockChestAccess = value;
                break;
            }
            case 21: {
                this.blockCropHarvest = value;
                break;
            }
            case 22: {
                this.blockAnvilUse = value;
                break;
            }
            case 23: {
                this.blockEnderPearl = value;
                break;
            }
            case 24: {
                this.blockSignEditing = value;
                break;
            }
            case 25: {
                this.allowFlight = value;
                break;
            }
            case 26: {
                this.blockDoorsAccess = value;
            }
        }
    }

    public void toggle(FlagId id) {
        this.set(id, !this.get(id));
    }

    public static boolean isPaidOnly(FlagId id) {
        return id == FlagId.EFFECT_REGEN || id == FlagId.EFFECT_RESIST || id == FlagId.EFFECT_SPEED || id == FlagId.ALLOW_FLIGHT;
    }

    public static enum FlagId {
        BUILDING,
        BREAKING,
        EXPLOSIONS,
        FIRE,
        MOB_SPAWN,
        PVP,
        MOB_DAMAGE,
        ALERTS,
        ITEM_USE,
        ENTITY_INTERACT,
        TRAMPLING,
        FLUIDS,
        PVP_ALL,
        TREE_CHOPPING,
        PUBLIC_MODE,
        SHOW_WELCOME,
        EFFECT_REGEN,
        EFFECT_RESIST,
        EFFECT_SPEED,
        ANIMAL_KILLING,
        CHEST_ACCESS,
        CROP_HARVEST,
        ANVIL_USE,
        ENDER_PEARL,
        SIGN_EDITING,
        ALLOW_FLIGHT,
        DOORS_ACCESS;

    }
}

