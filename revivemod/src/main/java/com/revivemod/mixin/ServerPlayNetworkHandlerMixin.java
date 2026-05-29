package com.revivemod.mixin;

import com.revivemod.state.DownManager;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detects the two key presses a downed player can fire that actually reach the
 * server: the inventory key (E -> OPEN_INVENTORY) and the swap-hands key
 * (F -> SWAP_ITEM_WITH_OFFHAND). While downed these are repurposed to toggle
 * the surrender (E) and self-revive (F) channels. We queue the toggle on a
 * thread-safe set; DownTicker consumes it on the main thread.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onClientCommand", at = @At("HEAD"), cancellable = true)
    private void revivemod$onE(ClientCommandC2SPacket packet, CallbackInfo ci) {
        if (player != null
                && packet.getMode() == ClientCommandC2SPacket.Mode.OPEN_INVENTORY
                && DownManager.isDown(player.getUuid())) {
            DownManager.requestSurrenderToggle(player.getUuid());
            ci.cancel();
        }
    }

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
