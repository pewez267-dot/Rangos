package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.block.PopBlockColor;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import com.theplumteam.figure.PlayerCollectionHelper;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetFavoriteColorPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(SetFavoriteColorPacket.class);
   public static final ResourceLocation ID = new ResourceLocation("blockpops", "set_favorite_color");
   private final String colorName;

   public SetFavoriteColorPacket(String colorName) {
      this.colorName = colorName;
   }

   public FriendlyByteBuf encode() {
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      buffer.writeUtf(this.colorName);
      return buffer;
   }

   public static SetFavoriteColorPacket decode(FriendlyByteBuf buffer) {
      String colorName = buffer.readUtf();
      return new SetFavoriteColorPacket(colorName);
   }

   public static void handleServer(FriendlyByteBuf buf, PacketContext context) {
      SetFavoriteColorPacket packet = decode(buf);
      context.queue(() -> {
         if (context.getPlayer() instanceof ServerPlayer player) {
            IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);

            try {
               PopBlockColor color = PopBlockColor.valueOf(packet.colorName.toUpperCase());
               discovery.setFavoriteColor(color);
               discovery.setHasChosenFavoriteColor(true);
               PlayerDataManager.markDirty(player, discovery);
               BlockPopsMod.logDebug("Player {} chose favorite color: {}", player.getName().getString(), color.getSerializedName());
               if (player.getServer() != null) {
                  PlayerCollectionHelper.regenerateAndSyncPlayerCollection(player.getServer());
                  BlockPopsMod.logDebug("Regenerated World Players collection after {} changed their favorite color", player.getName().getString());
               }
            } catch (IllegalArgumentException var5) {
               LOGGER.warn("Player {} sent invalid color name: {}", player.getName().getString(), packet.colorName);
            }
         }
      });
   }

   public void sendToServer() {
      NetworkManager.sendToServer(ID, this.encode());
   }
}
