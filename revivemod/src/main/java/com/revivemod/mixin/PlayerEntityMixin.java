package com.revivemod.mixin;

import com.revivemod.state.DownManager;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Down state uses the SLEEPING pose (player lies on their back, 100% server-side,
 * visible to themselves AND others, no jitter). This mixin removes the unwanted
 * side effects of sleeping for a downed player:
 *  - wakeUp cancelled (dawn / damage / teleport can't auto-wake).
 *  - canResetTimeBySleeping false (never counts toward skip-the-night).
 *  - getBaseDimensions kept at STANDING so the hitbox stays full-size (allies
 *    can click the body easily; no tiny sleeping hitbox).
 *  - sleepTimer held at 0 so the vanilla full-screen black sleep overlay never
 *    fades in (the HUD / bossbar stay visible behind the sleep chat screen).
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Shadow public int sleepTimer;

    @Inject(method = "wakeUp(ZZ)V", at = @At("HEAD"), cancellable = true)
    private void revivemod$preventWakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (DownManager.isDown(self.getUuid())) {
            ci.cancel();
        }
    }

    @Inject(method = "canResetTimeBySleeping", at = @At("HEAD"), cancellable = true)
    private void revivemod$dontSkipNight(CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (DownManager.isDown(self.getUuid())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void revivemod$keepStandingHitbox(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self instanceof ServerPlayerEntity && DownManager.isDown(self.getUuid())) {
            cir.setReturnValue(PlayerEntity.STANDING_DIMENSIONS);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void revivemod$resetSleepTimer(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (DownManager.isDown(self.getUuid())) {
            this.sleepTimer = 0;
        }
    }
}
