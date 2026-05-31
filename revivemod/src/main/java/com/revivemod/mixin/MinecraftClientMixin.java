package com.revivemod.mixin;

import com.revivemod.client.RevivemodClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only: while the local player is downed, block opening the inventory /
 * any container screen (HandledScreen covers the survival & creative inventory
 * and all containers). The death screen and other non-container screens are not
 * affected.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void revivemod$blockInventoryWhileDowned(Screen screen, CallbackInfo ci) {
        if (screen instanceof HandledScreen<?> && RevivemodClient.LOCAL_DOWNED) {
            ci.cancel();
        }
    }
}
