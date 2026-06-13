// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.event;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraft.nbt.ListTag;
import com.fspawner.config.DropEntry;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.Iterator;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import com.fspawner.config.EquipmentEntry;
import net.minecraft.nbt.Tag;
import com.fspawner.integration.InfernalMobsIntegration;
import com.fspawner.config.InfernalConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import com.fspawner.config.SpawnerConfig;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;

public final class FSpawnerEvents
{
    private FSpawnerEvents() {
    }
    
    @SubscribeEvent
    public static void onCheckSpawn(final MobSpawnEvent.FinalizeSpawn event) {
        if (event.getSpawnType() != MobSpawnType.SPAWNER) {
            return;
        }
        final Entity entity = (Entity)event.getEntity();
        if (entity == null) {
            return;
        }
        final CompoundTag forgeData = entity.getPersistentData();
        if (!forgeData.contains("fspawner")) {
            return;
        }
        final CompoundTag marker = forgeData.getCompound("fspawner");
        if (!marker.contains("cfg")) {
            return;
        }
        final SpawnerConfig cfg = SpawnerConfig.load(marker.getCompound("cfg"));
        final LevelAccessor level = (LevelAccessor)event.getLevel();
        final BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        if (!matchesDayCycle(level, cfg) || !matchesWeather(level, cfg) || !matchesSky(level, pos, cfg) || !matchesLight(level, pos, cfg)) {
            event.setSpawnCancelled(true);
        }
    }
    
    private static boolean matchesDayCycle(final LevelAccessor level, final SpawnerConfig cfg) {
        if (cfg.dayCycle == SpawnerConfig.DayCycle.ANY) {
            return true;
        }
        if (level instanceof final Level lvl) {
            final long time = lvl.getDayTime() % 24000L;
            final boolean isDay = time < 13000L || time > 23000L;
            return (cfg.dayCycle == SpawnerConfig.DayCycle.DAY_ONLY) ? isDay : (!isDay);
        }
        return true;
    }
    
    private static boolean matchesWeather(final LevelAccessor level, final SpawnerConfig cfg) {
        if (cfg.weather == SpawnerConfig.Weather.ANY) {
            return true;
        }
        if (level instanceof final Level lvl) {
            final boolean thunder = lvl.isThundering();
            final boolean rain = lvl.isRaining();
            return switch (cfg.weather) {
                case CLEAR -> !rain && !thunder;
                case RAIN -> rain && !thunder;
                case THUNDER -> thunder;
                default -> true;
            };
        }
        return true;
    }
    
    private static boolean matchesSky(final LevelAccessor level, final BlockPos pos, final SpawnerConfig cfg) {
        if (!cfg.requiresSky && !cfg.requiresNoSky) {
            return true;
        }
        final boolean canSee = level.canSeeSky(pos);
        return (!cfg.requiresSky || canSee) && (!cfg.requiresNoSky || !canSee);
    }
    
    private static boolean matchesLight(final LevelAccessor level, final BlockPos pos, final SpawnerConfig cfg) {
        if (cfg.minLight <= 0 && cfg.maxLight >= 15) {
            return true;
        }
        final int light = level.getBrightness(LightLayer.BLOCK, pos);
        return light >= cfg.minLight && light <= cfg.maxLight;
    }
    
    @SubscribeEvent
    public static void onEntityJoin(final EntityJoinLevelEvent event) {
        final Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        final Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        final LivingEntity living = (LivingEntity)entity;
        final CompoundTag forgeData = entity.getPersistentData();
        if (!forgeData.contains("fspawner")) {
            return;
        }
        final CompoundTag marker = forgeData.getCompound("fspawner");
        handleAppearChances(living, marker);
        if (marker.contains("infernal")) {
            final InfernalConfig inf = InfernalConfig.load(marker.getCompound("infernal"));
            if (inf.isEnabled()) {
                final String mods = inf.resolveModifierString(living.getRandom());
                if (!mods.isBlank()) {
                    InfernalMobsIntegration.applyModifiers(living, mods);
                }
            }
        }
        final CompoundTag slim = new CompoundTag();
        boolean keepMarker = false;
        if (marker.contains("drops")) {
            slim.put("drops", marker.get("drops"));
            keepMarker = true;
        }
        if (marker.contains("keepVanillaDrops")) {
            slim.putBoolean("keepVanillaDrops", marker.getBoolean("keepVanillaDrops"));
            keepMarker = true;
        }
        if (keepMarker) {
            forgeData.put("fspawner", (Tag)slim);
        }
        else {
            forgeData.remove("fspawner");
        }
    }
    
    private static void handleAppearChances(final LivingEntity living, final CompoundTag marker) {
        if (!marker.contains("appearChances")) {
            return;
        }
        final CompoundTag appear = marker.getCompound("appearChances");
        final RandomSource random = living.getRandom();
        for (final String slotName : appear.getAllKeys()) {
            final float chance = appear.getFloat(slotName);
            if (random.nextFloat() > chance) {
                final EquipmentSlot slot = EquipmentEntry.slotByName(slotName);
                living.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(final LivingDropsEvent event) {
        final LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        final CompoundTag forgeData = entity.getPersistentData();
        if (!forgeData.contains("fspawner")) {
            return;
        }
        final CompoundTag marker = forgeData.getCompound("fspawner");
        final boolean keepVanilla = !marker.contains("keepVanillaDrops") || marker.getBoolean("keepVanillaDrops");
        if (!keepVanilla) {
            event.getDrops().clear();
        }
        if (!marker.contains("drops")) {
            return;
        }
        final ListTag list = marker.getList("drops", 10);
        final RandomSource random = entity.getRandom();
        final Level level = entity.level();
        for (int i = 0; i < list.size(); ++i) {
            final DropEntry drop = DropEntry.load(list.getCompound(i));
            if (!drop.item.isEmpty()) {
                if (random.nextFloat() <= drop.chance) {
                    final int min = Math.max(0, Math.min(drop.min, drop.max));
                    final int max = Math.max(drop.min, drop.max);
                    final int count = min + ((max > min) ? random.nextInt(max - min + 1) : 0);
                    if (count > 0) {
                        spawnDrop(event, level, entity, drop.item, count);
                    }
                }
            }
        }
    }
    
    private static void spawnDrop(final LivingDropsEvent event, final Level level, final LivingEntity entity, final ItemStack template, final int count) {
        final int maxStack = template.getMaxStackSize();
        int remaining = count;
        while (remaining > 0) {
            final int take = Math.min(remaining, maxStack);
            remaining -= take;
            final ItemStack stack = template.copy();
            stack.setCount(take);
            final ItemEntity itemEntity = new ItemEntity(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), stack);
            itemEntity.setDefaultPickUpDelay();
            event.getDrops().add(itemEntity);
        }
    }
}
