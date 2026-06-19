package com.fantasticterraform.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S->C: progreso de una generacion inteligente (biomas/poblamiento/dungeon). El
 * progreso de la materializacion por lotes ya se reporta con {@link EditProgressPacket};
 * este packet existe para reportes especificos del pipeline de generacion y actualiza
 * la misma barra del HUD.
 */
public final class GenerationProgressPacket {

    public final String name;
    public final int processed;
    public final int total;
    public final boolean done;

    public GenerationProgressPacket(String name, int processed, int total, boolean done) {
        this.name = name;
        this.processed = processed;
        this.total = total;
        this.done = done;
    }

    public static void encode(GenerationProgressPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.name);
        buf.writeInt(m.processed);
        buf.writeInt(m.total);
        buf.writeBoolean(m.done);
    }

    public static GenerationProgressPacket decode(FriendlyByteBuf buf) {
        return new GenerationProgressPacket(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(GenerationProgressPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.fantasticterraform.client.ClientEditorState.updateProgress(m.name, m.processed, m.total, m.done)));
        c.setPacketHandled(true);
    }
}
