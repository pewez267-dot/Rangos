package com.fsmobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Aplica los topes de mobs al aparecer de forma natural. Solo actua sobre spawns naturales y de
 * generacion de terreno (no toca spawners, cria, huevos, comandos, etc. => no rompe granjas ni
 * mecanicas intencionales).
 */
@Mod.EventBusSubscriber(modid = FSMobs.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpawnEvents {

    private SpawnEvents() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION) {
            return;
        }
        Mob mob = event.getEntity();

        // 1) Multiplicador de aparicion (probabilidad). 1.0 = no hace nada.
        double mult = MobControl.getMultiplier();
        if (mult < 1.0 && event.getLevel().getRandom().nextDouble() > mult) {
            event.setSpawnCancelled(true);
            return;
        }

        // 2) Tope por radio. Determinar el limite y como contar.
        EntityType<?> etype = mob.getType();
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(etype);
        MobCategory cat = etype.getCategory();

        Integer typeCap = MobControl.typeCap(id == null ? null : id.toString());
        boolean byType = typeCap != null;
        int cap = byType ? typeCap : MobControl.categoryCap(cat);

        // Sin limite => salir YA, sin escanear entidades (coste cero).
        if (cap < 0) {
            return;
        }

        int radius = MobControl.getRadius();
        ServerLevel level = event.getLevel().getLevel();
        double x = event.getX();
        double y = event.getY();
        double z = event.getZ();
        AABB box = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);

        int count;
        if (byType) {
            count = level.getEntitiesOfClass(Mob.class, box, m -> m.getType() == etype).size();
        } else {
            count = level.getEntitiesOfClass(Mob.class, box, m -> m.getType().getCategory() == cat).size();
        }

        if (count >= cap) {
            event.setSpawnCancelled(true);
        }
    }
}
