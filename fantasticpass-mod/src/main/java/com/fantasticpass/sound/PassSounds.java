package com.fantasticpass.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PassSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "fantasticpass");
   public static final RegistryObject<SoundEvent> PASS_MUSIC = SOUND_EVENTS.register(
      "pass_music", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("fantasticpass", "pass_music"))
   );

   private PassSounds() {
   }

   public static void register(IEventBus modBus) {
      SOUND_EVENTS.register(modBus);
   }
}
