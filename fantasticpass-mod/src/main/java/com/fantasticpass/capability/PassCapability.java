package com.fantasticpass.capability;

import com.fantasticpass.data.PlayerPassData;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

public final class PassCapability {
   public static final Capability<PlayerPassData> PASS_DATA = CapabilityManager.get(new CapabilityToken<PlayerPassData>() {
   });

   private PassCapability() {
   }

   public static LazyOptional<PlayerPassData> get(Player player) {
      return player.getCapability(PASS_DATA);
   }

   @Nullable
   public static PlayerPassData getData(Player player) {
      return (PlayerPassData)player.getCapability(PASS_DATA).resolve().orElse(null);
   }
}
