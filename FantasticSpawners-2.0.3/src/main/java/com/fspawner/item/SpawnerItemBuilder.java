// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.item;

import java.util.Iterator;
import com.fspawner.config.EntityEntry;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.util.Map;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import com.fspawner.config.SpawnerConfig;

public final class SpawnerItemBuilder
{
    private SpawnerItemBuilder() {
    }
    
    public static ItemStack build(final SpawnerConfig cfg) {
        final ItemStack stack = new ItemStack((ItemLike)Items.SPAWNER);
        stack.getOrCreateTag().put("fspawner", (Tag)cfg.save());
        stack.getOrCreateTag().put("BlockEntityTag", (Tag)buildBlockEntityTag(cfg));
        final String name = (cfg.itemName == null || cfg.itemName.isEmpty()) ? "§d\u2726 Fantastic Spawner \u2726" : cfg.itemName;
        stack.setHoverName((Component)Component.literal(name));
        EnchantmentHelper.setEnchantments((Map)Map.of(Enchantments.UNBREAKING, 1), stack);
        stack.getOrCreateTag().putInt("HideFlags", 1);
        return stack;
    }
    
    public static CompoundTag buildBlockEntityTag(final SpawnerConfig cfg) {
        final CompoundTag tag = new CompoundTag();
        final int maxNearby = cfg.bossMode ? 1 : Math.max(1, cfg.maxNearbyEntities);
        final int spawnCount = cfg.bossMode ? 1 : Math.max(1, cfg.spawnCount);
        final int minDelay = Math.max(0, Math.min(cfg.spawnDelayMin, cfg.spawnDelayMax)) + Math.max(0, cfg.extraCooldown);
        final int maxDelay = Math.max(cfg.spawnDelayMin, cfg.spawnDelayMax) + Math.max(0, cfg.extraCooldown);
        tag.putShort("Delay", (short)minDelay);
        tag.putShort("MinSpawnDelay", (short)minDelay);
        tag.putShort("MaxSpawnDelay", (short)Math.max(1, maxDelay));
        tag.putShort("SpawnCount", (short)spawnCount);
        tag.putShort("MaxNearbyEntities", (short)maxNearby);
        tag.putShort("RequiredPlayerRange", (short)(cfg.requiresPlayer ? Math.max(0, cfg.activationRange) : 0));
        tag.putShort("SpawnRange", (short)Math.max(1, cfg.spawnRange));
        final ListTag potentials = new ListTag();
        if (cfg.entityMode == SpawnerConfig.EntityMode.POOL && cfg.entities.size() > 1) {
            for (final EntityEntry e : cfg.entities) {
                potentials.add(potentialEntry(cfg, e.id, e.weight, true));
            }
        }
        else {
            potentials.add(potentialEntry(cfg, cfg.primaryEntityId(), 1, true));
        }
        tag.put("SpawnPotentials", (Tag)potentials);
        final CompoundTag spawnData = new CompoundTag();
        spawnData.put("entity", (Tag)EntityNbtBuilder.build(cfg, cfg.primaryEntityId(), true));
        spawnData.put("custom_spawn_rules", (Tag)customSpawnRules());
        tag.put("SpawnData", (Tag)spawnData);
        final CompoundTag forgeData = tag.contains("ForgeData") ? tag.getCompound("ForgeData") : new CompoundTag();
        final CompoundTag marker = new CompoundTag();
        marker.put("cfg", (Tag)cfg.save());
        forgeData.put("fspawner", (Tag)marker);
        tag.put("ForgeData", (Tag)forgeData);
        return tag;
    }
    
    private static CompoundTag customSpawnRules() {
        return new CompoundTag();
    }
    
    private static CompoundTag potentialEntry(final SpawnerConfig cfg, final String entityId, final int weight, final boolean includeFullConfig) {
        final CompoundTag entry = new CompoundTag();
        entry.putInt("weight", Math.max(1, weight));
        final CompoundTag data = new CompoundTag();
        data.put("entity", (Tag)EntityNbtBuilder.build(cfg, entityId, includeFullConfig));
        data.put("custom_spawn_rules", (Tag)customSpawnRules());
        entry.put("data", (Tag)data);
        return entry;
    }
    
    public static boolean isFantasticSpawner(final ItemStack stack) {
        return stack != null && stack.is(Items.SPAWNER) && stack.hasTag() && stack.getTag().contains("fspawner");
    }
    
    public static SpawnerConfig readConfig(final ItemStack stack) {
        if (!isFantasticSpawner(stack)) {
            return null;
        }
        return SpawnerConfig.load(stack.getTag().getCompound("fspawner"));
    }
    
    public static ItemStack fromBlockEntityNbt(final CompoundTag beTag) {
        final CompoundTag cfgTag = extractConfigForEditing(beTag);
        if (cfgTag == null) {
            return null;
        }
        return build(SpawnerConfig.load(cfgTag));
    }
    
    public static boolean isFantasticSpawnerBlock(final CompoundTag beTag) {
        return extractConfigForEditing(beTag) != null;
    }
    
    public static CompoundTag extractConfigForEditing(final CompoundTag beTag) {
        if (beTag == null) {
            return null;
        }
        if (beTag.contains("ForgeData")) {
            final CompoundTag marker = beTag.getCompound("ForgeData").getCompound("fspawner");
            if (marker.contains("cfg")) {
                final CompoundTag cfg = marker.getCompound("cfg");
                if (!cfg.isEmpty()) {
                    return cfg;
                }
            }
        }
        CompoundTag entity = null;
        if (beTag.contains("SpawnData")) {
            entity = beTag.getCompound("SpawnData").getCompound("entity");
        }
        if ((entity == null || entity.isEmpty()) && beTag.contains("SpawnPotentials")) {
            final ListTag list = beTag.getList("SpawnPotentials", 10);
            if (!list.isEmpty()) {
                entity = list.getCompound(0).getCompound("data").getCompound("entity");
            }
        }
        if (entity != null && !entity.isEmpty()) {
            final CompoundTag forgeData = entity.getCompound("ForgeData");
            final CompoundTag marker2 = forgeData.getCompound("fspawner");
            if (marker2.contains("cfg")) {
                return marker2.getCompound("cfg");
            }
        }
        final SpawnerConfig fallback = new SpawnerConfig();
        fallback.entities.clear();
        if (entity != null && entity.contains("id")) {
            fallback.entities.add(new EntityEntry(entity.getString("id")));
        }
        else {
            fallback.entities.add(new EntityEntry("minecraft:zombie"));
        }
        if (beTag.contains("MinSpawnDelay")) {
            fallback.spawnDelayMin = beTag.getShort("MinSpawnDelay");
        }
        if (beTag.contains("MaxSpawnDelay")) {
            fallback.spawnDelayMax = beTag.getShort("MaxSpawnDelay");
        }
        if (beTag.contains("SpawnCount")) {
            fallback.spawnCount = beTag.getShort("SpawnCount");
        }
        if (beTag.contains("SpawnRange")) {
            fallback.spawnRange = beTag.getShort("SpawnRange");
        }
        if (beTag.contains("MaxNearbyEntities")) {
            fallback.maxNearbyEntities = beTag.getShort("MaxNearbyEntities");
        }
        if (beTag.contains("RequiredPlayerRange")) {
            fallback.activationRange = beTag.getShort("RequiredPlayerRange");
        }
        return fallback.save();
    }
}
