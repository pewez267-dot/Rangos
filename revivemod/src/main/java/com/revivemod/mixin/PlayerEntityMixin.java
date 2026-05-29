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
 * SERVER side of the crawl pose. While downed the server forces the pose to
 * SWIMMING (the crawl animation) and never recomputes it, so OTHER players see
 * a stable crawl (no jitter). We deliberately do NOT touch the swimming FLAG
 * (isSwimming) nor the hitbox dimensions: keeping the swim flag off means the
 * player keeps normal walking physics (they can walk and JUMP while crawling),
 * and the real low crawl hitbox makes the first-person camera drop to the floor.
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
}
