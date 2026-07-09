package com.fsrecipes.network;

import com.fsrecipes.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/** Servidor -> cliente: actualiza el set de baneados que ve la GUI (sin abrirla). */
public class SyncBansPacket {

    private final Set<ResourceLocation> banned;

    public SyncBansPacket(Set<ResourceLocation> banned) {
        this.banned = banned;
    }

    public static void encode(SyncBansPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.banned.size());
        for (ResourceLocation id : msg.banned) {
            buf.writeResourceLocation(id);
        }
    }

    public static SyncBansPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Set<ResourceLocation> set = new HashSet<>();
        for (int i = 0; i < n; i++) {
            set.add(buf.readResourceLocation());
        }
        return new SyncBansPacket(set);
    }

    public static void handle(SyncBansPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHooks.updateBans(msg.banned)));
        c.setPacketHandled(true);
    }
}
