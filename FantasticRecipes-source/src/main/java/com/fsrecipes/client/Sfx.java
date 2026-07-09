package com.fsrecipes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Sonidos suaves de UI para la GUI (estilo Fantastic). */
public final class Sfx {

    private Sfx() {}

    private static void play(net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    public static void click() {
        play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.15f);
    }

    public static void select() {
        // Sin sonido (el "piano"/note block pling se quito a pedido del usuario).
    }

    public static void success() {
        play(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 0.2f);
    }
}
