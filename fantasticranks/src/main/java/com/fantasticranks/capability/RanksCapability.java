package com.fantasticranks.capability;

import com.fantasticranks.data.PlayerRanksData;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

/**
 * Capability token + convenience accessors for {@link PlayerRanksData}.
 */
public final class RanksCapability {

    public static final Capability<PlayerRanksData> RANKS_DATA =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private RanksCapability() {
    }

    public static LazyOptional<PlayerRanksData> get(Player player) {
        return player.getCapability(RANKS_DATA);
    }

    @Nullable
    public static PlayerRanksData getData(Player player) {
        return player.getCapability(RANKS_DATA).resolve().orElse(null);
    }
}
