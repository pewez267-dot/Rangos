package com.revivemod.event;

import com.revivemod.state.DownManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CombatEvents {
    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer)livingEntity;
        DamageSource source = event.getSource();
        if (DownManager.isReviving(player.getUUID()) && !CombatEvents.isLethalAllowed(source)) {
            event.setCanceled(true);
            return;
        }
        if (!DownManager.isDown(player)) {
            return;
        }
        if (!CombatEvents.isLethalAllowed(source)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer)livingEntity;
        DamageSource source = event.getSource();
        if (DownManager.isForceKilling(player.getUUID())) {
            return;
        }
        if (DownManager.isDown(player)) {
            if (!CombatEvents.isLethalAllowed(source)) {
                event.setCanceled(true);
            }
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (CombatEvents.isLethalAllowed(source)) {
            return;
        }
        DownManager.knockDown(player, source);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onChangeTarget(LivingChangeTargetEvent event) {
        ServerPlayer sp;
        LivingEntity target = event.getNewTarget();
        if (target instanceof ServerPlayer && DownManager.isDown((sp = (ServerPlayer)target).getUUID())) {
            event.setCanceled(true);
        }
    }

    private static boolean isLethalAllowed(DamageSource source) {
        if (source == null) {
            return false;
        }
        return source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL);
    }
}

