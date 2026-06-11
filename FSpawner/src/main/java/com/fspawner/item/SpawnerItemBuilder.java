package com.fspawner.item;

import com.fspawner.config.EntityEntry;
import com.fspawner.config.FSKeys;
import com.fspawner.config.SpawnerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Builds and reads the Fantastic Spawner ItemStack. The result is a perfectly
 * normal {@code minecraft:spawner} stack whose NBT drives both the vanilla
 * spawn engine (via BlockEntityTag) and the FSpawner enhancements.
 */
public final class SpawnerItemBuilder {

    private SpawnerItemBuilder() {}

    /** Builds the configured spawner ItemStack from a config. */
    public static ItemStack build(SpawnerConfig cfg) {
        ItemStack stack = new ItemStack(Items.SPAWNER);

        // 1. authoritative config for tooltip / GUI / reference
        stack.getOrCreateTag().put(FSKeys.ITEM_CONFIG, cfg.save());

        // 2. vanilla BlockEntityTag so placement just works
        stack.getOrCreateTag().put(FSKeys.BLOCK_ENTITY_TAG, buildBlockEntityTag(cfg));

        // 3. custom display name
        String name = cfg.itemName == null || cfg.itemName.isEmpty()
                ? "\u00A7d\u2726 Fantastic Spawner \u2726" : cfg.itemName;
        stack.setHoverName(Component.literal(name));

        // 4. enchantment glint without showing an enchantment line
        EnchantmentHelper.setEnchantments(java.util.Map.of(Enchantments.UNBREAKING, 1), stack);
        stack.getOrCreateTag().putInt("HideFlags", 1); // hide "Enchantments"

        return stack;
    }

    /** Builds the vanilla spawner BlockEntity NBT (without x/y/z/id metadata). */
    public static CompoundTag buildBlockEntityTag(SpawnerConfig cfg) {
        CompoundTag tag = new CompoundTag();

        int maxNearby = cfg.bossMode ? 1 : Math.max(1, cfg.maxNearbyEntities);
        int spawnCount = cfg.bossMode ? 1 : Math.max(1, cfg.spawnCount);
        int minDelay = Math.max(0, Math.min(cfg.spawnDelayMin, cfg.spawnDelayMax));
        int maxDelay = Math.max(cfg.spawnDelayMin, cfg.spawnDelayMax);

        tag.putShort("Delay", (short) minDelay);
        tag.putShort("MinSpawnDelay", (short) minDelay);
        tag.putShort("MaxSpawnDelay", (short) Math.max(1, maxDelay));
        tag.putShort("SpawnCount", (short) spawnCount);
        tag.putShort("MaxNearbyEntities", (short) maxNearby);
        tag.putShort("RequiredPlayerRange", (short) Math.max(0, cfg.activationRange));
        tag.putShort("SpawnRange", (short) Math.max(1, cfg.spawnRange));

        // Build the weighted potentials list. The stored SpawnData carries the
        // full config so /fspawner pickup can rebuild the item losslessly.
        ListTag potentials = new ListTag();
        if (cfg.entityMode == SpawnerConfig.EntityMode.POOL && cfg.entities.size() > 1) {
            for (EntityEntry e : cfg.entities) {
                potentials.add(potentialEntry(cfg, e.id, e.weight, true));
            }
        } else {
            potentials.add(potentialEntry(cfg, cfg.primaryEntityId(), 1, true));
        }
        tag.put("SpawnPotentials", potentials);

        // The visible/default SpawnData (also used for the spinning preview mob).
        CompoundTag spawnData = new CompoundTag();
        spawnData.put("entity", EntityNbtBuilder.build(cfg, cfg.primaryEntityId(), true));
        tag.put("SpawnData", spawnData);

        return tag;
    }

    private static CompoundTag potentialEntry(SpawnerConfig cfg, String entityId, int weight, boolean includeFullConfig) {
        CompoundTag entry = new CompoundTag();
        entry.putInt("weight", Math.max(1, weight));
        CompoundTag data = new CompoundTag();
        data.put("entity", EntityNbtBuilder.build(cfg, entityId, includeFullConfig));
        entry.put("data", data);
        return entry;
    }

    // ------------------------------------------------------------------
    // Reading helpers
    // ------------------------------------------------------------------

    public static boolean isFantasticSpawner(ItemStack stack) {
        return stack != null
                && stack.is(Items.SPAWNER)
                && stack.hasTag()
                && stack.getTag().contains(FSKeys.ITEM_CONFIG);
    }

    /** Reads the authoritative config from a Fantastic Spawner item, or null. */
    public static SpawnerConfig readConfig(ItemStack stack) {
        if (!isFantasticSpawner(stack)) {
            return null;
        }
        return SpawnerConfig.load(stack.getTag().getCompound(FSKeys.ITEM_CONFIG));
    }

    /**
     * Rebuilds the item from a placed spawner BlockEntity NBT by extracting the
     * embedded config (ForgeData.fspawner.cfg) from its SpawnData.
     * Returns null if the block is not a Fantastic Spawner.
     */
    public static ItemStack fromBlockEntityNbt(CompoundTag beTag) {
        CompoundTag cfgTag = extractEmbeddedConfig(beTag);
        if (cfgTag == null) {
            return null;
        }
        return build(SpawnerConfig.load(cfgTag));
    }

    /** Returns true if a placed spawner BlockEntity NBT was created by FSpawner. */
    public static boolean isFantasticSpawnerBlock(CompoundTag beTag) {
        return extractEmbeddedConfig(beTag) != null;
    }

    private static CompoundTag extractEmbeddedConfig(CompoundTag beTag) {
        if (beTag == null) {
            return null;
        }
        CompoundTag entity = null;
        if (beTag.contains("SpawnData")) {
            entity = beTag.getCompound("SpawnData").getCompound("entity");
        }
        if ((entity == null || entity.isEmpty()) && beTag.contains("SpawnPotentials")) {
            ListTag list = beTag.getList("SpawnPotentials", net.minecraft.nbt.Tag.TAG_COMPOUND);
            if (!list.isEmpty()) {
                entity = list.getCompound(0).getCompound("data").getCompound("entity");
            }
        }
        if (entity == null || entity.isEmpty()) {
            return null;
        }
        CompoundTag forgeData = entity.getCompound(FSKeys.FORGE_DATA);
        CompoundTag marker = forgeData.getCompound(FSKeys.MARKER);
        if (marker.contains(FSKeys.MARKER_CONFIG)) {
            return marker.getCompound(FSKeys.MARKER_CONFIG);
        }
        return null;
    }
}
