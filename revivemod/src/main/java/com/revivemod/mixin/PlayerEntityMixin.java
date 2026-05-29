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
 * SERVER side of the crawl pose. While a player is downed, the server keeps the
 * pose at SWIMMING (crawling) and never recomputes it — so the value broadcast
 * to OTHER players is stable (no jitter). The hitbox is kept STANDING so the
 * server/client collision boxes match (no rubber-band) and allies can click the
 * body easily.
 *
 * The local owner's own view is handled separately by PlayerEntityClientMixin,
 * because a player never receives their own pose from the server.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    private boolean revivemod$serverDowned() {
        Object self = this;
        return self instanceof ServerPlayerEntity sp && DownManager.isDown(sp.getUuid());
    }

    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void revivemod$forceCrawlPose(CallbackInfo ci) {
        if (revivemod$serverDowned()) {
            ((PlayerEntity) (Object) this).setPose(EntityPose.SWIMMING);
            ci.cancel();
        }
    }

    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void revivemod$forceSwimFlag(CallbackInfo ci) {
        if (revivemod$serverDowned()) {
            PlayerEntity self = (PlayerEntity) (Object) this;
            if (!self.isSwimming()) self.setSwimming(true);
            ci.cancel();
        }
    }

    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void revivemod$keepStandingHitbox(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (revivemod$serverDowned()) {
            cir.setReturnValue(PlayerEntity.STANDING_DIMENSIONS);
        }
    }
}
