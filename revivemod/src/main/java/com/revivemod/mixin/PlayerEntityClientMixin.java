package com.revivemod.mixin;

import com.revivemod.client.RevivemodClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CLIENT side of the crawl pose: forces the LOCAL player into the SWIMMING
 * (crawl) pose while downed, so the player sees THEMSELVES crawling in 1st/3rd
 * person (and the first-person camera drops to the ground). A player never
 * receives their own pose from the server, so this can only be done with a
 * client-side mixin.
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
}
