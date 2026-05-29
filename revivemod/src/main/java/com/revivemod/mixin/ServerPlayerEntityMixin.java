package com.revivemod.mixin;

import com.revivemod.state.DownManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code ServerPlayerEntity} overrides {@code wakeUp(ZZ)V} to additionally send
 * a wake-up animation packet to nearby clients and a position resync packet to
 * the woken player, AROUND the call to {@code super.wakeUp}. The
 * {@code PlayerEntityMixin} only cancels the super call, so without this mixin
 * those side-effect packets fire on every teleport / dimension change of a
 * downed player (visible "leave bed" animation to other players).
 *
 * Cancelling the override at HEAD short-circuits all of it.
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
