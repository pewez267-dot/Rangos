package com.fscrates.mixin;

import com.fscrates.client.screen.CrateCinematicScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={LevelRenderer.class})
public class LevelRendererMixin {
    @Inject(method={"renderLevel"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void fscrates$skipWorldDuringCinematic(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CrateCinematicScreen) {
            ci.cancel();
        }
    }
}

