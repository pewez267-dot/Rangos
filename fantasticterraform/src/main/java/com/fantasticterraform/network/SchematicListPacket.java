package com.fantasticterraform.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** S->C: lista de nombres de schematic disponibles, para mostrar en el HUD. */
public final class SchematicListPacket {

    private final List<String> files;

    public SchematicListPacket(List<String> files) {
        this.files = files;
    }

    public static void encode(SchematicListPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.files.size());
        for (String f : msg.files) {
            buf.writeUtf(f);
        }
    }

    public static SchematicListPacket decode(FriendlyByteBuf buf) {
        int n = buf.readInt();
        List<String> files = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            files.add(buf.readUtf());
        }
        return new SchematicListPacket(files);
    }

    public static void handle(SchematicListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.fantasticterraform.client.ClientSchematicList.set(msg.files)));
        c.setPacketHandled(true);
    }
}
