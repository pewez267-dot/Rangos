package com.fantasticterraform.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S->C: activa o desactiva el estado de editor (y el HUD) en el cliente. */
public final class EditorStatePacket {

    private final boolean active;

    public EditorStatePacket(boolean active) {
        this.active = active;
    }

    public static void encode(EditorStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
    }

    public static EditorStatePacket decode(FriendlyByteBuf buf) {
        return new EditorStatePacket(buf.readBoolean());
    }

    public static void handle(EditorStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.fantasticterraform.client.ClientEditorState.setActive(msg.active)));
        c.setPacketHandled(true);
    }
}
