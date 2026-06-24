package com.fspawner.network;

import com.fspawner.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client. Tells the client to open the FSpawner GUI seeded with the
 * given configuration NBT.
 */
public class OpenScreenPacket {

    private final CompoundTag configNbt;

    public OpenScreenPacket(CompoundTag configNbt) {
        this.configNbt = configNbt;
    }

    public static void encode(OpenScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.configNbt);
    }

    public static OpenScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenScreenPacket(buf.readNbt());
    }

    public static void handle(OpenScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientPacketHandler.openScreen(msg.configNbt)));
        context.setPacketHandled(true);
    }
}
