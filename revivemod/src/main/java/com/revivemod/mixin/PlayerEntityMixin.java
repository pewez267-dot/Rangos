package com.revivemod.mixin;

import com.revivemod.util.BleedPose;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the SLEEPING pose usable as a "lying down / bleeding" state without the
 * sleep side effects. All checks use {@link BleedPose#isBleeding} so they run
 * correctly on BOTH the server and the client (the local player's own client
 * is what draws the black overlay, so this MUST work client-side too).
 *
 *  - wakeUp cancelled (nothing auto-wakes a bleeding player).
 *  - canResetTimeBySleeping false (never counts toward skip-the-night).
 *  - getBaseDimensions kept STANDING (full-size hitbox, normal eye height).
 *  - sleepTimer held at 0 so the vanilla full-screen black sleep overlay never
 *    fades in.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Shadow public int sleepTimer;

    @Inject(method = "wakeUp(ZZ)V", at = @At("HEAD"), cancellable = true)
    private void revivemod$preventWakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        if (BleedPose.isBleeding((PlayerEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "canResetTimeBySleeping", at = @At("HEAD"), cancellable = true)
    private void revivemod$dontSkipNight(CallbackInfoReturnable<Boolean> cir) {
        if (BleedPose.isBleeding((PlayerEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void revivemod$keepStandingHitbox(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (BleedPose.isBleeding((PlayerEntity) (Object) this)) {
            cir.setReturnValue(PlayerEntity.STANDING_DIMENSIONS);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void revivemod$resetSleepTimer(CallbackInfo ci) {
        if (BleedPose.isBleeding((PlayerEntity) (Object) this)) {
            this.sleepTimer = 0;
        }
    }
}
