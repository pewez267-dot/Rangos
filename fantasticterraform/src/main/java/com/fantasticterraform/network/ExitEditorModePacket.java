package com.fantasticterraform.network;

import com.fantasticterraform.core.EditorModeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: solicita salir del modo editor (boton del HUD). Validado OP-only. */
public final class ExitEditorModePacket {

    public ExitEditorModePacket() {
    }

    public static void encode(ExitEditorModePacket msg, FriendlyByteBuf buf) {
    }

    public static ExitEditorModePacket decode(FriendlyByteBuf buf) {
        return new ExitEditorModePacket();
    }

    public static void handle(ExitEditorModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player != null) {
                EditorModeManager.get().exit(player);
            }
        });
        c.setPacketHandled(true);
    }
}
