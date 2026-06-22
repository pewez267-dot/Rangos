package com.fantasticpass.capability;

import com.fantasticpass.FantasticPass;
import com.fantasticpass.data.PlayerPassData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Capability lifecycle: registration (mod bus), attachment to players, and copying
 * data across the player clone that occurs on death and dimension change (forge bus).
 */
public final class CapabilityEvents {

    public static final ResourceLocation PROVIDER_ID = new ResourceLocation(FantasticPass.MOD_ID, "pass_data");

    private CapabilityEvents() {
    }

    /** Mod event bus. */
    @SubscribeEvent
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerPassData.class);
    }

    /** Forge event bus. */
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (event.getObject() instanceof Player) {
            PassCapabilityProvider provider = new PassCapabilityProvider();
            event.addCapability(PROVIDER_ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    /** Forge event bus. Preserves pass data across death/respawn and dimension changes. */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        PlayerPassData oldData = PassCapability.getData(event.getOriginal());
        PlayerPassData newData = PassCapability.getData(event.getEntity());
        if (oldData != null && newData != null) {
            newData.copyFrom(oldData);
        }
        event.getOriginal().invalidateCaps();
    }
}
