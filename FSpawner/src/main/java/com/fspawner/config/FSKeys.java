package com.fspawner.config;

/**
 * Central place for every NBT key used by FSpawner. Keeping them here avoids
 * typos and makes the serialization format easy to audit.
 */
public final class FSKeys {

    private FSKeys() {}

    // ---- ItemStack level ----
    /** Authoritative full config stored on the spawner ItemStack. */
    public static final String ITEM_CONFIG = "fspawner";
    /** Vanilla key copied into the BlockEntity when the item is placed. */
    public static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    // ---- ForgeData marker (travels on the entity NBT) ----
    public static final String FORGE_DATA = "ForgeData";
    public static final String MARKER = "fspawner";
    public static final String MARKER_CONFIG = "cfg";
    public static final String MARKER_INFERNAL = "infernal";
    public static final String MARKER_DROPS = "drops";
    public static final String MARKER_KEEP_VANILLA = "keepVanillaDrops";
    public static final String MARKER_APPEAR = "appearChances";

    // ---- SpawnerConfig top level ----
    public static final String ENTITY_MODE = "entityMode";
    public static final String ENTITIES = "entities";
    public static final String SPAWN = "spawn";
    public static final String ATTRIBUTES = "attributes";
    public static final String EQUIPMENT = "equipment";
    public static final String EFFECTS = "effects";
    public static final String INFERNAL = "infernal";
    public static final String DROPS = "drops";
    public static final String KEEP_VANILLA_DROPS = "keepVanillaDrops";
    public static final String APPEARANCE = "appearance";

    // ---- entity entry ----
    public static final String ENTITY_ID = "id";
    public static final String ENTITY_WEIGHT = "weight";

    // ---- spawn ----
    public static final String SPAWN_DELAY_MIN = "delayMin";
    public static final String SPAWN_DELAY_MAX = "delayMax";
    public static final String SPAWN_COUNT = "count";
    public static final String SPAWN_RANGE = "range";
    public static final String ACTIVATION_RANGE = "activationRange";
    public static final String MAX_NEARBY = "maxNearby";
    public static final String WAVES = "waves";
    public static final String BOSS_MODE = "bossMode";
    public static final String CONTINUOUS = "continuous";

    // ---- equipment entry ----
    public static final String EQ_SLOT = "slot";
    public static final String EQ_ITEM = "item";
    public static final String EQ_DROP_CHANCE = "dropChance";
    public static final String EQ_APPEAR_CHANCE = "appearChance";

    // ---- effect entry ----
    public static final String FX_ID = "id";
    public static final String FX_AMPLIFIER = "amplifier";
    public static final String FX_DURATION = "duration";
    public static final String FX_PERMANENT = "permanent";
    public static final String FX_AMBIENT = "ambient";
    public static final String FX_PARTICLES = "particles";

    // ---- infernal ----
    public static final String INF_MODE = "mode";
    public static final String INF_MODS = "mods";
    public static final String INF_MIN = "min";
    public static final String INF_MAX = "max";
    public static final String INF_POOL = "pool";

    // ---- drop entry ----
    public static final String DROP_ITEM = "item";
    public static final String DROP_MIN = "min";
    public static final String DROP_MAX = "max";
    public static final String DROP_CHANCE = "chance";

    // ---- appearance ----
    public static final String AP_ITEM_NAME = "itemName";
    public static final String AP_MOB_NAME = "mobName";
    public static final String AP_MOB_NAME_VISIBLE = "mobNameVisible";
    public static final String AP_GLOWING = "glowing";
    public static final String AP_NAME_COLOR = "nameColor";
    public static final String AP_PARTICLES = "particles";
}
