package com.revivemod.mixin;

import com.revivemod.state.DownManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Hostile mobs ignore downed players entirely: any attempt to target a downed
 * player is rewritten to "no target", so mobs lose interest and wander off
 * instead of standing there swinging at a body they can't hurt.
 */
@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity revivemod$ignoreDowned(LivingEntity target) {
        if (target instanceof ServerPlayerEntity sp && DownManager.isDown(sp.getUuid())) {
            return null;
        }
        return target;
    }
}
