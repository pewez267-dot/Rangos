package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class VersionCheckPacket {
   public static final ResourceLocation ID = new ResourceLocation("blockpops", "version_check");
   public static final String REQUIRED_VERSION = "1.5.7-securityfix";

   private VersionCheckPacket() {
   }

   public static void enforce(ServerPlayer player) {
      if (player != null) {
         try {
            boolean canReceive = NetworkManager.canPlayerReceive(player, ID);
            if (!canReceive) {
               String name = "?";

               try {
                  name = player.getName().getString();
               } catch (Throwable var4) {
               }

               BlockPopsMod.LOGGER.warn("[BlockPops] Kicking {} (outdated BlockPops client; please update to {})", name, "1.5.7-securityfix");
               player.connection
                  .disconnect(
                     Component.literal(
                        "Tu mod BlockPops esta desactualizado.\n\nDescarga la version 1.5.7-securityfix\ny reemplaza el archivo en tu carpeta mods/.\n\nTu cliente actual no es compatible con este servidor."
                     )
                  );
            }
         } catch (Throwable var5) {
            BlockPopsMod.LOGGER.warn("[BlockPops] version-check skipped (no kick): {}", var5.toString());
         }
      }
   }

   public static void handleClient(FriendlyByteBuf buf, PacketContext context) {
   }

   public static void registerCommon() {
      // On Architectury 9.x (1.20.1) there is no registerS2CPayloadType; the existence of a
      // client-side receiver is what canPlayerReceive() checks, so nothing to do server-side.
   }

   public static void registerClient() {
      try {
         NetworkManager.registerReceiver(NetworkManager.s2c(), ID, VersionCheckPacket::handleClient);
      } catch (Throwable var1) {
         BlockPopsMod.LOGGER.warn("[BlockPops] could not register version-check receiver: {}", var1.toString());
      }
   }
}
