package com.fshop.network;

import com.fshop.client.ClientPacketHandler;
import com.fshop.shop.PlayerShop;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Opens a specific shop's buy GUI, carrying the player's per-coin balances. */
public final class OpenShopViewScreenPacket {
   private final PlayerShop shop;
   private final long[] balances; // [bronze, silver, gold]

   public OpenShopViewScreenPacket(PlayerShop shop, long[] balances) {
      this.shop = shop;
      this.balances = balances;
   }

   public PlayerShop getShop() {
      return this.shop;
   }

   public long[] getBalances() {
      return this.balances;
   }

   public static void encode(OpenShopViewScreenPacket packet, FriendlyByteBuf buf) {
      packet.shop.toBuf(buf);
      for (int i = 0; i < 3; i++) {
         buf.writeVarLong(packet.balances[i]);
      }
   }

   public static OpenShopViewScreenPacket decode(FriendlyByteBuf buf) {
      PlayerShop shop = PlayerShop.fromBuf(buf);
      long[] bal = new long[3];
      for (int i = 0; i < 3; i++) {
         bal[i] = buf.readVarLong();
      }
      return new OpenShopViewScreenPacket(shop, bal);
   }

   public static void handle(OpenShopViewScreenPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.openShopView(packet)));
      context.setPacketHandled(true);
   }
}
