package com.fantasticranks.network;

import com.fantasticranks.data.RanksPackage;
import com.fantasticranks.data.RanksSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: persist an edited/created rank package. Requires OP (permission level
 * 4); a spoofed packet from a non-OP client is rejected server-side.
 */
public final class SavePackagePacket {

    private final RanksPackage pkg;

    public SavePackagePacket(RanksPackage pkg) {
        this.pkg = pkg;
    }

    public static void encode(SavePackagePacket packet, FriendlyByteBuf buf) {
        packet.pkg.toBuf(buf);
    }

    public static SavePackagePacket decode(FriendlyByteBuf buf) {
        return new SavePackagePacket(RanksPackage.fromBuf(buf));
    }

    public static void handle(SavePackagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !sender.hasPermissions(4)) {
                return;
            }
            if (packet.pkg.getId() == null || packet.pkg.getId().isEmpty()) {
                return;
            }
            MinecraftServer server = sender.getServer();
            if (server == null) {
                return;
            }
            packet.pkg.renumber();
            RanksSavedData.get(server).putPackage(packet.pkg);
            sender.sendSystemMessage(Component.translatable("fantasticranks.msg.package_saved", packet.pkg.getId()));
        });
        context.setPacketHandled(true);
    }
}
