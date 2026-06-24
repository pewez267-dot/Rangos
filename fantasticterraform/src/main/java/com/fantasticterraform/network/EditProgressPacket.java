package com.fantasticterraform.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S->C: progreso de una operacion masiva (edicion o terreno) para mostrarlo en el HUD.
 * Unifica el progreso de edicion y de terreno en un solo paquete.
 */
public final class EditProgressPacket {

    public final String name;
    public final int processed;
    public final int total;
    public final boolean done;

    public EditProgressPacket(String name, int processed, int total, boolean done) {
        this.name = name;
        this.processed = processed;
        this.total = total;
        this.done = done;
    }

    public static void encode(EditProgressPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name);
        buf.writeInt(msg.processed);
        buf.writeInt(msg.total);
        buf.writeBoolean(msg.done);
    }

    public static EditProgressPacket decode(FriendlyByteBuf buf) {
        return new EditProgressPacket(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(EditProgressPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.fantasticterraform.client.ClientEditorState.updateProgress(msg.name, msg.processed, msg.total, msg.done)));
        c.setPacketHandled(true);
    }
}
