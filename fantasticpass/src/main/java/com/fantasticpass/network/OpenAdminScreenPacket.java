package com.fantasticpass.network;

import com.fantasticpass.client.ClientPacketHandler;
import com.fantasticpass.data.PassDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: open the admin pass editor preloaded with the given definition
 * (a fresh, empty definition for {@code /fspass create}).
 */
public final class OpenAdminScreenPacket {

    private final PassDefinition pass;

    public OpenAdminScreenPacket(PassDefinition pass) {
        this.pass = pass;
    }

    public PassDefinition getPass() {
        return pass;
    }

    public static void encode(OpenAdminScreenPacket packet, FriendlyByteBuf buf) {
        packet.pass.toBuf(buf);
    }

    public static OpenAdminScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenAdminScreenPacket(PassDefinition.fromBuf(buf));
    }

    public static void handle(OpenAdminScreenPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.openAdminScreen(packet)));
        context.setPacketHandled(true);
    }
}
