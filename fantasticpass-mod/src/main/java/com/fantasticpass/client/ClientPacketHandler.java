package com.fantasticpass.client;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.gui.admin.PassAdminScreen;
import com.fantasticpass.gui.player.PassViewScreen;
import com.fantasticpass.nametag.ClientNametagCache;
import com.fantasticpass.network.NametagUpdatePacket;
import com.fantasticpass.network.OpenAdminScreenPacket;
import com.fantasticpass.network.OpenViewScreenPacket;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandler {
   private ClientPacketHandler() {
   }

   public static void openViewScreen(OpenViewScreenPacket packet) {
      PassDefinition pass = packet.getPass();
      PlayerPassData data = packet.getPlayerData();
      Minecraft.getInstance().setScreen(new com.fantasticpass.gui.castle.PassHubScreen(pass, data, packet.getPointsPerTier()));
   }

   public static void openAdminScreen(OpenAdminScreenPacket packet) {
      Minecraft.getInstance().setScreen(new PassAdminScreen(packet.getPass()));
   }

   public static void updateNametag(NametagUpdatePacket packet) {
      ClientNametagCache.put(packet.getPlayerId(), packet.getData());
   }

   public static void onClaimResult(com.fantasticpass.network.ClaimResultPacket packet) {
      net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
      if (screen instanceof PassViewScreen view) {
         view.applyServerData(packet.getPlayerData(), packet.getResult(), packet.getTier());
      }
   }
}
