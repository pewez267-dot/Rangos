package com.fantasticterraform.intelligent.dungeon.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Coloca el/los mob(s) de jefe (vanilla o de mod) en la sala de jefe usando su IA
 * vanilla, opcionalmente con equipamiento basico. Se ejecuta en el hilo del servidor
 * al terminar la materializacion de la dungeon.
 */
public final class BossEntityPlacer {

    private BossEntityPlacer() {
    }

    public static void spawn(ServerLevel level, BlockPos center, BossRoomConfig config) {
        ResourceLocation id = ResourceLocation.tryParse(config.entityId);
        if (id == null) {
            return;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) {
            return;
        }
        for (int i = 0; i < config.count; i++) {
            Entity entity = type.create(level);
            if (entity == null) {
                continue;
            }
            double ox = center.getX() + 0.5 + (i % 2 == 0 ? i : -i) * 0.6;
            double oz = center.getZ() + 0.5;
            entity.moveTo(ox, center.getY(), oz, 0.0F, 0.0F);
            if (config.equip && entity instanceof Mob mob) {
                mob.setPersistenceRequired();
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            }
            level.addFreshEntity(entity);
        }
    }
}
