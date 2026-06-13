package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.client.ClientHelpers;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenFavoriteColorScreenPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(OpenFavoriteColorScreenPacket.class);
   public static final ResourceLocation ID = new ResourceLocation("blockpops", "open_favorite_color_screen");

   public FriendlyByteBuf encode() {
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      buffer.writeBoolean(true);
      return buffer;
   }

   public static OpenFavoriteColorScreenPacket decode(FriendlyByteBuf buffer) {
      buffer.readBoolean();
      return new OpenFavoriteColorScreenPacket();
   }

   public static void handleClient(FriendlyByteBuf buf, PacketContext context) {
      OpenFavoriteColorScreenPacket packet = decode(buf);
      context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
               BlockPopsMod.logDebug("Opening favorite color selection screen");
               ClientHelpers.openFavoriteColorScreen();
            }));
   }

   public static void sendToPlayer(ServerPlayer player) {
      OpenFavoriteColorScreenPacket packet = new OpenFavoriteColorScreenPacket();
      NetworkManager.sendToPlayer(player, ID, packet.encode());
   }
}
