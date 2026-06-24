package com.fspawner.event;

import com.fspawner.config.DropEntry;
import com.fspawner.config.EquipmentEntry;
import com.fspawner.config.FSKeys;
import com.fspawner.config.InfernalConfig;
import com.fspawner.integration.InfernalMobsIntegration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Server-side gameplay glue. Reads the FSpawner marker carried in each spawned
 * entity's ForgeData to apply Infernal Mobs, roll equipment appearance and
 * inject custom drops.
 */
public final class FSpawnerEvents {

    private FSpawnerEvents() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        CompoundTag forgeData = entity.getPersistentData();
        if (!forgeData.contains(FSKeys.MARKER)) {
            return;
        }
        CompoundTag marker = forgeData.getCompound(FSKeys.MARKER);

        // 1. equipment appearance chance
        handleAppearChances(living, marker);

        // 2. Infernal Mobs modifiers
        if (marker.contains(FSKeys.MARKER_INFERNAL)) {
            InfernalConfig inf = InfernalConfig.load(marker.getCompound(FSKeys.MARKER_INFERNAL));
            if (inf.isEnabled()) {
                String mods = inf.resolveModifierString(living.getRandom());
                if (!mods.isBlank()) {
                    InfernalMobsIntegration.applyModifiers(living, mods);
                }
            }
        }

        // 3. slim the marker down so live entities only carry what death needs
        CompoundTag slim = new CompoundTag();
        boolean keepMarker = false;
        if (marker.contains(FSKeys.MARKER_DROPS)) {
            slim.put(FSKeys.MARKER_DROPS, marker.get(FSKeys.MARKER_DROPS));
            keepMarker = true;
        }
        if (marker.contains(FSKeys.MARKER_KEEP_VANILLA)) {
            slim.putBoolean(FSKeys.MARKER_KEEP_VANILLA, marker.getBoolean(FSKeys.MARKER_KEEP_VANILLA));
        }
        if (keepMarker) {
            forgeData.put(FSKeys.MARKER, slim);
        } else {
            forgeData.remove(FSKeys.MARKER);
        }
    }

    private static void handleAppearChances(LivingEntity living, CompoundTag marker) {
        if (!marker.contains(FSKeys.MARKER_APPEAR)) {
            return;
        }
        CompoundTag appear = marker.getCompound(FSKeys.MARKER_APPEAR);
        RandomSource random = living.getRandom();
        for (String slotName : appear.getAllKeys()) {
            float chance = appear.getFloat(slotName);
            if (random.nextFloat() > chance) {
                EquipmentSlot slot = EquipmentEntry.slotByName(slotName);
                living.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        CompoundTag forgeData = entity.getPersistentData();
        if (!forgeData.contains(FSKeys.MARKER)) {
            return;
        }
        CompoundTag marker = forgeData.getCompound(FSKeys.MARKER);

        boolean keepVanilla = !marker.contains(FSKeys.MARKER_KEEP_VANILLA)
                || marker.getBoolean(FSKeys.MARKER_KEEP_VANILLA);
        if (!keepVanilla) {
            event.getDrops().clear();
        }

        if (!marker.contains(FSKeys.MARKER_DROPS)) {
            return;
        }
        ListTag list = marker.getList(FSKeys.MARKER_DROPS, Tag.TAG_COMPOUND);
        RandomSource random = entity.getRandom();
        Level level = entity.level();

        for (int i = 0; i < list.size(); i++) {
            DropEntry drop = DropEntry.load(list.getCompound(i));
            if (drop.item.isEmpty()) {
                continue;
            }
            if (random.nextFloat() > drop.chance) {
                continue;
            }
            int min = Math.max(0, Math.min(drop.min, drop.max));
            int max = Math.max(drop.min, drop.max);
            int count = min + (max > min ? random.nextInt(max - min + 1) : 0);
            if (count <= 0) {
                continue;
            }
            spawnDrop(event, level, entity, drop.item, count);
        }
    }

    private static void spawnDrop(LivingDropsEvent event, Level level, LivingEntity entity,
                                  ItemStack template, int count) {
        int maxStack = template.getMaxStackSize();
        int remaining = count;
        while (remaining > 0) {
            int take = Math.min(remaining, maxStack);
            remaining -= take;
            ItemStack stack = template.copy();
            stack.setCount(take);
            ItemEntity itemEntity = new ItemEntity(level,
                    entity.getX(), entity.getY() + 0.5D, entity.getZ(), stack);
            itemEntity.setDefaultPickUpDelay();
            event.getDrops().add(itemEntity);
        }
    }
}
