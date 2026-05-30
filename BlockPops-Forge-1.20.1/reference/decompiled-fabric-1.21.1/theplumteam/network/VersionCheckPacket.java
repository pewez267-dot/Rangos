package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.class_2540;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_3222;

public final class VersionCheckPacket {
   public static final class_2960 ID = class_2960.method_60655("blockpops", "version_check");
   public static final String REQUIRED_VERSION = "1.5.7-securityfix";

   private VersionCheckPacket() {
   }

   public static void enforce(class_3222 var0) {
      if (var0 != null) {
         try {
            boolean var1 = NetworkManager.canPlayerReceive(var0, ID);
            if (!var1) {
               String var2 = "?";

               try {
                  var2 = var0.method_5477().getString();
               } catch (Throwable var4) {
               }

               BlockPopsMod.LOGGER.warn("[BlockPops] Kicking {} (outdated BlockPops client; please update to {})", var2, "1.5.7-securityfix");
               var0.field_13987
                  .method_52396(
                     class_2561.method_43470(
                        "Tu mod BlockPops esta desactualizado.\n\nDescarga la version 1.5.7-securityfix\ny reemplaza el archivo en tu carpeta mods/.\n\nTu cliente actual no es compatible con este servidor."
                     )
                  );
            }
         } catch (Throwable var5) {
            BlockPopsMod.LOGGER.warn("[BlockPops] version-check skipped (no kick): {}", var5.toString());
         }
      }
   }

   public static void handleClient(class_2540 var0, PacketContext var1) {
   }

   public static void registerCommon() {
      try {
         NetworkManager.registerS2CPayloadType(ID, null);
      } catch (Throwable var1) {
         BlockPopsMod.LOGGER.warn("[BlockPops] could not register version-check S2C type: {}", var1.toString());
      }
   }

   public static void registerClient() {
      try {
         NetworkManager.registerReceiver(NetworkManager.s2c(), ID, VersionCheckPacket::handleClient);
      } catch (Throwable var1) {
         BlockPopsMod.LOGGER.warn("[BlockPops] could not register version-check receiver: {}", var1.toString());
      }
   }
}
