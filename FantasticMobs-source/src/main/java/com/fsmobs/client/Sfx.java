package com.fsmobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Sonido de UI discreto (clic de menu suave). */
public final class Sfx {

    private Sfx() {}

    public static void click() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.15f));
        }
    }
}
