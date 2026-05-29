package com.revivemod.mixin;

import com.revivemod.state.DownManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ServerPlayerEntity overrides wakeUp to also send a wake-up animation packet
 * (to nearby clients) and a position resync, around the super.wakeUp call. The
 * PlayerEntityMixin only cancels the super call, so without this the stray
 * "leave bed" animation fires on every teleport of a downed player. Cancelling
 * at HEAD avoids it.
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "wakeUp(ZZ)V", at = @At("HEAD"), cancellable = true)
    private void revivemod$preventServerWakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (DownManager.isDown(self.getUuid())) {
            ci.cancel();
        }
    }
}
