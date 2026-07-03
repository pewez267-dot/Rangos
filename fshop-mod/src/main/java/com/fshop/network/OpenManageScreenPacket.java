package com.fshop.network;

import com.fshop.client.ClientPacketHandler;
import com.fshop.shop.PlayerShop;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Opens the owner's shop management (sell/stock) GUI on the client. */
public final class OpenManageScreenPacket {
   private final PlayerShop shop;

   public OpenManageScreenPacket(PlayerShop shop) {
      this.shop = shop;
   }

   public PlayerShop getShop() {
      return this.shop;
   }

   public static void encode(OpenManageScreenPacket packet, FriendlyByteBuf buf) {
      packet.shop.toBuf(buf);
   }

   public static OpenManageScreenPacket decode(FriendlyByteBuf buf) {
      return new OpenManageScreenPacket(PlayerShop.fromBuf(buf));
   }

   public static void handle(OpenManageScreenPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.openManage(packet)));
      context.setPacketHandled(true);
   }
}
