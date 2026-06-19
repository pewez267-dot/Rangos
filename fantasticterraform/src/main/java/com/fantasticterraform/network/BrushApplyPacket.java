package com.fantasticterraform.network;

import com.fantasticterraform.brushes.BrushManager;
import com.fantasticterraform.brushes.BrushSettings;
import com.fantasticterraform.brushes.Falloff;
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
 * C->S: aplica el brush activo del jugador en un punto, incluyendo TODA su configuracion
 * (tipo, radio, intensidad, altura, bloque, falloff, bloque secundario, mezcla, profundidad
 * y modo hueco). El servidor valida OP y que el brush quede contenido en la seleccion.
 */
public final class BrushApplyPacket {

    private final BlockPos center;
    private final String brushId;
    private final int radius;
    private final double intensity;
    private final int height;
    private final String block;
    private final int falloff;
    private final String secondaryBlock;
    private final double mix;
    private final int depth;
    private final boolean hollow;

    public BrushApplyPacket(BlockPos center, String brushId, int radius, double intensity, int height, String block,
                            int falloff, String secondaryBlock, double mix, int depth, boolean hollow) {
        this.center = center;
        this.brushId = brushId;
        this.radius = radius;
        this.intensity = intensity;
        this.height = height;
        this.block = block;
        this.falloff = falloff;
        this.secondaryBlock = secondaryBlock;
        this.mix = mix;
        this.depth = depth;
        this.hollow = hollow;
    }

    public static void encode(BrushApplyPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.center);
        buf.writeUtf(msg.brushId);
        buf.writeInt(msg.radius);
        buf.writeDouble(msg.intensity);
        buf.writeInt(msg.height);
        buf.writeUtf(msg.block);
        buf.writeInt(msg.falloff);
        buf.writeUtf(msg.secondaryBlock);
        buf.writeDouble(msg.mix);
        buf.writeInt(msg.depth);
        buf.writeBoolean(msg.hollow);
    }

    public static BrushApplyPacket decode(FriendlyByteBuf buf) {
        return new BrushApplyPacket(buf.readBlockPos(), buf.readUtf(), buf.readInt(),
                buf.readDouble(), buf.readInt(), buf.readUtf(),
                buf.readInt(), buf.readUtf(), buf.readDouble(), buf.readInt(), buf.readBoolean());
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
            s.falloff = Falloff.byIndex(msg.falloff);
            s.secondaryBlock = BlockStateCodec.parse(lookup, msg.secondaryBlock);
            s.mix = msg.mix;
            s.depth = msg.depth;
            s.hollow = msg.hollow;
            BrushManager.setSettings(player.getUUID(), s);
            BrushManager.apply(player, (ServerLevel) player.level(), msg.center);
        });
        c.setPacketHandled(true);
    }
}
