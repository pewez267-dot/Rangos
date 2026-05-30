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

/**
 * Replaces Fabric's ServerLivingEntityEvents.ALLOW_DAMAGE / ALLOW_DEATH and the MobEntity#setTarget mixin.
 *  - {@link LivingAttackEvent}        : downed players & active revivers are immune to non-lethal damage.
 *  - {@link LivingDeathEvent}         : the killing blow knocks a player down instead of killing them.
 *  - {@link LivingChangeTargetEvent}  : mobs refuse to target downed players.
 */
public final class CombatEvents {

    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        // Revivers are invincible while reviving (except to "lethal-allowed" sources).
        if (DownManager.isReviving(player.getUUID()) && !isLethalAllowed(source)) {
            event.setCanceled(true);
            return;
        }
        if (!DownManager.isDown(player)) {
            return;
        }
        // Downed players only take "lethal-allowed" damage (void / kill).
        if (!isLethalAllowed(source)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (DownManager.isForceKilling(player.getUUID())) {
            return; // a real, intended death (surrender / timeout / admin kill)
        }
        if (DownManager.isDown(player)) {
            if (!isLethalAllowed(source)) {
                event.setCanceled(true);
            }
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (isLethalAllowed(source)) {
            return;
        }
        DownManager.knockDown(player, source);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target instanceof ServerPlayer sp && DownManager.isDown(sp.getUUID())) {
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
