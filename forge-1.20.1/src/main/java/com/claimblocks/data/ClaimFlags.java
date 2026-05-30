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
        return switch (id) {
            case BUILDING -> this.blockBuilding;
            case BREAKING -> this.blockBreaking;
            case EXPLOSIONS -> this.blockExplosions;
            case FIRE -> this.blockFire;
            case MOB_SPAWN -> this.blockMobSpawn;
            case PVP -> this.blockPVP;
            case MOB_DAMAGE -> this.blockMobDamage;
            case ALERTS -> this.trespasserAlerts;
            case ITEM_USE -> this.blockItemUse;
            case ENTITY_INTERACT -> this.blockEntityInteract;
            case TRAMPLING -> this.blockTrampling;
            case FLUIDS -> this.blockFluids;
            case PVP_ALL -> this.pvpAll;
            case TREE_CHOPPING -> this.blockTreeChopping;
            case PUBLIC_MODE -> this.publicMode;
            case SHOW_WELCOME -> this.showWelcome;
            case EFFECT_REGEN -> this.effectRegeneration;
            case EFFECT_RESIST -> this.effectResistance;
            case EFFECT_SPEED -> this.effectSpeed;
            case ANIMAL_KILLING -> this.blockAnimalKilling;
            case CHEST_ACCESS -> this.blockChestAccess;
            case CROP_HARVEST -> this.blockCropHarvest;
            case ANVIL_USE -> this.blockAnvilUse;
            case ENDER_PEARL -> this.blockEnderPearl;
            case SIGN_EDITING -> this.blockSignEditing;
            case ALLOW_FLIGHT -> this.allowFlight;
            case DOORS_ACCESS -> this.blockDoorsAccess;
        };
    }

    public void set(FlagId id, boolean value) {
        switch (id) {
            case BUILDING -> this.blockBuilding = value;
            case BREAKING -> this.blockBreaking = value;
            case EXPLOSIONS -> this.blockExplosions = value;
            case FIRE -> this.blockFire = value;
            case MOB_SPAWN -> this.blockMobSpawn = value;
            case PVP -> this.blockPVP = value;
            case MOB_DAMAGE -> this.blockMobDamage = value;
            case ALERTS -> this.trespasserAlerts = value;
            case ITEM_USE -> this.blockItemUse = value;
            case ENTITY_INTERACT -> this.blockEntityInteract = value;
            case TRAMPLING -> this.blockTrampling = value;
            case FLUIDS -> this.blockFluids = value;
            case PVP_ALL -> this.pvpAll = value;
            case TREE_CHOPPING -> this.blockTreeChopping = value;
            case PUBLIC_MODE -> this.publicMode = value;
            case SHOW_WELCOME -> this.showWelcome = value;
            case EFFECT_REGEN -> this.effectRegeneration = value;
            case EFFECT_RESIST -> this.effectResistance = value;
            case EFFECT_SPEED -> this.effectSpeed = value;
            case ANIMAL_KILLING -> this.blockAnimalKilling = value;
            case CHEST_ACCESS -> this.blockChestAccess = value;
            case CROP_HARVEST -> this.blockCropHarvest = value;
            case ANVIL_USE -> this.blockAnvilUse = value;
            case ENDER_PEARL -> this.blockEnderPearl = value;
            case SIGN_EDITING -> this.blockSignEditing = value;
            case ALLOW_FLIGHT -> this.allowFlight = value;
            case DOORS_ACCESS -> this.blockDoorsAccess = value;
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
