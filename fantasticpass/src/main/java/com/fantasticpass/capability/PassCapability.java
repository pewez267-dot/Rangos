package com.fantasticpass.capability;

import com.fantasticpass.data.PlayerPassData;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

/**
 * Capability token + convenience accessors for {@link PlayerPassData}.
 */
public final class PassCapability {

    public static final Capability<PlayerPassData> PASS_DATA =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private PassCapability() {
    }

    public static LazyOptional<PlayerPassData> get(Player player) {
        return player.getCapability(PASS_DATA);
    }

    /**
     * @return the player's pass data, or {@code null} if the capability is not present
     *         (should not happen for real players once attached).
     */
    @Nullable
    public static PlayerPassData getData(Player player) {
        return player.getCapability(PASS_DATA).resolve().orElse(null);
    }
}
