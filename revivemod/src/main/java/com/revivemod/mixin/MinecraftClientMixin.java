package com.revivemod.mixin;

import com.revivemod.util.BleedPose;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only: cancel the vanilla auto-open of the "Leave Bed" SleepingChatScreen
 * while the local player is bleeding. Self-contained check (no timing race).
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void revivemod$cancelSleepScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof SleepingChatScreen) {
            MinecraftClient mc = (MinecraftClient) (Object) this;
            if (BleedPose.isBleeding(mc.player)) {
                ci.cancel();
            }
        }
    }
}
