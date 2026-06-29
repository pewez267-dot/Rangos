package com.fantasticpass.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkRegistry.ChannelBuilder;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PacketHandler {
   private static final String PROTOCOL_VERSION = "1";
   public static final SimpleChannel CHANNEL = ChannelBuilder.named(new ResourceLocation("fantasticpass", "main"))
      .networkProtocolVersion(() -> "1")
      .clientAcceptedVersions("1"::equals)
      .serverAcceptedVersions("1"::equals)
      .simpleChannel();

   private PacketHandler() {
   }

   public static void register() {
      int id = 0;
      CHANNEL.messageBuilder(ClaimTierPacket.class, id++)
         .encoder(ClaimTierPacket::encode)
         .decoder(ClaimTierPacket::decode)
         .consumerMainThread(ClaimTierPacket::handle)
         .add();
      CHANNEL.messageBuilder(SetDisplayRankPacket.class, id++)
         .encoder(SetDisplayRankPacket::encode)
         .decoder(SetDisplayRankPacket::decode)
         .consumerMainThread(SetDisplayRankPacket::handle)
         .add();
      CHANNEL.messageBuilder(SavePassPacket.class, id++)
         .encoder(SavePassPacket::encode)
         .decoder(SavePassPacket::decode)
         .consumerMainThread(SavePassPacket::handle)
         .add();
      CHANNEL.messageBuilder(OpenViewScreenPacket.class, id++)
         .encoder(OpenViewScreenPacket::encode)
         .decoder(OpenViewScreenPacket::decode)
         .consumerMainThread(OpenViewScreenPacket::handle)
         .add();
      CHANNEL.messageBuilder(OpenAdminScreenPacket.class, id++)
         .encoder(OpenAdminScreenPacket::encode)
         .decoder(OpenAdminScreenPacket::decode)
         .consumerMainThread(OpenAdminScreenPacket::handle)
         .add();
      CHANNEL.messageBuilder(NametagUpdatePacket.class, id++)
         .encoder(NametagUpdatePacket::encode)
         .decoder(NametagUpdatePacket::decode)
         .consumerMainThread(NametagUpdatePacket::handle)
         .add();
      CHANNEL.messageBuilder(ClaimResultPacket.class, id++)
         .encoder(ClaimResultPacket::encode)
         .decoder(ClaimResultPacket::decode)
         .consumerMainThread(ClaimResultPacket::handle)
         .add();
   }

   public static void sendToServer(Object message) {
      CHANNEL.sendToServer(message);
   }

   public static void sendToPlayer(ServerPlayer player, Object message) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
   }

   public static void sendToTrackingAndSelf(ServerPlayer player, Object message) {
      CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message);
   }
}
