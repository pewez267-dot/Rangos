package com.revivemod.mixin;

import com.revivemod.state.DownManager;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server-side mixin that locks a downed player into the SWIMMING (crawl) pose
 * WITHOUT spamming data-tracker delta packets every tick. The previous v1.2
 * approach toggled setSwimming/setPose from outside each tick, which the server
 * tracker re-broadcast and the client kept fighting -> visible vertical jitter.
 *
 * Here we cancel the server-side updateSwimming / updatePose calls so the
 * server's pose stays at SWIMMING permanently after the initial knockdown,
 * with no further deltas. The client local will still recompute STANDING in
 * its own tick (we can't patch a vanilla client without a client mod), but
 * because the server stops broadcasting changes after the first packet,
 * cliente y servidor dejan de pelearse cada tick — el ping-pong desaparece.
 *
 * Other observers see the crawl pose normally (their data tracker copy stays
 * at SWIMMING since the server never changes it).
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void revivemod$lockSwimming(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self instanceof ServerPlayerEntity && DownManager.isDown(self.getUuid())) {
            if (!self.isSwimming()) self.setSwimming(true);
            ci.cancel();
        }
    }

    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void revivemod$lockPose(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self instanceof ServerPlayerEntity && DownManager.isDown(self.getUuid())) {
            if (self.getPose() != EntityPose.SWIMMING) self.setPose(EntityPose.SWIMMING);
            ci.cancel();
        }
    }

    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void revivemod$keepStandingHitbox(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self instanceof ServerPlayerEntity && DownManager.isDown(self.getUuid())) {
            cir.setReturnValue(PlayerEntity.STANDING_DIMENSIONS);
        }
    }
}
