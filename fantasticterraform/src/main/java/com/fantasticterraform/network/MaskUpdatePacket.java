package com.fantasticterraform.network;

import com.fantasticterraform.masks.MaskManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * C->S: actualiza por completo la configuracion de mascaras del jugador. Las mascaras
 * activas se combinan con AND al aplicar edicion, brushes o terreno.
 */
public final class MaskUpdatePacket {

    private final MaskManager.MaskSettings settings;

    public MaskUpdatePacket(MaskManager.MaskSettings settings) {
        this.settings = settings;
    }

    public static void encode(MaskUpdatePacket msg, FriendlyByteBuf buf) {
        MaskManager.MaskSettings s = msg.settings;
        buf.writeBoolean(s.blockActive);
        buf.writeUtf(s.blockId == null ? "" : s.blockId.toString());
        buf.writeBoolean(s.listActive);
        writeIds(buf, s.listIds);
        buf.writeBoolean(s.exclusionActive);
        writeIds(buf, s.exclusionIds);
        buf.writeBoolean(s.heightActive);
        buf.writeInt(s.heightMin);
        buf.writeInt(s.heightMax);
        buf.writeBoolean(s.airOnlyActive);
        buf.writeBoolean(s.skyExposedActive);
    }

    public static MaskUpdatePacket decode(FriendlyByteBuf buf) {
        MaskManager.MaskSettings s = new MaskManager.MaskSettings();
        s.blockActive = buf.readBoolean();
        String block = buf.readUtf();
        s.blockId = block.isEmpty() ? null : ResourceLocation.tryParse(block);
        s.listActive = buf.readBoolean();
        s.listIds.addAll(readIds(buf));
        s.exclusionActive = buf.readBoolean();
        s.exclusionIds.addAll(readIds(buf));
        s.heightActive = buf.readBoolean();
        s.heightMin = buf.readInt();
        s.heightMax = buf.readInt();
        s.airOnlyActive = buf.readBoolean();
        s.skyExposedActive = buf.readBoolean();
        return new MaskUpdatePacket(s);
    }

    private static void writeIds(FriendlyByteBuf buf, List<ResourceLocation> ids) {
        buf.writeInt(ids.size());
        for (ResourceLocation id : ids) {
            buf.writeUtf(id.toString());
        }
    }

    private static List<ResourceLocation> readIds(FriendlyByteBuf buf) {
        int n = buf.readInt();
        List<ResourceLocation> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ResourceLocation id = ResourceLocation.tryParse(buf.readUtf());
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    public static void handle(MaskUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            MaskManager.set(player.getUUID(), msg.settings);
        });
        c.setPacketHandled(true);
    }
}
