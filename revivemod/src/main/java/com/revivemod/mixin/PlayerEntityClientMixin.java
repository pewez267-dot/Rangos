package com.revivemod.mixin;

import com.revivemod.client.RevivemodClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CLIENT side of the crawl pose: forces the LOCAL player into the SWIMMING
 * (crawl) pose while downed, so the player sees THEMSELVES crawling in 1st/3rd
 * person. A player never receives their own pose from the server, so this can
 * only be done with a client-side mixin (which is why the mod must be installed
 * client-side). Hitbox kept STANDING to match the server -> no rubber-band.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityClientMixin {

    private boolean revivemod$localDowned() {
        return RevivemodClient.LOCAL_DOWNED
                && MinecraftClient.getInstance().player == (Object) this;
    }

    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void revivemod$forceLocalCrawl(CallbackInfo ci) {
        if (revivemod$localDowned()) {
            ((PlayerEntity) (Object) this).setPose(EntityPose.SWIMMING);
            ci.cancel();
        }
    }

    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void revivemod$forceLocalSwimFlag(CallbackInfo ci) {
        if (revivemod$localDowned()) {
            PlayerEntity self = (PlayerEntity) (Object) this;
            if (!self.isSwimming()) self.setSwimming(true);
            ci.cancel();
        }
    }

    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void revivemod$localStandingHitbox(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (revivemod$localDowned()) {
            cir.setReturnValue(PlayerEntity.STANDING_DIMENSIONS);
        }
    }
}
