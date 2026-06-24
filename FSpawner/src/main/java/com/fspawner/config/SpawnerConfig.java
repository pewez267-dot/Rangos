package com.fspawner.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The full, authoritative description of a Fantastic Spawner. Everything the
 * GUI edits lives here and the whole object round-trips to/from NBT so it can
 * be stored inside an ItemStack and inside a placed BlockEntity.
 */
public class SpawnerConfig {

    public enum EntityMode { FIXED, POOL }

    // ---- Entities ----
    public EntityMode entityMode = EntityMode.FIXED;
    public final List<EntityEntry> entities = new ArrayList<>();

    // ---- Spawn ----
    public int spawnDelayMin = 200;
    public int spawnDelayMax = 800;
    public int spawnCount = 4;
    public int spawnRange = 4;
    public int activationRange = 16;
    public int maxNearbyEntities = 6;
    public boolean waves = false;
    public boolean bossMode = false;
    public boolean continuous = true;

    // ---- Attributes (registry id -> base value) ----
    public final Map<String, Double> attributes = new LinkedHashMap<>();

    // ---- Equipment ----
    public final List<EquipmentEntry> equipment = new ArrayList<>();

    // ---- Effects ----
    public final List<EffectEntry> effects = new ArrayList<>();

    // ---- Infernal ----
    public InfernalConfig infernal = new InfernalConfig();

    // ---- Drops ----
    public final List<DropEntry> drops = new ArrayList<>();
    public boolean keepVanillaDrops = true;

    // ---- Appearance ----
    public String itemName = "\u00A7d\u2726 Fantastic Spawner \u2726";
    public String mobName = "";
    public boolean mobNameVisible = false;
    public boolean glowing = false;
    public String nameColor = "white";
    public boolean particles = false;

    public SpawnerConfig() {
        entities.add(new EntityEntry("minecraft:zombie"));
    }

    /** The primary entity id (first entry) or pig as a safe fallback. */
    public String primaryEntityId() {
        return entities.isEmpty() ? "minecraft:pig" : entities.get(0).id;
    }

    // ------------------------------------------------------------------
    // Serialization
    // ------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(FSKeys.ENTITY_MODE, entityMode.name());

        ListTag entityList = new ListTag();
        for (EntityEntry e : entities) {
            entityList.add(e.save());
        }
        tag.put(FSKeys.ENTITIES, entityList);

        CompoundTag spawn = new CompoundTag();
        spawn.putInt(FSKeys.SPAWN_DELAY_MIN, spawnDelayMin);
        spawn.putInt(FSKeys.SPAWN_DELAY_MAX, spawnDelayMax);
        spawn.putInt(FSKeys.SPAWN_COUNT, spawnCount);
        spawn.putInt(FSKeys.SPAWN_RANGE, spawnRange);
        spawn.putInt(FSKeys.ACTIVATION_RANGE, activationRange);
        spawn.putInt(FSKeys.MAX_NEARBY, maxNearbyEntities);
        spawn.putBoolean(FSKeys.WAVES, waves);
        spawn.putBoolean(FSKeys.BOSS_MODE, bossMode);
        spawn.putBoolean(FSKeys.CONTINUOUS, continuous);
        tag.put(FSKeys.SPAWN, spawn);

        CompoundTag attrs = new CompoundTag();
        for (Map.Entry<String, Double> e : attributes.entrySet()) {
            attrs.putDouble(e.getKey(), e.getValue());
        }
        tag.put(FSKeys.ATTRIBUTES, attrs);

        ListTag eqList = new ListTag();
        for (EquipmentEntry e : equipment) {
            eqList.add(e.save());
        }
        tag.put(FSKeys.EQUIPMENT, eqList);

        ListTag fxList = new ListTag();
        for (EffectEntry e : effects) {
            fxList.add(e.save());
        }
        tag.put(FSKeys.EFFECTS, fxList);

        tag.put(FSKeys.INFERNAL, infernal.save());

        ListTag dropList = new ListTag();
        for (DropEntry d : drops) {
            dropList.add(d.save());
        }
        tag.put(FSKeys.DROPS, dropList);
        tag.putBoolean(FSKeys.KEEP_VANILLA_DROPS, keepVanillaDrops);

        CompoundTag ap = new CompoundTag();
        ap.putString(FSKeys.AP_ITEM_NAME, itemName);
        ap.putString(FSKeys.AP_MOB_NAME, mobName);
        ap.putBoolean(FSKeys.AP_MOB_NAME_VISIBLE, mobNameVisible);
        ap.putBoolean(FSKeys.AP_GLOWING, glowing);
        ap.putString(FSKeys.AP_NAME_COLOR, nameColor);
        ap.putBoolean(FSKeys.AP_PARTICLES, particles);
        tag.put(FSKeys.APPEARANCE, ap);

