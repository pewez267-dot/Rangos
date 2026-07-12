package com.fantasticpass.network;

import com.fantasticpass.client.ClientPacketHandler;
import com.fantasticpass.data.PassDefinition;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public final class OpenAdminScreenPacket {
   private final PassDefinition pass;

   public OpenAdminScreenPacket(PassDefinition pass) {
      this.pass = pass;
   }

   public PassDefinition getPass() {
      return this.pass;
   }

   public static void encode(OpenAdminScreenPacket packet, FriendlyByteBuf buf) {
      packet.pass.toBuf(buf);
   }

   public static OpenAdminScreenPacket decode(FriendlyByteBuf buf) {
      return new OpenAdminScreenPacket(PassDefinition.fromBuf(buf));
   }

   public static void handle(OpenAdminScreenPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openAdminScreen(packet)));
      context.setPacketHandled(true);
   }
}
