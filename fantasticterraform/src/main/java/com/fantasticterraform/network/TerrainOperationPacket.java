package com.fantasticterraform.network;

import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.masks.MaskManager;
import com.fantasticterraform.schematics.BlockStateCodec;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.terrain.CaveGenerator;
import com.fantasticterraform.terrain.DeformOperation;
import com.fantasticterraform.terrain.ErosionOperation;
import com.fantasticterraform.terrain.HydraulicErosionOperation;
import com.fantasticterraform.terrain.MountainGenerator;
import com.fantasticterraform.terrain.NaturalizeOperation;
import com.fantasticterraform.terrain.SmoothOperation;
import com.fantasticterraform.terrain.TerraceOperation;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C->S: ejecuta una operacion de terreno sobre la seleccion activa. Todas pasan por la
 * cola por ticks. Los parametros genericos se mapean segun el tipo de operacion.
 */
public final class TerrainOperationPacket {

    public enum Op {
        SMOOTH, DEFORM, NATURALIZE, CAVE, MOUNTAIN, EROSION, HYDRAULIC, TERRACE
    }

    private final Op op;
    private final int i1;
    private final int i2;
    private final int i3;
    private final double d1;
    private final double d2;
    private final long seed;
    private final String s1;
    private final String s2;
    private final String s3;

    public TerrainOperationPacket(Op op, int i1, int i2, int i3, double d1, double d2, long seed,
                                  String s1, String s2, String s3) {
        this.op = op;
        this.i1 = i1;
        this.i2 = i2;
        this.i3 = i3;
        this.d1 = d1;
        this.d2 = d2;
        this.seed = seed;
        this.s1 = s1 == null ? "" : s1;
        this.s2 = s2 == null ? "" : s2;
        this.s3 = s3 == null ? "" : s3;
    }

    public static void encode(TerrainOperationPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.op);
        buf.writeInt(msg.i1);
        buf.writeInt(msg.i2);
        buf.writeInt(msg.i3);
        buf.writeDouble(msg.d1);
        buf.writeDouble(msg.d2);
        buf.writeLong(msg.seed);
        buf.writeUtf(msg.s1);
        buf.writeUtf(msg.s2);
        buf.writeUtf(msg.s3);
    }

    public static TerrainOperationPacket decode(FriendlyByteBuf buf) {
        return new TerrainOperationPacket(buf.readEnum(Op.class), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readDouble(), buf.readDouble(), buf.readLong(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(TerrainOperationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            ServerLevel level = (ServerLevel) player.level();
            SelectionShape sel = SelectionManager.get(player).getShape();
            if (sel == null) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7cNecesitas una seleccion activa valida para operaciones de terreno."));
                return;
            }
            Mask mask = MaskManager.combinedFor(player);
            HolderLookup<Block> lookup = player.server.registryAccess().lookupOrThrow(Registries.BLOCK);

            switch (msg.op) {
                case SMOOTH:
                    SmoothOperation.apply(player, level, sel, msg.i1, msg.d1, msg.i2, mask);
                    break;
                case DEFORM:
                    DeformOperation.apply(player, level, sel, curve(msg.i1), msg.d1, msg.seed, mask);
                    break;
                case NATURALIZE:
                    NaturalizeOperation.apply(player, level, sel,
                            BlockStateCodec.parse(lookup, msg.s1),
                            BlockStateCodec.parse(lookup, msg.s2),
                            BlockStateCodec.parse(lookup, msg.s3), msg.i1, mask);
                    break;
                case CAVE:
                    CaveGenerator.apply(player, level, sel, msg.d1, msg.d2, msg.seed, mask);
                    break;
                case MOUNTAIN:
                    MountainGenerator.apply(player, level, sel, msg.d1, msg.d2, msg.i1, msg.i2, msg.seed,
                            BlockStateCodec.parse(lookup, msg.s1),
                            BlockStateCodec.parse(lookup, msg.s2),
                            BlockStateCodec.parse(lookup, msg.s3), mask);
                    break;
                case EROSION:
                    ErosionOperation.apply(player, level, sel, msg.i1, msg.d1, msg.d2, mask);
                    break;
                case HYDRAULIC:
                    HydraulicErosionOperation.apply(player, level, sel, msg.i1 * 1000, msg.d1, msg.seed, mask);
                    break;
                case TERRACE:
                    TerraceOperation.apply(player, level, sel, msg.i1, mask);
                    break;
                default:
                    break;
            }
        });
        c.setPacketHandled(true);
    }

    private static DeformOperation.Curve curve(int index) {
        DeformOperation.Curve[] values = DeformOperation.Curve.values();
        return index >= 0 && index < values.length ? values[index] : DeformOperation.Curve.LINEAR;
    }
}