        return tag;
    }

    public static SpawnerConfig load(CompoundTag tag) {
        SpawnerConfig cfg = new SpawnerConfig();
        cfg.entities.clear();
        cfg.attributes.clear();
        cfg.equipment.clear();
        cfg.effects.clear();
        cfg.drops.clear();

        try {
            cfg.entityMode = EntityMode.valueOf(tag.getString(FSKeys.ENTITY_MODE));
        } catch (IllegalArgumentException ignored) {
            cfg.entityMode = EntityMode.FIXED;
        }

        ListTag entityList = tag.getList(FSKeys.ENTITIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < entityList.size(); i++) {
            cfg.entities.add(EntityEntry.load(entityList.getCompound(i)));
        }
        if (cfg.entities.isEmpty()) {
            cfg.entities.add(new EntityEntry("minecraft:zombie"));
        }

        if (tag.contains(FSKeys.SPAWN)) {
            CompoundTag spawn = tag.getCompound(FSKeys.SPAWN);
            cfg.spawnDelayMin = spawn.getInt(FSKeys.SPAWN_DELAY_MIN);
            cfg.spawnDelayMax = spawn.getInt(FSKeys.SPAWN_DELAY_MAX);
            cfg.spawnCount = spawn.getInt(FSKeys.SPAWN_COUNT);
            cfg.spawnRange = spawn.getInt(FSKeys.SPAWN_RANGE);
            cfg.activationRange = spawn.getInt(FSKeys.ACTIVATION_RANGE);
            cfg.maxNearbyEntities = spawn.getInt(FSKeys.MAX_NEARBY);
            cfg.waves = spawn.getBoolean(FSKeys.WAVES);
            cfg.bossMode = spawn.getBoolean(FSKeys.BOSS_MODE);
            cfg.continuous = spawn.getBoolean(FSKeys.CONTINUOUS);
        }

        if (tag.contains(FSKeys.ATTRIBUTES)) {
            CompoundTag attrs = tag.getCompound(FSKeys.ATTRIBUTES);
            for (String key : attrs.getAllKeys()) {
                cfg.attributes.put(key, attrs.getDouble(key));
            }
        }

        ListTag eqList = tag.getList(FSKeys.EQUIPMENT, Tag.TAG_COMPOUND);
        for (int i = 0; i < eqList.size(); i++) {
            cfg.equipment.add(EquipmentEntry.load(eqList.getCompound(i)));
        }

        ListTag fxList = tag.getList(FSKeys.EFFECTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < fxList.size(); i++) {
            cfg.effects.add(EffectEntry.load(fxList.getCompound(i)));
        }

        if (tag.contains(FSKeys.INFERNAL)) {
            cfg.infernal = InfernalConfig.load(tag.getCompound(FSKeys.INFERNAL));
        }

        ListTag dropList = tag.getList(FSKeys.DROPS, Tag.TAG_COMPOUND);
        for (int i = 0; i < dropList.size(); i++) {
            cfg.drops.add(DropEntry.load(dropList.getCompound(i)));
        }
        cfg.keepVanillaDrops = !tag.contains(FSKeys.KEEP_VANILLA_DROPS) || tag.getBoolean(FSKeys.KEEP_VANILLA_DROPS);

        if (tag.contains(FSKeys.APPEARANCE)) {
            CompoundTag ap = tag.getCompound(FSKeys.APPEARANCE);
            cfg.itemName = ap.getString(FSKeys.AP_ITEM_NAME);
            cfg.mobName = ap.getString(FSKeys.AP_MOB_NAME);
            cfg.mobNameVisible = ap.getBoolean(FSKeys.AP_MOB_NAME_VISIBLE);
            cfg.glowing = ap.getBoolean(FSKeys.AP_GLOWING);
            cfg.nameColor = ap.contains(FSKeys.AP_NAME_COLOR) ? ap.getString(FSKeys.AP_NAME_COLOR) : "white";
            cfg.particles = ap.getBoolean(FSKeys.AP_PARTICLES);
        }
        if (cfg.itemName == null || cfg.itemName.isEmpty()) {
            cfg.itemName = "\u00A7d\u2726 Fantastic Spawner \u2726";
        }
        return cfg;
    }

    public EquipmentEntry equipmentFor(EquipmentSlot slot) {
        for (EquipmentEntry e : equipment) {
            if (e.slot == slot) {
                return e;
            }
        }
        return null;
    }

    public SpawnerConfig copy() {
        return load(this.save());
    }
}
