package com.fantasticterraform.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S->C: resultado de la validacion de tamano de dungeon (cumple/no cumple + cifra). */
public final class DungeonSelectionValidationResultPacket {

    private final boolean ok;
    private final String message;

    public DungeonSelectionValidationResultPacket(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public static void encode(DungeonSelectionValidationResultPacket m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.ok);
        buf.writeUtf(m.message);
    }

    public static DungeonSelectionValidationResultPacket decode(FriendlyByteBuf buf) {
        return new DungeonSelectionValidationResultPacket(buf.readBoolean(), buf.readUtf());
    }

    public static void handle(DungeonSelectionValidationResultPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            com.fantasticterraform.client.ClientToolState.genValidationOk = m.ok;
            com.fantasticterraform.client.ClientToolState.genValidationMsg = m.message;
        }));
        c.setPacketHandled(true);
    }
}
