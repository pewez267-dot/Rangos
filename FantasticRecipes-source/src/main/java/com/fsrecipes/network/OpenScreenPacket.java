package com.fsrecipes.network;

import com.fsrecipes.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Servidor -> cliente: abre la GUI de baneo de recetas con el set actual de baneados. */
public class OpenScreenPacket {

    private final Set<ResourceLocation> banned;

    public OpenScreenPacket(Set<ResourceLocation> banned) {
        this.banned = banned;
    }

    public static void encode(OpenScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.banned.size());
        for (ResourceLocation id : msg.banned) {
            buf.writeResourceLocation(id);
        }
    }

    public static OpenScreenPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Set<ResourceLocation> set = new HashSet<>();
        for (int i = 0; i < n; i++) {
            set.add(buf.readResourceLocation());
        }
        return new OpenScreenPacket(set);
    }

    public static void handle(OpenScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHooks.openScreen(msg.banned)));
        c.setPacketHandled(true);
    }

    public List<ResourceLocation> asList() {
        return new ArrayList<>(banned);
    }
}
