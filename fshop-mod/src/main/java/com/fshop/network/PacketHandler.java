package com.fshop.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PacketHandler {
   public static final SimpleChannel CHANNEL = ChannelBuilder.named(new ResourceLocation("fshop", "main"))
         .networkProtocolVersion(() -> "1")
         .clientAcceptedVersions("1"::equals)
         .serverAcceptedVersions("1"::equals)
         .simpleChannel();

   private PacketHandler() {
   }

   public static void register() {
      int id = 0;
      // Server -> Client (open screens)
      CHANNEL.messageBuilder(OpenBrowseScreenPacket.class, id++)
            .encoder(OpenBrowseScreenPacket::encode).decoder(OpenBrowseScreenPacket::decode)
            .consumerMainThread(OpenBrowseScreenPacket::handle).add();
      CHANNEL.messageBuilder(OpenShopViewScreenPacket.class, id++)
            .encoder(OpenShopViewScreenPacket::encode).decoder(OpenShopViewScreenPacket::decode)
            .consumerMainThread(OpenShopViewScreenPacket::handle).add();
      CHANNEL.messageBuilder(OpenManageScreenPacket.class, id++)
            .encoder(OpenManageScreenPacket::encode).decoder(OpenManageScreenPacket::decode)
            .consumerMainThread(OpenManageScreenPacket::handle).add();
      // Client -> Server (requests / actions)
      CHANNEL.messageBuilder(RequestBrowsePacket.class, id++)
            .encoder(RequestBrowsePacket::encode).decoder(RequestBrowsePacket::decode)
            .consumerMainThread(RequestBrowsePacket::handle).add();
      CHANNEL.messageBuilder(OpenShopRequestPacket.class, id++)
            .encoder(OpenShopRequestPacket::encode).decoder(OpenShopRequestPacket::decode)
            .consumerMainThread(OpenShopRequestPacket::handle).add();
      CHANNEL.messageBuilder(RequestManagePacket.class, id++)
            .encoder(RequestManagePacket::encode).decoder(RequestManagePacket::decode)
            .consumerMainThread(RequestManagePacket::handle).add();
      CHANNEL.messageBuilder(BuyPacket.class, id++)
            .encoder(BuyPacket::encode).decoder(BuyPacket::decode)
            .consumerMainThread(BuyPacket::handle).add();
      CHANNEL.messageBuilder(AddOfferPacket.class, id++)
            .encoder(AddOfferPacket::encode).decoder(AddOfferPacket::decode)
            .consumerMainThread(AddOfferPacket::handle).add();
      CHANNEL.messageBuilder(SetPricePacket.class, id++)
            .encoder(SetPricePacket::encode).decoder(SetPricePacket::decode)
            .consumerMainThread(SetPricePacket::handle).add();
      CHANNEL.messageBuilder(RemoveOfferPacket.class, id++)
            .encoder(RemoveOfferPacket::encode).decoder(RemoveOfferPacket::decode)
            .consumerMainThread(RemoveOfferPacket::handle).add();
      CHANNEL.messageBuilder(CollectPacket.class, id++)
            .encoder(CollectPacket::encode).decoder(CollectPacket::decode)
            .consumerMainThread(CollectPacket::handle).add();
      // Admin main-shop creator
      CHANNEL.messageBuilder(OpenCreatorScreenPacket.class, id++)
            .encoder(OpenCreatorScreenPacket::encode).decoder(OpenCreatorScreenPacket::decode)
            .consumerMainThread(OpenCreatorScreenPacket::handle).add();
      CHANNEL.messageBuilder(SaveMainShopPacket.class, id++)
            .encoder(SaveMainShopPacket::encode).decoder(SaveMainShopPacket::decode)
            .consumerMainThread(SaveMainShopPacket::handle).add();
   }

   public static void sendToServer(Object message) {
      CHANNEL.sendToServer(message);
   }

   public static void sendToPlayer(ServerPlayer player, Object message) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
   }
}
