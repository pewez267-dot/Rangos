package com.revivemod.mixin;

import com.revivemod.state.DownManager;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SERVER side of the crawl pose. While a player is downed the server keeps the
 * pose at SWIMMING (crawling) and the swim flag set, never recomputing them, so
 * the value broadcast to OTHER players is stable (no jitter). We do NOT override
 * the hitbox dimensions, so the player gets the real low crawl hitbox / eye
 * height (the camera drops to the ground in first person).
 *
 * The local owner's own view is handled by PlayerEntityClientMixin.
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
}
