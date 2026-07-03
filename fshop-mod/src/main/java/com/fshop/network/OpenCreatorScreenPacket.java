package com.fshop.network;

import com.fshop.client.ClientPacketHandler;
import com.fshop.shop.PlayerShop;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Opens the admin main-shop creator/editor GUI on the client. */
public final class OpenCreatorScreenPacket {
   private final PlayerShop shop;

   public OpenCreatorScreenPacket(PlayerShop shop) {
      this.shop = shop;
   }

   public PlayerShop getShop() {
      return this.shop;
   }

   public static void encode(OpenCreatorScreenPacket packet, FriendlyByteBuf buf) {
      packet.shop.toBuf(buf);
   }

   public static OpenCreatorScreenPacket decode(FriendlyByteBuf buf) {
      return new OpenCreatorScreenPacket(PlayerShop.fromBuf(buf));
   }

   public static void handle(OpenCreatorScreenPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.openCreator(packet)));
      context.setPacketHandled(true);
   }
}
