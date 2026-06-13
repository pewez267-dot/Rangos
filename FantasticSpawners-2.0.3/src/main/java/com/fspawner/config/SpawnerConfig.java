// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.config;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import java.util.Iterator;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class SpawnerConfig
{
    public EntityMode entityMode;
    public final List<EntityEntry> entities;
    public int spawnDelayMin;
    public int spawnDelayMax;
    public int spawnCount;
    public int spawnRange;
    public int activationRange;
    public int maxNearbyEntities;
    public boolean waves;
    public boolean bossMode;
    public boolean continuous;
    public DayCycle dayCycle;
    public Weather weather;
    public int minLight;
    public int maxLight;
    public boolean requiresSky;
    public boolean requiresNoSky;
    public boolean requiresPlayer;
    public int extraCooldown;
    public final Map<String, Double> attributes;
    public final List<EquipmentEntry> equipment;
    public final List<EffectEntry> effects;
    public InfernalConfig infernal;
    public final List<DropEntry> drops;
    public boolean keepVanillaDrops;
    public String itemName;
    public String mobName;
    public boolean mobNameVisible;
    public boolean glowing;
    public String nameColor;
    public boolean particles;
    
    public SpawnerConfig() {
        this.entityMode = EntityMode.FIXED;
        this.entities = new ArrayList<EntityEntry>();
        this.spawnDelayMin = 200;
        this.spawnDelayMax = 800;
        this.spawnCount = 4;
        this.spawnRange = 4;
        this.activationRange = 16;
        this.maxNearbyEntities = 6;
        this.waves = false;
        this.bossMode = false;
        this.continuous = true;
        this.dayCycle = DayCycle.ANY;
        this.weather = Weather.ANY;
        this.minLight = 0;
        this.maxLight = 15;
        this.requiresSky = false;
        this.requiresNoSky = false;
        this.requiresPlayer = true;
        this.extraCooldown = 0;
        this.attributes = new LinkedHashMap<String, Double>();
        this.equipment = new ArrayList<EquipmentEntry>();
        this.effects = new ArrayList<EffectEntry>();
        this.infernal = new InfernalConfig();
        this.drops = new ArrayList<DropEntry>();
        this.keepVanillaDrops = true;
        this.itemName = "§d\u2726 Fantastic Spawner \u2726";
        this.mobName = "";
        this.mobNameVisible = false;
        this.glowing = false;
        this.nameColor = "white";
        this.particles = false;
        this.entities.add(new EntityEntry("minecraft:zombie"));
    }
    
    public String primaryEntityId() {
        return this.entities.isEmpty() ? "minecraft:pig" : this.entities.get(0).id;
    }
    
    public EquipmentEntry equipmentFor(final EquipmentSlot slot) {
        for (final EquipmentEntry e : this.equipment) {
            if (e.slot == slot) {
                return e;
            }
        }
        return null;
    }
    
    public SpawnerConfig copy() {
        return load(this.save());
    }
    
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("entityMode", this.entityMode.name());
        final ListTag entityList = new ListTag();
        for (final EntityEntry e : this.entities) {
            entityList.add((Object)e.save());
        }
        tag.put("entities", (Tag)entityList);
        final CompoundTag spawn = new CompoundTag();
        spawn.putInt("delayMin", this.spawnDelayMin);
        spawn.putInt("delayMax", this.spawnDelayMax);
        spawn.putInt("count", this.spawnCount);
        spawn.putInt("range", this.spawnRange);
        spawn.putInt("activationRange", this.activationRange);
        spawn.putInt("maxNearby", this.maxNearbyEntities);
        spawn.putBoolean("waves", this.waves);
        spawn.putBoolean("bossMode", this.bossMode);
        spawn.putBoolean("continuous", this.continuous);
        spawn.putString("dayCycle", this.dayCycle.name());
        spawn.putString("weather", this.weather.name());
        spawn.putInt("minLight", this.minLight);
        spawn.putInt("maxLight", this.maxLight);
        spawn.putBoolean("requiresSky", this.requiresSky);
        spawn.putBoolean("requiresNoSky", this.requiresNoSky);
        spawn.putBoolean("requiresPlayer", this.requiresPlayer);
        spawn.putInt("extraCooldown", this.extraCooldown);
        tag.put("spawn", (Tag)spawn);
        final CompoundTag attrs = new CompoundTag();
        for (final Map.Entry<String, Double> e2 : this.attributes.entrySet()) {
            attrs.putDouble((String)e2.getKey(), (double)e2.getValue());
        }
        tag.put("attributes", (Tag)attrs);
        final ListTag eqList = new ListTag();
        for (final EquipmentEntry e3 : this.equipment) {
            eqList.add((Object)e3.save());
        }
        tag.put("equipment", (Tag)eqList);
        final ListTag fxList = new ListTag();
        for (final EffectEntry e4 : this.effects) {
            fxList.add((Object)e4.save());
        }
        tag.put("effects", (Tag)fxList);
        tag.put("infernal", (Tag)this.infernal.save());
        final ListTag dropList = new ListTag();
        for (final DropEntry d : this.drops) {
            dropList.add((Object)d.save());
        }
        tag.put("drops", (Tag)dropList);
        tag.putBoolean("keepVanillaDrops", this.keepVanillaDrops);
        final CompoundTag ap = new CompoundTag();
        ap.putString("itemName", this.itemName);
        ap.putString("mobName", this.mobName);
        ap.putBoolean("mobNameVisible", this.mobNameVisible);
        ap.putBoolean("glowing", this.glowing);
        ap.putString("nameColor", this.nameColor);
        ap.putBoolean("particles", this.particles);
        tag.put("appearance", (Tag)ap);
        return tag;
    }
    
    public static SpawnerConfig load(final CompoundTag tag) {
        final SpawnerConfig cfg = new SpawnerConfig();
        cfg.entities.clear();
        cfg.attributes.clear();
        cfg.equipment.clear();
        cfg.effects.clear();
        cfg.drops.clear();
        try {
            cfg.entityMode = EntityMode.valueOf(tag.getString("entityMode"));
        }
        catch (final Exception ignored) {
            cfg.entityMode = EntityMode.FIXED;
        }
        final ListTag entityList = tag.getList("entities", 10);
        for (int i = 0; i < entityList.size(); ++i) {
            cfg.entities.add(EntityEntry.load(entityList.getCompound(i)));
        }
        if (cfg.entities.isEmpty()) {
            cfg.entities.add(new EntityEntry("minecraft:zombie"));
        }
        if (tag.contains("spawn")) {
            final CompoundTag spawn = tag.getCompound("spawn");
            cfg.spawnDelayMin = spawn.getInt("delayMin");
            cfg.spawnDelayMax = spawn.getInt("delayMax");
            cfg.spawnCount = spawn.getInt("count");
            cfg.spawnRange = spawn.getInt("range");
            cfg.activationRange = spawn.getInt("activationRange");
            cfg.maxNearbyEntities = spawn.getInt("maxNearby");
            cfg.waves = spawn.getBoolean("waves");
            cfg.bossMode = spawn.getBoolean("bossMode");
            cfg.continuous = spawn.getBoolean("continuous");
            try {
                cfg.dayCycle = DayCycle.valueOf(spawn.getString("dayCycle"));
            }
            catch (final Exception ex) {}
            try {
                cfg.weather = Weather.valueOf(spawn.getString("weather"));
            }
            catch (final Exception ex2) {}
            cfg.minLight = (spawn.contains("minLight") ? spawn.getInt("minLight") : 0);
            cfg.maxLight = (spawn.contains("maxLight") ? spawn.getInt("maxLight") : 15);
            cfg.requiresSky = spawn.getBoolean("requiresSky");
            cfg.requiresNoSky = spawn.getBoolean("requiresNoSky");
            cfg.requiresPlayer = (!spawn.contains("requiresPlayer") || spawn.getBoolean("requiresPlayer"));
            cfg.extraCooldown = (spawn.contains("extraCooldown") ? spawn.getInt("extraCooldown") : 0);
        }
        if (tag.contains("attributes")) {
            final CompoundTag attrs = tag.getCompound("attributes");
            for (final String key : attrs.getAllKeys()) {
                cfg.attributes.put(key, attrs.getDouble(key));
            }
        }
        final ListTag eqList = tag.getList("equipment", 10);
        for (int j = 0; j < eqList.size(); ++j) {
            cfg.equipment.add(EquipmentEntry.load(eqList.getCompound(j)));
        }
        final ListTag fxList = tag.getList("effects", 10);
        for (int k = 0; k < fxList.size(); ++k) {
            cfg.effects.add(EffectEntry.load(fxList.getCompound(k)));
        }
        if (tag.contains("infernal")) {
            cfg.infernal = InfernalConfig.load(tag.getCompound("infernal"));
        }
        final ListTag dropList = tag.getList("drops", 10);
        for (int l = 0; l < dropList.size(); ++l) {
            cfg.drops.add(DropEntry.load(dropList.getCompound(l)));
        }
        cfg.keepVanillaDrops = (!tag.contains("keepVanillaDrops") || tag.getBoolean("keepVanillaDrops"));
        if (tag.contains("appearance")) {
            final CompoundTag ap = tag.getCompound("appearance");
            cfg.itemName = ap.getString("itemName");
            cfg.mobName = ap.getString("mobName");
            cfg.mobNameVisible = ap.getBoolean("mobNameVisible");
            cfg.glowing = ap.getBoolean("glowing");
            cfg.nameColor = (ap.contains("nameColor") ? ap.getString("nameColor") : "white");
            cfg.particles = ap.getBoolean("particles");
        }
        if (cfg.itemName == null || cfg.itemName.isEmpty()) {
            cfg.itemName = "§d\u2726 Fantastic Spawner \u2726";
        }
        return cfg;
    }
    
    public enum EntityMode
    {
        FIXED, 
        POOL;
    }
    
    public enum DayCycle
    {
        ANY, 
        DAY_ONLY, 
        NIGHT_ONLY;
    }
    
    public enum Weather
    {
        ANY, 
        CLEAR, 
        RAIN, 
        THUNDER;
    }
}
