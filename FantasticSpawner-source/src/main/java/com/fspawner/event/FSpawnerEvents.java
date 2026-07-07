/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.StringTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.MobSpawnType
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.ServerLevelAccessor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.SpawnerBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraftforge.event.entity.EntityJoinLevelEvent
 *  net.minecraftforge.event.entity.living.LivingDropsEvent
 *  net.minecraftforge.event.entity.living.LivingHurtEvent
 *  net.minecraftforge.event.entity.living.MobSpawnEvent$FinalizeSpawn
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 */
package com.fspawner.event;

import com.fspawner.config.DropEntry;
import com.fspawner.config.EquipmentEntry;
import com.fspawner.config.InfernalConfig;
import com.fspawner.config.SpawnerConfig;
import com.fspawner.integration.InfernalMobsIntegration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class FSpawnerEvents {
    private FSpawnerEvents() {
    }

    @SubscribeEvent
    public static void onCheckSpawn(MobSpawnEvent.FinalizeSpawn event) {
        CompoundTag marker;
        CompoundTag forgeData;
        Mob entity;
        if (event.getSpawnType() == MobSpawnType.SPAWNER && (entity = event.getEntity()) != null && (forgeData = entity.getPersistentData()).contains("fspawner") && (marker = forgeData.getCompound("fspawner")).contains("cfg")) {
            SpawnerConfig cfg = SpawnerConfig.load(marker.getCompound("cfg"));
            ServerLevelAccessor level = event.getLevel();
            BlockPos pos = BlockPos.containing((double)event.getX(), (double)event.getY(), (double)event.getZ());
            if (cfg.isOneTime() && !FSpawnerEvents.hasEligiblePlayerNearby((LevelAccessor)level, pos, cfg)) {
                event.setSpawnCancelled(true);
                return;
            }
            if (!(FSpawnerEvents.matchesDayCycle((LevelAccessor)level, cfg) && FSpawnerEvents.matchesWeather((LevelAccessor)level, cfg) && FSpawnerEvents.matchesSky((LevelAccessor)level, pos, cfg) && FSpawnerEvents.matchesLight((LevelAccessor)level, pos, cfg))) {
                event.setSpawnCancelled(true);
            }
        }
    }

    private static boolean matchesDayCycle(LevelAccessor level, SpawnerConfig cfg) {
        boolean isDay;
        if (cfg.dayCycle == SpawnerConfig.DayCycle.ANY) {
            return true;
        }
        if (!(level instanceof Level)) {
            return true;
        }
        Level lvl = (Level)level;
        long time = lvl.getDayTime() % 24000L;
        boolean bl = isDay = time < 13000L || time > 23000L;
        return cfg.dayCycle == SpawnerConfig.DayCycle.DAY_ONLY ? isDay : !isDay;
    }

    private static boolean matchesWeather(LevelAccessor level, SpawnerConfig cfg) {
        if (cfg.weather == SpawnerConfig.Weather.ANY) {
            return true;
        }
        if (!(level instanceof Level)) {
            return true;
        }
        Level lvl = (Level)level;
        boolean thunder = lvl.isThundering();
        boolean rain = lvl.isRaining();
        return switch (cfg.weather) {
            case CLEAR -> {
                if (!rain && !thunder) {
                    yield true;
                }
                yield false;
            }
            case RAIN -> {
                if (rain && !thunder) {
                    yield true;
                }
                yield false;
            }
            case THUNDER -> thunder;
            default -> true;
        };
    }

    private static boolean matchesSky(LevelAccessor level, BlockPos pos, SpawnerConfig cfg) {
        if (!cfg.requiresSky && !cfg.requiresNoSky) {
            return true;
        }
        boolean canSee = level.canSeeSky(pos);
        return !(cfg.requiresSky && !canSee || cfg.requiresNoSky && canSee);
    }

    private static boolean matchesLight(LevelAccessor level, BlockPos pos, SpawnerConfig cfg) {
        if (cfg.minLight <= 0 && cfg.maxLight >= 15) {
            return true;
        }
        int light = level.getBrightness(LightLayer.BLOCK, pos);
        return light >= cfg.minLight && light <= cfg.maxLight;
    }

    private static boolean hasEligiblePlayerNearby(LevelAccessor level, BlockPos pos, SpawnerConfig cfg) {
        int base = cfg.activationRange > 0 ? cfg.activationRange : 16;
        double range = base + Math.max(1, cfg.spawnRange);
        AABB box = new AABB(pos).inflate(range);
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity;
        Level level = event.getLevel();
        if (!level.isClientSide() && (entity = event.getEntity()) instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            CompoundTag forgeData = entity.getPersistentData();
            if (forgeData.contains("fspawner")) {
                SpawnerConfig cfg;
                String mods;
                InfernalConfig inf;
                CompoundTag marker = forgeData.getCompound("fspawner");
                // Marca permanente: todo mob del spawner es inmune al quemado por sol (se conserva aunque el marcador se limpie).
                forgeData.putBoolean("fspNoSunBurn", true);
                FSpawnerEvents.handleAppearChances(living, marker);
                if (marker.contains("infernal") && (inf = InfernalConfig.load(marker.getCompound("infernal"))).isEnabled() && !(mods = inf.resolveModifierString(living.getRandom())).isBlank()) {
                    InfernalMobsIntegration.applyModifiers(living, mods);
                }
                SpawnerConfig spawnerConfig = cfg = marker.contains("cfg") ? SpawnerConfig.load(marker.getCompound("cfg")) : null;
                if (cfg != null && cfg.isOneTime() && level instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel)level;
                    FSpawnerEvents.scheduleOneTimeSpawnerRemoval(serverLevel, living.blockPosition(), cfg.spawnRange);
                }
                CompoundTag slim = new CompoundTag();
                boolean keepMarker = false;
                if (marker.contains("drops")) {
                    slim.put("drops", marker.get("drops"));
                    keepMarker = true;
                }
                if (marker.contains("keepVanillaDrops")) {
                    slim.putBoolean("keepVanillaDrops", marker.getBoolean("keepVanillaDrops"));
                    keepMarker = true;
                }
                if (cfg != null && cfg.dropToInventory) {
                    slim.putBoolean("dropToInventory", true);
                    slim.putInt("rewardRadius", Math.max(0, cfg.rewardRadius));
                    keepMarker = true;
                }
                if (keepMarker) {
                    forgeData.put("fspawner", (Tag)slim);
                } else {
                    forgeData.remove("fspawner");
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide()) {
            return;
        }
        if (!entity.getPersistentData().getBoolean("fspNoSunBurn")) {
            return;
        }
        if (entity.getRemainingFireTicks() <= 0) {
            return;
        }
        // Solo apaga el fuego bajo condiciones de quemado por sol (dia + cielo visible + seco),
        // para no afectar fuego de lava u otras fuentes.
        BlockPos eye = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
        if (level.isDay() && level.canSeeSky(eye) && !entity.isInWaterRainOrBubble()) {
            entity.setRemainingFireTicks(0);
        }
    }

    private static void handleAppearChances(LivingEntity living, CompoundTag marker) {
        if (marker.contains("appearChances")) {
            CompoundTag appear = marker.getCompound("appearChances");
            RandomSource random = living.getRandom();
            for (String slotName : appear.getAllKeys()) {
                float chance = appear.getFloat(slotName);
                if (!(random.nextFloat() > chance)) continue;
                EquipmentSlot slot = EquipmentEntry.slotByName(slotName);
                living.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        CompoundTag forgeData = entity.getPersistentData();
        if (!forgeData.contains("fspawner")) {
            return;
        }
        CompoundTag marker = forgeData.getCompound("fspawner");
        if (!marker.getBoolean("dropToInventory")) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player)) {
            return;
        }
        String id = source.getUUID().toString();
        ListTag attackers = marker.getList("atk", 8);
        for (int i = 0; i < attackers.size(); ++i) {
            if (!attackers.getString(i).equals(id)) continue;
            return;
        }
        attackers.add(StringTag.valueOf((String)id));
        marker.put("atk", (Tag)attackers);
        forgeData.put("fspawner", (Tag)marker);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        ServerLevel serverLevel;
        List<ServerPlayer> participants;
        boolean keepVanilla;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        CompoundTag forgeData = entity.getPersistentData();
        if (!forgeData.contains("fspawner")) {
            return;
        }
        CompoundTag marker = forgeData.getCompound("fspawner");
        boolean bl = keepVanilla = !marker.contains("keepVanillaDrops") || marker.getBoolean("keepVanillaDrops");
        if (!keepVanilla) {
            event.getDrops().clear();
        }
        if (!marker.contains("drops")) {
            return;
        }
        ListTag list = marker.getList("drops", 10);
        RandomSource random = entity.getRandom();
        Level level = entity.level();
        if (marker.getBoolean("dropToInventory") && level instanceof ServerLevel && !(participants = FSpawnerEvents.collectParticipants(serverLevel = (ServerLevel)level, entity, marker)).isEmpty()) {
            for (ServerPlayer player : participants) {
                FSpawnerEvents.distributeRewards(player, list, random, entity);
            }
            return;
        }
        for (int i = 0; i < list.size(); ++i) {
            int max;
            int min;
            int count;
            DropEntry drop = DropEntry.load(list.getCompound(i));
            if (drop.item.isEmpty() || !(random.nextFloat() <= drop.chance) || (count = (min = Math.max(0, Math.min(drop.min, drop.max))) + ((max = Math.max(drop.min, drop.max)) > min ? random.nextInt(max - min + 1) : 0)) <= 0) continue;
            FSpawnerEvents.spawnDrop(event, level, entity, drop.item, count);
        }
    }

    private static void spawnDrop(LivingDropsEvent event, Level level, LivingEntity entity, ItemStack template, int count) {
        int take;
        int maxStack = template.getMaxStackSize();
        for (int remaining = count; remaining > 0; remaining -= take) {
            take = Math.min(remaining, maxStack);
            ItemStack stack = template.copy();
            stack.setCount(take);
            ItemEntity itemEntity = new ItemEntity(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), stack);
            itemEntity.setDefaultPickUpDelay();
            event.getDrops().add(itemEntity);
        }
    }

    private static List<ServerPlayer> collectParticipants(ServerLevel level, LivingEntity boss, CompoundTag marker) {
        int radius;
        LinkedHashMap<UUID, ServerPlayer> participants = new LinkedHashMap<UUID, ServerPlayer>();
        if (marker.contains("atk")) {
            ListTag attackers = marker.getList("atk", 8);
            for (int i = 0; i < attackers.size(); ++i) {
                try {
                    UUID id = UUID.fromString(attackers.getString(i));
                    ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                    if (player == null || !player.isAlive() || player.level() != level) continue;
                    participants.put(id, player);
                    continue;
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    // empty catch block
                }
            }
        }
        int n = radius = marker.contains("rewardRadius") ? marker.getInt("rewardRadius") : 24;
        if (radius > 0) {
            AABB box = boss.getBoundingBox().inflate((double)radius);
            for (Player player : level.getEntitiesOfClass(Player.class, box)) {
                if (!(player instanceof ServerPlayer)) continue;
                ServerPlayer serverPlayer = (ServerPlayer)player;
                if (!serverPlayer.isAlive()) continue;
                participants.putIfAbsent(serverPlayer.getUUID(), serverPlayer);
            }
        }
        return new ArrayList<ServerPlayer>(participants.values());
    }

    private static void distributeRewards(ServerPlayer player, ListTag drops, RandomSource random, LivingEntity boss) {
        ArrayList<ItemStack> received = new ArrayList<ItemStack>();
        for (int i = 0; i < drops.size(); ++i) {
            int take;
            int max;
            DropEntry drop = DropEntry.load(drops.getCompound(i));
            if (drop.item.isEmpty() || random.nextFloat() > drop.chance) continue;
            int min = Math.max(0, Math.min(drop.min, drop.max));
            int count = min + ((max = Math.max(drop.min, drop.max)) > min ? random.nextInt(max - min + 1) : 0);
            if (count <= 0) continue;
            int maxStack = drop.item.getMaxStackSize();
            for (int remaining = count; remaining > 0; remaining -= take) {
                take = Math.min(remaining, maxStack);
                ItemStack stack = drop.item.copy();
                stack.setCount(take);
                FSpawnerEvents.giveToPlayer(player, stack);
            }
            ItemStack summary = drop.item.copy();
            summary.setCount(count);
            received.add(summary);
        }
        if (!received.isEmpty()) {
            FSpawnerEvents.sendRewardMessage(player, received, boss);
        }
    }

    private static void giveToPlayer(ServerPlayer player, ItemStack stack) {
        ItemStack copy = stack.copy();
        boolean added = player.getInventory().add(copy);
        if (!added || !copy.isEmpty()) {
            player.drop(copy, false);
        }
    }

    private static void sendRewardMessage(ServerPlayer player, List<ItemStack> items, LivingEntity boss) {
        player.sendSystemMessage((Component)Component.literal((String)"\u00a76\u00a7l\u2726 \u00a1Jefe derrotado! \u2726").append((Component)Component.literal((String)" \u00a77(").append((Component)boss.getDisplayName().copy()).append("\u00a77)")));
        MutableComponent line = Component.literal((String)"\u00a7e\u25b6 Recibiste en tu inventario: ");
        for (int i = 0; i < items.size(); ++i) {
            if (i > 0) {
                line.append((Component)Component.literal((String)"\u00a77, "));
            }
            ItemStack stack = items.get(i);
            line.append((Component)Component.literal((String)("\u00a7a" + stack.getCount() + "x \u00a7f"))).append((Component)stack.getHoverName().copy());
        }
        player.sendSystemMessage((Component)line);
    }

    private static void scheduleOneTimeSpawnerRemoval(ServerLevel level, BlockPos center, int spawnRange) {
        int radius = Math.max(1, spawnRange) + 2;
        level.getServer().execute(() -> FSpawnerEvents.removeNearbyOneTimeSpawner(level, center, radius));
    }

    private static void removeNearbyOneTimeSpawner(ServerLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dy = -3; dy <= 3; ++dy) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    SpawnerConfig cfg;
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockEntity be = level.getBlockEntity((BlockPos)cursor);
                    if (!(be instanceof SpawnerBlockEntity) || (cfg = FSpawnerEvents.readSpawnerConfig(be)) == null || !cfg.isOneTime()) continue;
                    BlockPos found = cursor.immutable();
                    level.levelEvent(2001, found, Block.getId((BlockState)Blocks.SPAWNER.defaultBlockState()));
                    level.removeBlock(found, false);
                    return;
                }
            }
        }
    }

    private static SpawnerConfig readSpawnerConfig(BlockEntity be) {
        CompoundTag marker;
        CompoundTag persistentData = be.getPersistentData();
        if (persistentData.contains("fspawner") && (marker = persistentData.getCompound("fspawner")).contains("cfg")) {
            return SpawnerConfig.load(marker.getCompound("cfg"));
        }
        return null;
    }
}

