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
 * Patches the side effects of pose SLEEPING that we don't want for a downed
 * player:
 *  - {@code wakeUp} cancelled so dawn / damage / movement don't auto-wake.
 *  - {@code canResetTimeBySleeping} returns false so the downed player never
 *    contributes to the "skip the night" vote.
 *  - {@code getBaseDimensions} returns STANDING_DIMENSIONS so the server-side
 *    hitbox stays normal (avoids tiny 0.2x0.2 sleeping hitbox).
 *  - {@code sleepTimer} is held at 0 so vanilla's full-screen black sleep
 *    overlay (rendered when sleepTimer >= ~100) is never triggered.
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

    /**
     * Hold sleepTimer at 0 every tick so the client never renders the
     * full-screen black "fade to sleep" overlay. Without this, vanilla's
     * InGameHud paints a heavy black overlay covering the whole screen
     * for the entire down state.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void revivemod$resetSleepTimer(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (DownManager.isDown(self.getUuid())) {
            this.sleepTimer = 0;
        }
    }
}
