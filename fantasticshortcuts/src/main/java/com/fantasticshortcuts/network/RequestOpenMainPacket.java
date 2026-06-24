package com.fantasticshortcuts.network;

import com.fantasticshortcuts.FantasticShortcuts;
import com.fantasticshortcuts.gui.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client &rarr; server: request to (re)open the main shortcuts management screen. */
public final class RequestOpenMainPacket {

    public RequestOpenMainPacket() {
    }

    public static void encode(final RequestOpenMainPacket msg, final FriendlyByteBuf buf) {
    }

    public static RequestOpenMainPacket decode(final FriendlyByteBuf buf) {
        return new RequestOpenMainPacket();
    }

    public static void handle(final RequestOpenMainPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(FantasticShortcuts.ADMIN_PERMISSION_LEVEL)) {
                ModMenus.openMain(player);
            }
        });
        context.setPacketHandled(true);
    }
}
