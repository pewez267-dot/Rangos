package com.revivemod.mixin;

import com.revivemod.client.RevivemodClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only mixin: cancel the vanilla auto-open of {@link SleepingChatScreen}
 * (the "Leave Bed" overlay) while the local player is bleeding. Vanilla
 * MinecraftClient.tick opens this screen whenever currentScreen==null and
 * player.isSleeping() — we want the sleeping pose without that screen.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void revivemod$cancelSleepScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof SleepingChatScreen && RevivemodClient.SUPPRESS_SLEEP_SCREEN) {
            ci.cancel();
        }
    }
}
