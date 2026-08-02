package com.fshop.network;

import com.fshop.client.ClientPacketHandler;
import com.fshop.shop.PlayerShop;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Server tells the client to open the ADD-mode price editor for a specific
 * inventory slot, because the clicked item is a brand-new product (no existing
 * offer to merge into). Sent only after the server has confirmed it is new.
 */
public final class OpenPriceScreenPacket {
   private final PlayerShop shop;
   private final int slot;

   public OpenPriceScreenPacket(PlayerShop shop, int slot) {
      this.shop = shop;
      this.slot = slot;
   }

   public PlayerShop getShop() {
      return this.shop;
   }

   public int getSlot() {
      return this.slot;
   }

   public static void encode(OpenPriceScreenPacket packet, FriendlyByteBuf buf) {
      packet.shop.toBuf(buf);
      buf.writeVarInt(packet.slot);
   }

   public static OpenPriceScreenPacket decode(FriendlyByteBuf buf) {
      PlayerShop shop = PlayerShop.fromBuf(buf);
      return new OpenPriceScreenPacket(shop, buf.readVarInt());
   }

   public static void handle(OpenPriceScreenPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.openPriceScreen(packet)));
      context.setPacketHandled(true);
   }
}
