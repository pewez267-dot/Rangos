package com.revivemod.mixin;

import com.revivemod.state.DownManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detects the swap-hands key (F -> SWAP_ITEM_WITH_OFFHAND), which is the only
 * single-key press a downed player can fire that reaches the server without
 * opening a screen. While downed it is repurposed to toggle the self-revive
 * channel. Surrender uses SHIFT (sneak) detected directly in DownTicker since
 * sneak is a continuous, holdable state. We queue the toggle on a thread-safe
 * set; DownTicker consumes it on the main thread.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void revivemod$onF(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (player != null
                && packet.getAction() == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND
                && DownManager.isDown(player.getUuid())) {
            DownManager.requestSelfToggle(player.getUuid());
            ci.cancel();
        }
    }
}
