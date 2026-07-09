package com.fsrecipes.network;

import com.fsrecipes.RecipeBans;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * Cliente -> servidor: accion en lote. clear=true desbanea TODO; si no, banea/desbanea la lista de
 * items segun 'ban'. El servidor valida que sea OP.
 */
public class BulkBanPacket {

    private final List<ResourceLocation> ids;
    private final boolean ban;
    private final boolean clear;

    public BulkBanPacket(List<ResourceLocation> ids, boolean ban, boolean clear) {
        this.ids = ids;
        this.ban = ban;
        this.clear = clear;
    }

    public static void encode(BulkBanPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.clear);
        buf.writeBoolean(msg.ban);
        buf.writeVarInt(msg.ids.size());
        for (ResourceLocation id : msg.ids) {
            buf.writeResourceLocation(id);
        }
    }

    public static BulkBanPacket decode(FriendlyByteBuf buf) {
        boolean clear = buf.readBoolean();
        boolean ban = buf.readBoolean();
        int n = buf.readVarInt();
        List<ResourceLocation> ids = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ids.add(buf.readResourceLocation());
        }
        return new BulkBanPacket(ids, ban, clear);
    }

    public static void handle(BulkBanPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp == null || !sp.hasPermissions(2)) {
                return;
            }
            if (msg.clear) {
                RecipeBans.clearAll(sp.server);
            } else {
                RecipeBans.setBannedBulk(sp.server, msg.ids, msg.ban);
            }
            Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new SyncBansPacket(new HashSet<>(RecipeBans.banned())));
        });
        c.setPacketHandled(true);
    }
}
