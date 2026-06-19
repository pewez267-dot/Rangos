package com.fantasticterraform.network;

import com.fantasticterraform.brushes.BrushManager;
import com.fantasticterraform.brushes.BrushSettings;
import com.fantasticterraform.schematics.BlockStateCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C->S: aplica el brush activo del jugador en un punto, incluyendo su configuracion.
 * El servidor valida OP y que el punto + radio del brush quede contenido en la
 * seleccion activa (ver {@link BrushManager}).
 */
public final class BrushApplyPacket {

    private final BlockPos center;
    private final String brushId;
    private final int radius;
    private final double intensity;
    private final int height;
    private final String block;

    public BrushApplyPacket(BlockPos center, String brushId, int radius, double intensity, int height, String block) {
        this.center = center;
        this.brushId = brushId;
        this.radius = radius;
        this.intensity = intensity;
        this.height = height;
        this.block = block;
    }

    public static void encode(BrushApplyPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.center);
        buf.writeUtf(msg.brushId);
        buf.writeInt(msg.radius);
        buf.writeDouble(msg.intensity);
        buf.writeInt(msg.height);
        buf.writeUtf(msg.block);
    }

    public static BrushApplyPacket decode(FriendlyByteBuf buf) {
        return new BrushApplyPacket(buf.readBlockPos(), buf.readUtf(), buf.readInt(),
                buf.readDouble(), buf.readInt(), buf.readUtf());
    }

    public static void handle(BrushApplyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            HolderLookup<Block> lookup = player.server.registryAccess().lookupOrThrow(Registries.BLOCK);
            BrushSettings s = BrushManager.settings(player.getUUID());
            s.brushId = msg.brushId;
            s.radius = msg.radius;
            s.intensity = msg.intensity;
            s.height = msg.height;
            s.block = BlockStateCodec.parse(lookup, msg.block);
            BrushManager.setSettings(player.getUUID(), s);
            BrushManager.apply(player, (ServerLevel) player.level(), msg.center);
        });
        c.setPacketHandled(true);
    }
}
