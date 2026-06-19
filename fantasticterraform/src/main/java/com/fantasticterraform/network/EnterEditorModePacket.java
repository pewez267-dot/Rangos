package com.fantasticterraform.network;

import com.fantasticterraform.core.EditorModeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: solicita entrar al modo editor. Validado OP-only. */
public final class EnterEditorModePacket {

    public EnterEditorModePacket() {
    }

    public static void encode(EnterEditorModePacket msg, FriendlyByteBuf buf) {
    }

    public static EnterEditorModePacket decode(FriendlyByteBuf buf) {
        return new EnterEditorModePacket();
    }

    public static void handle(EnterEditorModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player != null) {
                EditorModeManager.get().enter(player);
            }
        });
        c.setPacketHandled(true);
    }
}
