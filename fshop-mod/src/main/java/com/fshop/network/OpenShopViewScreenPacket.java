package com.fshop.network;

import com.fshop.client.ClientPacketHandler;
import com.fshop.shop.PlayerShop;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Opens a specific shop's buy GUI on the client. */
public final class OpenShopViewScreenPacket {
   private final PlayerShop shop;
   private final long balance;

   public OpenShopViewScreenPacket(PlayerShop shop, long balance) {
      this.shop = shop;
      this.balance = balance;
   }

   public PlayerShop getShop() {
      return this.shop;
   }

   public long getBalance() {
      return this.balance;
   }

   public static void encode(OpenShopViewScreenPacket packet, FriendlyByteBuf buf) {
      packet.shop.toBuf(buf);
      buf.writeVarLong(packet.balance);
   }

   public static OpenShopViewScreenPacket decode(FriendlyByteBuf buf) {
      PlayerShop shop = PlayerShop.fromBuf(buf);
      long balance = buf.readVarLong();
      return new OpenShopViewScreenPacket(shop, balance);
   }

   public static void handle(OpenShopViewScreenPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.openShopView(packet)));
      context.setPacketHandled(true);
   }
}
