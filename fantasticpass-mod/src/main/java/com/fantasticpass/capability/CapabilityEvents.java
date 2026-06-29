package com.fantasticpass.capability;

import com.fantasticpass.data.PlayerPassData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CapabilityEvents {
   public static final ResourceLocation PROVIDER_ID = new ResourceLocation("fantasticpass", "pass_data");

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.register(PlayerPassData.class);
   }

   @SubscribeEvent
   public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
      if (event.getObject() instanceof Player) {
         PassCapabilityProvider provider = new PassCapabilityProvider();
         event.addCapability(PROVIDER_ID, provider);
         event.addListener(provider::invalidate);
      }
   }

   @SubscribeEvent
   public void onPlayerClone(Clone event) {
      event.getOriginal().reviveCaps();
      PlayerPassData oldData = PassCapability.getData(event.getOriginal());
      PlayerPassData newData = PassCapability.getData(event.getEntity());
      if (oldData != null && newData != null) {
         newData.copyFrom(oldData);
      }

      event.getOriginal().invalidateCaps();
   }
}
