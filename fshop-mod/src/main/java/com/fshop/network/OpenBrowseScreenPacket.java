package com.fshop.network;

import com.fshop.client.ClientPacketHandler;
import com.fshop.shop.ShopSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public final class OpenBrowseScreenPacket {
   private final List<ShopSummary> shops;

   public OpenBrowseScreenPacket(List<ShopSummary> shops) {
      this.shops = shops;
   }

   public List<ShopSummary> getShops() {
      return this.shops;
   }

   public static void encode(OpenBrowseScreenPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.shops.size());
      for (ShopSummary s : packet.shops) {
         s.toBuf(buf);
      }
   }

   public static OpenBrowseScreenPacket decode(FriendlyByteBuf buf) {
      int n = buf.readVarInt();
      List<ShopSummary> list = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
         list.add(ShopSummary.fromBuf(buf));
      }
      return new OpenBrowseScreenPacket(list);
   }

   public static void handle(OpenBrowseScreenPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.openBrowse(packet)));
      context.setPacketHandled(true);
   }
}
