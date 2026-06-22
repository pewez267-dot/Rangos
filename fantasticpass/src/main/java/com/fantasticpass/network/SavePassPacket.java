package com.fantasticpass.network;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: persist an edited/created pass definition. Requires OP (permission
 * level 4); a spoofed packet from a non-OP client is rejected server-side.
 */
public final class SavePassPacket {

    private final PassDefinition pass;

    public SavePassPacket(PassDefinition pass) {
        this.pass = pass;
    }

    public static void encode(SavePassPacket packet, FriendlyByteBuf buf) {
        packet.pass.toBuf(buf);
    }

    public static SavePassPacket decode(FriendlyByteBuf buf) {
        return new SavePassPacket(PassDefinition.fromBuf(buf));
    }

    public static void handle(SavePassPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !sender.hasPermissions(4)) {
                return;
            }
            if (packet.pass.getId() == null || packet.pass.getId().isEmpty()) {
                return;
            }
            MinecraftServer server = sender.getServer();
            if (server == null) {
                return;
            }
            PassSavedData saved = PassSavedData.get(server);
            saved.putPass(packet.pass);
            sender.sendSystemMessage(Component.translatable("fantasticpass.msg.pass_saved", packet.pass.getId()));
        });
        context.setPacketHandled(true);
    }
}
