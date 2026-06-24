package com.fantasticranks.capability;

import com.fantasticranks.FantasticRanks;
import com.fantasticranks.data.PlayerRanksData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Capability lifecycle: registration (mod bus), attachment to players, and copying data
 * across the player clone that occurs on death and dimension change (forge bus).
 */
public final class CapabilityEvents {

    public static final ResourceLocation PROVIDER_ID = new ResourceLocation(FantasticRanks.MOD_ID, "ranks_data");

    public CapabilityEvents() {
    }

    /** Mod event bus. Registered via {@code addListener} in the main class. */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerRanksData.class);
    }

    /** Forge event bus. */
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            RanksCapabilityProvider provider = new RanksCapabilityProvider();
            event.addCapability(PROVIDER_ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    /** Forge event bus. Preserves rank data across death/respawn and dimension changes. */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        PlayerRanksData oldData = RanksCapability.getData(event.getOriginal());
        PlayerRanksData newData = RanksCapability.getData(event.getEntity());
        if (oldData != null && newData != null) {
            newData.copyFrom(oldData);
        }
        event.getOriginal().invalidateCaps();
    }
}
