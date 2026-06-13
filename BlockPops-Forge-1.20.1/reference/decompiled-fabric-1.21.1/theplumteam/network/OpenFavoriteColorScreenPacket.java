package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.client.ClientHelpers;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import io.netty.buffer.Unpooled;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_5455;
import net.minecraft.class_9129;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenFavoriteColorScreenPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(OpenFavoriteColorScreenPacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "open_favorite_color_screen");

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_52964(true);
      return buffer;
   }

   public static OpenFavoriteColorScreenPacket decode(class_2540 buffer) {
      buffer.readBoolean();
      return new OpenFavoriteColorScreenPacket();
   }

   public static void handleClient(class_2540 buf, PacketContext context) {
      OpenFavoriteColorScreenPacket packet = decode(buf);
      context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
               BlockPopsMod.logDebug("Opening favorite color selection screen");
               ClientHelpers.openFavoriteColorScreen();
            }));
   }

   public static void sendToPlayer(class_3222 player) {
      OpenFavoriteColorScreenPacket packet = new OpenFavoriteColorScreenPacket();
      NetworkManager.sendToPlayer(player, ID, packet.encode());
   }
}
