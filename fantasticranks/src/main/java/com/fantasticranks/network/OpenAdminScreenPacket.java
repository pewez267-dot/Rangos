package com.fantasticranks.network;

import com.fantasticranks.client.ClientPacketHandler;
import com.fantasticranks.data.RanksPackage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: open the admin package editor preloaded with the given package
 * (a fresh default ladder for {@code /fsranks create}).
 */
public final class OpenAdminScreenPacket {

    private final RanksPackage pkg;

    public OpenAdminScreenPacket(RanksPackage pkg) {
        this.pkg = pkg;
    }

    public RanksPackage getPackage() {
        return pkg;
    }

    public static void encode(OpenAdminScreenPacket packet, FriendlyByteBuf buf) {
        packet.pkg.toBuf(buf);
    }

    public static OpenAdminScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenAdminScreenPacket(RanksPackage.fromBuf(buf));
    }

    public static void handle(OpenAdminScreenPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.openAdminScreen(packet)));
        context.setPacketHandled(true);
    }
}
