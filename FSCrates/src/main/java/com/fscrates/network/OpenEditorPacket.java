package com.fscrates.network;

import com.fscrates.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> Client: open the crate editor GUI seeded with a crate config. */
public class OpenEditorPacket {

    private final CompoundTag configNbt;

    public OpenEditorPacket(CompoundTag configNbt) {
        this.configNbt = configNbt;
    }

    public static void encode(OpenEditorPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.configNbt);
    }

    public static OpenEditorPacket decode(FriendlyByteBuf buf) {
        return new OpenEditorPacket(buf.readNbt());
    }

    public static void handle(OpenEditorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientPacketHandler.openEditor(msg.configNbt)));
        context.setPacketHandled(true);
    }
}
